# Boragora Viagens — API

Backend do app de controle de passageiros de van. Java 21 + Spring Boot 4 + Gradle, banco MySQL 8 com migrações Flyway.

O app fica no repositório [boragora-mobile](https://github.com/codesArmando/boragora-mobile). Clone os dois lado a lado, porque o app lê os documentos desta pasta `docs/`:

```
Boragora/
├── boragora-api/
└── boragora-mobile/
```

## Pré-requisitos

| Ferramenta | Versão | Para quê |
|---|---|---|
| [JDK](https://adoptium.net/) | 21 | Compilar e rodar a API |
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows/macOS) ou Docker Engine + plugin Compose (Linux) | qualquer recente | Subir o MySQL local e rodar os testes |
| Git | qualquer | — |

Não precisa instalar Gradle nem MySQL: o projeto traz o Gradle Wrapper (`./gradlew`), e o banco roda dentro do Docker.

Confira a instalação:

```bash
java -version            # deve mostrar 21
docker --version
docker compose version   # Compose v2 (com espaço, não "docker-compose")
```

> **Windows:** use o PowerShell ou o Git Bash e troque `./gradlew` por `.\gradlew.bat`. O Docker Desktop precisa estar aberto (ícone da baleia ativo) antes de rodar os comandos.

## Rodando localmente

### 1. Clonar

```bash
git clone https://github.com/codesArmando/boragora-api.git
cd boragora-api
```

### 2. Subir o banco

```bash
docker compose up -d
```

Isso cria o container `boragora-mysql` com MySQL 8, exposto na porta **3307** da sua máquina. A porta não é a 3306 de propósito, para não brigar com um MySQL que você já tenha instalado. Os dados ficam no volume `mysql-dados` e não se perdem quando o container para.

Espere o banco ficar pronto, o que leva uns 20 segundos na primeira vez:

```bash
docker compose ps        # a coluna STATUS deve mostrar "(healthy)"
```

### 3. Rodar a API

```bash
./gradlew bootRun
```

A primeira execução baixa o Gradle e as dependências e demora alguns minutos. Na subida:

- o Flyway cria as tabelas (`src/main/resources/db/migration/`);
- se ainda não existir nenhum admin, é criado o motorista-chefe padrão (veja abaixo).

A API fica disponível em **http://localhost:8080**.

### 4. Conferir

| O quê | Endereço |
|---|---|
| Health check | http://localhost:8080/health |
| Swagger UI (documentação e testes dos endpoints) | http://localhost:8080/swagger-ui.html |

### Usuário admin padrão (perfil `dev`)

| Campo | Valor |
|---|---|
| E-mail | `admin@boragora.local` |
| Senha | `admin123` |

Para testar no Swagger: chame `POST /auth/login` com o e-mail e a senha, copie o token da resposta, clique em **Authorize** e cole o token. Para criar um usuário comum, use `POST /auth/cadastro` com `nome`, `email` e `senha`.

## Configuração

Tudo tem um valor padrão no perfil `dev` (o perfil ativo por padrão). Só defina alguma variável de ambiente se quiser mudar o padrão.

| Variável | Padrão no `dev` | Descrição |
|---|---|---|
| `DB_HOST` | `localhost` | Host do MySQL |
| `DB_PORTA` | `3307` | Porta do MySQL. Também muda a porta publicada pelo `docker-compose.yml` |
| `DB_NOME` | `boragora` | Nome do banco |
| `DB_USUARIO` / `DB_SENHA` | `boragora` / `boragora` | Credenciais do banco |
| `PORTA` | `8080` | Porta HTTP da API |
| `JWT_SEGREDO` | valor fixo de desenvolvimento | Chave HS256, com pelo menos 32 bytes. Obrigatória fora do `dev` |
| `ADMIN_NOME` / `ADMIN_EMAIL` / `ADMIN_SENHA` | ver tabela acima | Admin criado na subida |
| `APP_RELOGIO_FIXO` | vazio (relógio real) | Congela o relógio da API, ex.: `2026-10-05T18:30` (fuso America/Sao_Paulo) |

### Testando regras de horário com o relógio fixo

Várias regras dependem da hora atual (prazos de confirmação, viagens do dia). Para testá-las sem esperar o horário real:

```bash
APP_RELOGIO_FIXO=2026-10-05T18:30 ./gradlew bootRun
```

No PowerShell:

```powershell
$env:APP_RELOGIO_FIXO="2026-10-05T18:30"; .\gradlew.bat bootRun
```

## Testes

```bash
./gradlew test
```

Os testes de integração usam **Testcontainers**: cada execução sobe um MySQL descartável próprio. Por isso o Docker precisa estar rodando, mas o `docker compose up` não é necessário para os testes.

## Acessando a API pelo celular (app em Expo)

O celular não enxerga o `localhost` do seu computador. O app descobre sozinho o IP da máquina que roda o Expo e chama a API na porta 8080. Para isso funcionar:

- o celular e o computador precisam estar **na mesma rede Wi-Fi**;
- o firewall precisa liberar conexões de entrada na porta **8080**. No Windows, aceite o aviso do firewall na primeira execução do Java ou crie uma regra para a porta.

Teste abrindo `http://<IP-do-computador>:8080/health` no navegador do celular. Mais detalhes no README do `boragora-mobile`.

## Comandos úteis do Docker

```bash
docker compose ps                  # status do banco
docker compose logs -f mysql       # logs do MySQL
docker compose stop                # para o banco e mantém os dados
docker compose down                # remove o container e mantém os dados (volume)
docker compose down -v             # remove container e volume: APAGA o banco
docker exec -it boragora-mysql mysql -uboragora -pboragora boragora   # console SQL
```

Para recomeçar com o banco limpo: `docker compose down -v && docker compose up -d` e rode a API de novo.

## Problemas comuns

| Sintoma | Causa provável e solução |
|---|---|
| `Communications link failure` / `Connection refused` na subida | O banco não está de pé ou ainda não ficou `healthy`. Rode `docker compose ps` e espere. |
| `port is already allocated` no `docker compose up` | A porta 3307 já está em uso. Use outra: `DB_PORTA=3308 docker compose up -d` e `DB_PORTA=3308 ./gradlew bootRun`. |
| `Port 8080 was already in use` | Outra aplicação usa a 8080. Feche-a ou rode com `PORTA=8081` (no app, ajuste `EXPO_PUBLIC_API_URL`). |
| `Cannot connect to the Docker daemon` / testes falham com erro do Testcontainers | O Docker não está rodando. Abra o Docker Desktop. |
| `Validate failed: Migrations have failed validation` / erro do Flyway | Uma migração já aplicada foi alterada. Nunca edite um `V*.sql` existente: crie uma nova migração. Em desenvolvimento, dá para limpar o banco com `docker compose down -v`. |
| `Schema-validation: missing column` | Entidade e banco estão diferentes (`ddl-auto: validate`). Crie a migração correspondente. |
| `Unsupported class file major version` / erro de toolchain | O Gradle não achou o JDK 21. Instale-o e confira `java -version`. |
| `permission denied: ./gradlew` (Linux/macOS) | `chmod +x gradlew` |

## Estrutura e documentação

```
src/main/java/br/com/boragora/api/   código da API, por domínio (grupo, seguranca, ...)
src/main/resources/application.yml   configuração e perfis (dev, test)
src/main/resources/db/migration/     migrações Flyway do banco
src/test/java/                       testes (Testcontainers)
docs/                                documentação do projeto
```

| Documento | Conteúdo |
|---|---|
| `docs/plano-implementacao-backend.md` | Plano por etapas, endpoints e formato das respostas |
| `docs/checklist-backend.md` | O que já está pronto. É ele que diz ao time do app o que já pode ser integrado |
| `docs/documento-escopo-app-van-v7.md` | Escopo e regras de negócio (RN01, RN02, ...) |
| `docs/mer_ajustado_app_van_dark.html` | Diagrama do banco (MER) |

## Como contribuir

1. Atualize a `main` e crie uma branch a partir dela: `git checkout -b etapa-4-motoristas-turmas`.
2. Siga a etapa em andamento no `docs/plano-implementacao-backend.md`. As etapas são feitas na ordem.
3. Rode `./gradlew test` antes de abrir o PR.
4. Marque no `docs/checklist-backend.md` os itens concluídos.
5. Abra um Pull Request para a `main`.

Mudanças no esquema do banco sempre entram por uma **nova** migração Flyway (`V2__...sql`, `V3__...sql`, ...), nunca editando uma que já existe.
