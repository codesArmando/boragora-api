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
./gradlew -q javaToolchains   # deve listar um JDK 21 (o java padrão pode ser outra versão)
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
- se ainda não existir nenhum admin, é criado o motorista-chefe padrão (veja abaixo);
- na primeira subida, é criada a carga de demonstração com o cenário das telas (veja abaixo).

A API fica disponível em **http://localhost:8080** e também pelo IP da máquina na rede local (ela escuta em `0.0.0.0`).

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

O motorista-chefe padrão não conduz nenhuma turma, então a aba Hoje dele mostra a tela C0b. Ele acompanha o dia de qualquer turma pela aba Grupo.

### Carga de demonstração (perfil `dev`)

Na primeira subida do perfil `dev`, a API cria o mesmo cenário dos dados falsos do app e do design (`CargaDeDemonstracao`). Todos os usuários da carga usam a senha **`senha123`**.

| Usuário | E-mail | Situação |
|---|---|---|
| Carlos Menezes | `carlos@boragora.local` | Motorista: Manhã · Van 1 e Noite · Van 1 |
| Vanderlei Dias | `vanderlei@boragora.local` | Motorista: Manhã · Van 2 e Noite · Van 2 |
| João Gabriel | `joao@boragora.local` | Aluno: Manhã · Van 1 e Noite · Van 2 |
| Beatriz Souza, Camila Rocha, Larissa Costa, Lucas Pereira, Mariana Alves, Pedro Henrique Dias, Rafael Lima | `beatriz@`, `camila@`, `larissa@`, `lucas@`, `mariana@`, `pedro@`, `rafael@` + `boragora.local` | Alunos da Noite · Van 2 |
| Ana Beatriz Moura, Diego Farias, Vitor Sampaio | `ana@`, `diego@`, `vitor@` + `boragora.local` | Alunos sem vínculo (telas D6 e D10) |

Grupos: **Manhã** (ida 06:20, volta 12:10) e **Noite** (ida 18:10, volta 22:30).

A carga também cria as confirmações **do dia em que roda** na Noite · Van 2, iguais às da tela C1: 5 vão, 7 voltam, 2 liberados e 1 cancelamento tardio (Camila Rocha); Rafael Lima não declarou nada. No dia seguinte, as listas recomeçam vazias.

- A carga roda uma vez só: se qualquer e-mail da tabela já existir no banco, ela é pulada.
- Para recriá-la: `docker compose down -v && docker compose up -d` e suba a API de novo.
- Para os testes manuais, `scripts/banco-de-testes/` recria o mesmo cenário com as confirmações no dia de hoje, quantas vezes for preciso e sem apagar as tabelas. Veja o `LEIA-ME.txt` da pasta.
- Para não criá-la: `APP_DEMONSTRACAO=false`.

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
| `ENDERECO` | `0.0.0.0` | Interface em que a API escuta. `0.0.0.0` aceita conexões da rede local (celular) |
| `APP_RELOGIO_FIXO` | vazio (relógio real) | Congela o relógio da API, ex.: `2026-10-05T18:30` (fuso America/Sao_Paulo). Só no `dev` |
| `APP_DEMONSTRACAO` | `true` | Cria a carga de demonstração na primeira subida. Só no `dev` |
| `LEMBRETE_ANTECEDENCIA_MINUTOS` | `120` | Quantos minutos antes da saída da ida o lembrete do plano é enviado |
| `LEMBRETE_DIAS` | `MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY` | Dias em que o lembrete é enviado |
| `EXPO_PUSH_URL` | `https://exp.host/--/api/v2/push/send` | Endereço do Expo Push Service |

### Testando regras de horário com o relógio fixo

Várias regras dependem da hora atual (prazos de confirmação, viagens do dia). Para testá-las sem esperar o horário real:

```bash
APP_RELOGIO_FIXO=2026-10-05T18:30 ./gradlew bootRun
```

No PowerShell:

```powershell
$env:APP_RELOGIO_FIXO="2026-10-05T18:30"; .\gradlew.bat bootRun
```

O relógio fica congelado: para mudar de horário, pare a API, troque o valor e suba de novo. As regras usam sempre o horário do servidor (uma ação "até a saída" é aceita enquanto agora < horário da saída).

**Roteiro com a carga de demonstração.** A carga grava as confirmações no dia em que roda, então use o mesmo dia em todas as subidas. Suba com o banco limpo e `APP_RELOGIO_FIXO=2026-10-05T19:00`, e percorra as telas:

| Horário | O que ver | Como |
|---|---|---|
| 19:00 | Lista da ida com o cancelamento tardio da Camila (C1) e lista da volta (C2) | Login com `vanderlei@boragora.local`, `GET /motorista/turmas` e `GET /turmas/{id}/hoje` |
| 19:00 | Grupo do aluno com o selo "Você" (B8, B8b) e plano depois da saída (B3) | Login com `joao@boragora.local`, `GET /grupos/{id}/hoje` e `GET /aluno/turmas/{id}/plano` |
| 19:00 | Ida bloqueada (B6) | Login com `rafael@boragora.local` e `GET /aluno/turmas/{id}/plano` |
| 17:00 | Plano antes da saída (B1 e B2), cancelar sem aviso | Suba com `2026-10-05T17:00` e use os `PUT` do plano |
| 22:30 | Dia encerrado (B7): qualquer `PUT` devolve 422 `DIA_ENCERRADO` | Suba com `2026-10-05T22:30` |

As telas de gestão (D1 a D10) usam o admin padrão e não dependem do horário.

## Testes

```bash
./gradlew test
```

Os testes de integração usam **Testcontainers**: cada execução sobe um MySQL descartável próprio. Por isso o Docker precisa estar rodando, mas o `docker compose up` não é necessário para os testes.

## Notificações push

O app registra o token do Expo com `PUT /me/push-token` depois do login e o remove com `DELETE /me/push-token` ao sair. A API envia dois tipos de push pelo Expo Push Service, os dois com o campo `data` (`tipo` e `turmaId`) que o app usa para abrir a tela certa:

| Push | Quando | Para quem |
|---|---|---|
| `CANCELAMENTO_TARDIO` | O aluno cancela a ida depois da saída | Só o motorista da turma do aluno |
| `LEMBRETE` | 2 horas antes da saída da ida, de segunda a sexta (configurável) | Alunos da turma que não marcaram ida nem volta |

Usuário sem token é ignorado, e falha no envio só vai para o log. O lembrete depende de a API estar no ar no minuto do disparo: se estiver fora, o lembrete daquele dia não é enviado. Para receber push de verdade é preciso um development build do app (o Expo Go não recebe push remoto).

## Acessando a API pelo celular (app em Expo)

O celular não enxerga o `localhost` do seu computador. A API escuta em `0.0.0.0`, e o app descobre sozinho o IP da máquina que roda o Expo e chama a API na porta 8080. Para isso funcionar:

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
| `Cannot find a Java installation ... languageVersion=21` / erro de toolchain | O Gradle não achou o JDK 21. Instale-o (ex.: `sudo apt install openjdk-21-jdk`); ele não precisa ser o Java padrão, porque o Gradle procura sozinho os JDKs instalados. Confira com `./gradlew -q javaToolchains`. |
| `permission denied: ./gradlew` (Linux/macOS) | `chmod +x gradlew` |

## Estrutura e documentação

```
src/main/java/br/com/boragora/api/   código da API, por domínio:
    config/          relógio, segurança, OpenAPI, carga de demonstração
    seguranca/       login, cadastro, JWT, controle de acesso
    usuario/         usuários, /me, motoristas, push token
    grupo/           grupos
    turma/           turmas, vínculos de alunos, regra de conflito no grupo (RN08)
    viagem/          plano do dia do aluno e listagens do dia
    notificacao/     Expo Push, cancelamento tardio e lembrete
    compartilhado/   erros de negócio e formato padrão de erro
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

1. Atualize a `main` e crie uma branch a partir dela: `git checkout -b minha-alteracao`.
2. Siga a etapa em andamento no `docs/plano-implementacao-backend.md`. As etapas são feitas na ordem.
3. Rode `./gradlew test` antes de abrir o PR.
4. Marque no `docs/checklist-backend.md` os itens concluídos.
5. Abra um Pull Request para a `main`.

Mudanças no esquema do banco sempre entram por uma **nova** migração Flyway (`V2__...sql`, `V3__...sql`, ...), nunca editando uma que já existe.
