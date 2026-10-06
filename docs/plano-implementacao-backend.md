# Plano de Implementação do Backend
## Boragora Viagens — App de Controle de Passageiros de Van

**Base:** documento de escopo v7, MER (`mer_ajustado_app_van_dark.html`) e projeto de telas "Boragora Viagens" (telas A1 a E1).

**Para que serve:** guiar a construção da API etapa por etapa (inclusive pelo Claude Code) e permitir que a equipe valide o que será construído antes e depois de cada etapa.

**Versão:** 3 — turma com nome em texto livre (ver changelogs no final).

**Acompanhamento:** o progresso é registrado em `checklist-backend.md`.

**Como ler:** a seção 1 registra as decisões confirmadas pela equipe. A seção 2 fixa as decisões técnicas. As seções 3 e 4 são o plano em si. As seções 5 e 6 servem para conferir que nenhuma tela e nenhum requisito ficou sem cobertura.

---

## 1. Decisões confirmadas pela equipe

Ao cruzar escopo, MER e telas, surgiram dez pontos que dependiam de decisão. Todos foram respondidos pela equipe e estão incorporados nas etapas indicadas.

| # | Ponto | Decisão | Efeito no backend | Etapa |
|---|---|---|---|---|
| 1 | Botão "Abrir WhatsApp" (telas B5 e B6) | Sem botão por enquanto. O app mostra apenas o aviso para o aluno falar com o motorista. | Nenhum. `USUARIO` continua sem telefone. | — |
| 2 | Alterações depois da saída da volta | Ida e volta ficam restritas aos horários. Depois da saída da volta nada é alterado, inclusive o cancelamento da ida. | Erro 422 `DIA_ENCERRADO`. | 6 |
| 3 | Destinatário do push de cancelamento tardio | O motorista responsável pela van (turma) do aluno. | Envio apenas para `turma.id_motorista`. | 8 |
| 4 | Lembrete do plano do dia | 2 horas antes da saída da ida. | Padrão de 120 minutos, configurável. Dias de envio: segunda a sexta, também configurável (a equipe não comentou os dias; mantido o padrão). | 8 |
| 5 | Confirmação antes do cancelamento tardio | Validada apenas pela interface (tela B4). | Nenhum. | — |
| 6 | Gerenciamento de grupos e turmas | CRUD completo: criar, consultar, atualizar e excluir grupos e turmas; incluir e remover alunos; definir e trocar o motorista; atualizar dados cadastrais. | Rotas de exclusão de grupo e de turma, com as regras descritas abaixo. | 3, 4 e 5 |
| 7 | Remover aluno da turma | Desativa o vínculo (`ativo = false`). Vincular de novo reativa o mesmo registro. | Sem exclusão física em `TURMA_ALUNO`. | 5 |
| 8 | Identificação da turma | Nome em texto livre, definido pelo motorista-chefe. | Nova coluna `nome` em `TURMA` (alteração no MER). Sem validação de unicidade. | 1 e 4 |
| 9 | API externa (RNF04) | Serviço de push do Expo. Geolocalização fica fora (seção 7). | Integração HTTP com o Expo. | 8 |
| 10 | Duração da sessão | Token JWT com validade de 1 dia, sem refresh token. | Token expirado devolve 401 e o app volta para a tela de login. | 2 |

**Definições que decorrem dessas respostas:**

- **Nome da turma (ponto 8).** A tabela `TURMA` ganha a coluna `nome` (texto livre, obrigatório, até 60 caracteres, ex.: "Van 2" ou "Van da Zona Sul"). É o nome que identifica a turma em todas as telas e notificações, no lugar de "Van N". Nomes podem se repetir. As colunas `numero_van` e `capacidade` permanecem como dados cadastrais da van, sem uso em regras. O MER foi atualizado com a nova coluna.
- **Regras de exclusão (ponto 6), ainda a conferir pela equipe.** O MER não tem campo `ativo` em `GRUPO` nem em `TURMA`, então a exclusão é física. Um grupo só pode ser excluído se não tiver turmas. Excluir uma turma apaga junto os vínculos de alunos, as viagens e as confirmações dela, o que é coerente com o histórico estar fora de escopo. A alternativa seria bloquear a exclusão de turmas que já tiveram viagem, mas isso tornaria quase toda turma impossível de excluir.

**Alinhamento com o escopo:** as decisões desta seção já estão incorporadas ao documento de escopo v7 (RN15 a RN18, RF01, RF02, RF03, RF10, RF13, RF14, RNF03 e RNF04). Se ainda assim houver divergência entre os dois documentos, parar e perguntar.

---

## 2. Decisões técnicas

### 2.1 Stack

| Item | Escolha |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot, versão estável atual, projeto gerado pelo Spring Initializr com Gradle |
| Dependências (nomes no Initializr) | Spring Web, Spring Data JPA, Validation, Spring Security, OAuth2 Resource Server, Flyway Migration, MySQL Driver |
| Documentação da API | springdoc-openapi (Swagger UI) |
| Banco | MySQL 8, em Docker Compose no ambiente local |
| Migrações | Flyway. O Hibernate apenas valida o esquema (`ddl-auto=validate`) |
| Autenticação | JWT assinado com HS256 (chave em variável de ambiente), senha com BCrypt |
| Push | Expo Push Service, chamado por HTTP |
| Testes | JUnit 5, Mockito, MockMvc e Testcontainers (MySQL) |

### 2.2 Estrutura de pacotes

Pacote base sugerido: `br.com.boragora.api`. Organização por funcionalidade, cada pacote com seu controller, service, repository, entidade e DTOs.

```
config/         relógio, segurança, OpenAPI, propriedades
seguranca/      login, cadastro, JWT, usuário autenticado, controle de acesso
usuario/        entidade USUARIO, /me, push token
grupo/          entidade GRUPO, cadastro e consulta
turma/          entidades TURMA e TURMA_ALUNO, cadastro, vínculos, validador da RN08
viagem/         entidades VIAGEM e CONFIRMACAO, plano do dia, listagens do dia
notificacao/    gateway do Expo, push de cancelamento tardio, lembrete agendado
compartilhado/  exceções de negócio e tratamento global de erros
```

### 2.3 Convenções que valem para todas as etapas

1. **Regra de negócio fica no service, nunca no controller nem só no app** (RNF08). O controller só recebe, valida formato e delega.
2. **O horário é sempre o do servidor.** Existe um único bean `Clock` no fuso `America/Sao_Paulo`, injetado em todo service que depende de hora. Nenhum service chama `LocalDateTime.now()` diretamente. Isso permite testar as regras de horário com um relógio fixo.
3. **"Hoje" é definido pelo servidor.** Os endpoints do plano do dia e das listagens não recebem data.
4. **Limite de horário:** uma ação "até o horário de saída" é aceita enquanto `agora < horário`. No minuto exato da saída, já está bloqueada.
5. **Entidades JPA não saem da API.** Toda entrada e saída usa DTO (records).
6. **Formatos:** datas em `yyyy-MM-dd`, horários em `HH:mm`, enums em maiúsculas (`EM_AULA`, `LIBERADO`, `MANHA`, `TARDE`, `NOITE`).
7. **Nomes do domínio em português**, iguais aos do MER e do escopo.
8. **Erro padronizado** em todas as respostas de falha:

```json
{ "codigo": "IDA_ENCERRADA", "mensagem": "Não dá mais para incluir a ida pelo app. Fale com o motorista pelo WhatsApp.", "campos": null }
```

| HTTP | Código | Quando |
|---|---|---|
| 400 | `VALIDACAO` | Campo ausente ou em formato inválido (`campos` traz o detalhe) |
| 401 | `CREDENCIAIS_INVALIDAS` / `NAO_AUTENTICADO` | Login errado, token ausente, inválido ou expirado |
| 403 | `ACESSO_NEGADO` | Usuário sem permissão para o recurso |
| 404 | `NAO_ENCONTRADO` | Recurso inexistente |
| 409 | `EMAIL_JA_CADASTRADO` | Cadastro com e-mail existente |
| 409 | `CONFLITO_GRUPO_ALUNO` / `CONFLITO_GRUPO_MOTORISTA` | Violação da RN08 |
| 409 | `GRUPO_COM_TURMAS` | Excluir grupo que ainda tem turmas |
| 422 | `IDA_ENCERRADA` | Incluir ida após a saída da ida (RN02) |
| 422 | `VOLTA_NAO_INCLUIDA` | Alterar status sem a volta no plano (RN07) |
| 422 | `DIA_ENCERRADO` | Qualquer alteração após a saída da volta (RN03, RN07, RN14) |

### 2.4 Autorização

| Prefixo | Quem acessa |
|---|---|
| `/auth/**` | Público |
| `/me/**` | Qualquer usuário autenticado |
| `/aluno/**` | Papel `ALUNO` |
| `/motorista/**` | Papel `MOTORISTA` (inclui o motorista-chefe) |
| `/admin/**` | `MOTORISTA` com `admin = true` |
| `/grupos/**`, `/turmas/**` | Autenticado, com checagem por recurso (abaixo) |

A checagem por recurso fica em um único componente, `ControleDeAcesso`, com as regras de visibilidade:

- **Aluno** vê os grupos em que tem vínculo ativo e todas as turmas desses grupos (RN10).
- **Motorista** vê os grupos em que conduz alguma turma e todas as turmas desses grupos (RN11).
- **Motorista-chefe** vê tudo (RN12).

---

## 3. Visão geral das etapas

A ordem segue as dependências: cada etapa só usa o que as anteriores entregaram.

| Etapa | Entrega | Depende de | Telas atendidas |
|---|---|---|---|
| 0 | Projeto, banco local, relógio, erros padronizados | — | — |
| 1 | Esquema do banco e entidades | 0 | — |
| 2 | Cadastro, login, JWT, papéis | 1 | A1, A2, B9 |
| 3 | Grupos (CRUD) | 2 | D1, D2 |
| 4 | Motoristas e turmas (CRUD) | 3 | D4, D5, D7 |
| 5 | Vínculo e remoção de alunos | 4 | D5, D6 |
| 6 | Plano do dia do aluno | 5 | A3, tela inicial do aluno, B1 a B7 |
| 7 | Listagens do dia e visibilidade | 6 | B8, B8a, B8b, C0 a C5, D3 |
| 8 | Notificações push | 6 | E1 |
| 9 | Dados de demonstração e fechamento | todas | — |

Ao fim da etapa 5 o motorista-chefe já consegue montar toda a operação. A etapa 6 é o núcleo do sistema e a que mais merece testes. A etapa 8 pode ser feita em paralelo com a 7.

---

## 4. Etapas

### Etapa 0 — Fundação

**Objetivo:** ter um projeto que sobe, conecta no banco e já responde erros no formato padrão.

**Construir:**
- Projeto Spring Boot com Gradle e as dependências da seção 2.1.
- `docker-compose.yml` com MySQL 8 (banco, usuário e senha de desenvolvimento).
- `application.yml` com perfis `dev` e `test`. Segredos (senha do banco, chave do JWT, credenciais do admin inicial) vêm de variáveis de ambiente.
- Bean `Clock` no fuso `America/Sao_Paulo`. No perfil `dev`, a propriedade opcional `app.relogio.fixo` (ex.: `2026-10-05T18:30`) congela o relógio, para testar manualmente as regras de horário sem esperar a hora real.
- Exceção base `RegraDeNegocioException` (com código e status HTTP) e `@RestControllerAdvice` que converte exceções no formato da seção 2.3.
- Swagger UI habilitado.
- `GET /health` público.

**Pronto quando:**
- `docker compose up` e `./gradlew bootRun` sobem sem erro.
- `GET /health` responde 200.
- Uma rota inexistente responde 404 no formato padrão de erro.

---

### Etapa 1 — Banco de dados e entidades

**Objetivo:** reproduzir o MER no MySQL e mapeá-lo em JPA.

**Construir:**
- Migração `V1__criar_esquema.sql` com as seis tabelas do MER, exatamente com os mesmos nomes de tabela e coluna:

| Tabela | Colunas | Restrições |
|---|---|---|
| `usuario` | `id`, `nome_pessoa`, `email`, `senha_hash`, `role`, `admin`, `expo_push_token` (anulável), `ativo` | `email` único |
| `grupo` | `id`, `nome`, `periodo`, `horario_saida_ida`, `horario_saida_volta` | — |
| `turma` | `id`, `id_grupo`, `id_motorista`, `nome`, `numero_van`, `capacidade` | único (`id_grupo`, `id_motorista`); `nome` obrigatório, `VARCHAR(60)`, sem unicidade |
| `turma_aluno` | `id`, `id_turma`, `id_aluno`, `ativo` | único (`id_turma`, `id_aluno`) |
| `viagem` | `id`, `id_turma`, `data`, `id_viagem_absorvida_por` (anulável) | único (`id_turma`, `data`) |
| `confirmacao` | `id`, `id_viagem`, `id_turma_aluno`, `inclui_ida`, `inclui_volta`, `status_volta`, `cancelou_apos_saida`, `atualizado_em` | único (`id_viagem`, `id_turma_aluno`) |

- Chaves estrangeiras em todas as colunas `id_*`.
- Para a exclusão de turma (etapa 4), as chaves `turma_aluno.id_turma`, `viagem.id_turma`, `confirmacao.id_viagem` e `confirmacao.id_turma_aluno` usam `ON DELETE CASCADE`, e `viagem.id_viagem_absorvida_por` usa `ON DELETE SET NULL`. As chaves `turma.id_grupo`, `turma.id_motorista` e `turma_aluno.id_aluno` não têm cascata.
- Enums gravados como texto (`VARCHAR`), mapeados com `@Enumerated(EnumType.STRING)`.
- Padrões: `admin = false`, `ativo = true`, `inclui_ida = false`, `inclui_volta = false`, `status_volta = 'EM_AULA'`, `cancelou_apos_saida = false`.
- Entidades JPA e enums: `Papel` (`MOTORISTA`, `ALUNO`), `Periodo` (`MANHA`, `TARDE`, `NOITE`), `StatusVolta` (`EM_AULA`, `LIBERADO`).
- Um repositório Spring Data por entidade.
- `id_viagem_absorvida_por` existe na tabela e na entidade, mas nenhuma funcionalidade o utiliza (seção 9.1 do escopo).

**Pronto quando:**
- A aplicação sobe com `ddl-auto=validate` sem erro.
- Um teste de integração grava e lê cada entidade e confirma que as restrições de unicidade rejeitam duplicatas.
- Um teste confirma que apagar uma turma apaga seus vínculos, viagens e confirmações, e que apagar um grupo com turmas é rejeitado pelo banco.

---

### Etapa 2 — Autenticação e usuários

**Objetivo:** permitir cadastro de aluno, login e identificação do usuário em todas as rotas.

**Endpoints:**

| Método e rota | Uso | Corpo | Resposta |
|---|---|---|---|
| `POST /auth/cadastro` | Tela A2 | `nome`, `email`, `senha` | `token` e `usuario` |
| `POST /auth/login` | Tela A1 | `email`, `senha` | `token` e `usuario` |
| `GET /me` | Cabeçalhos e tela de conta | — | `id`, `nome`, `email`, `papel`, `admin` |

**Regras:**
- O cadastro público cria sempre um usuário com papel `ALUNO`, `admin = false`, sem vínculo com turma (RF07). O papel nunca é aceito no corpo da requisição.
- E-mail normalizado em minúsculas e único. Senha com no mínimo 6 caracteres, gravada com BCrypt.
- Login com e-mail inexistente, senha errada ou usuário inativo devolve o mesmo 401 `CREDENCIAIS_INVALIDAS`.
- O token expira em 1 dia (`app.jwt.validade-horas`, padrão 24). Token expirado devolve 401 `NAO_AUTENTICADO` e o app volta para a tela de login. O `expo_push_token` continua gravado no servidor, então o lembrete chega mesmo com a sessão expirada.
- O token carrega o id do usuário. A cada requisição o usuário é recarregado do banco, para que `ativo` e `admin` valham imediatamente.
- Autoridades: `ROLE_ALUNO` ou `ROLE_MOTORISTA`, mais `ROLE_ADMIN` quando `admin = true`. Regras de rota conforme a seção 2.4.
- **Motorista-chefe inicial:** na subida da aplicação, se não existir nenhum usuário com `admin = true`, cria um a partir das variáveis `ADMIN_NOME`, `ADMIN_EMAIL` e `ADMIN_SENHA`. É a única forma de o primeiro admin existir, já que o cadastro público só cria alunos.

**Pronto quando:**
- Cadastro seguido de login devolve token válido, e `GET /me` com esse token devolve o usuário.
- Cadastro com e-mail repetido devolve 409.
- Rota protegida sem token ou com token expirado devolve 401; aluno em rota `/admin/**` recebe 403.
- O admin inicial é criado uma única vez.

---

### Etapa 3 — Grupos: CRUD (motorista-chefe)

**Objetivo:** cadastrar os turnos e seus horários, que são os marcos de todas as regras do dia.

**Endpoints:**

| Método e rota | Uso | Observação |
|---|---|---|
| `POST /admin/grupos` | Tela D2 | `nome`, `periodo`, `horarioSaidaIda`, `horarioSaidaVolta` |
| `PUT /admin/grupos/{id}` | Tela D2 em modo edição | Mesmos campos |
| `DELETE /admin/grupos/{id}` | Excluir grupo | Só se o grupo não tiver turmas |
| `GET /grupos` | Telas D1, B8a e C3a | Devolve os grupos visíveis para quem chama (seção 2.4) |
| `GET /grupos/{id}` | Carregar o formulário de edição | Respeita a visibilidade da seção 2.4 |

**Regras:**
- Todos os campos obrigatórios. `horarioSaidaIda` deve ser anterior a `horarioSaidaVolta` (o sistema não trata turno que atravessa a meia-noite).
- **Exclusão:** um grupo com pelo menos uma turma não pode ser excluído (409 `GRUPO_COM_TURMAS`). O motorista-chefe exclui as turmas antes. Sem turmas, o registro é apagado.
- **Edição de horários:** os marcos do dia são sempre lidos do grupo no momento da ação, então uma mudança de horário vale imediatamente, inclusive para o dia em andamento.
- Cada item de `GET /grupos` traz: `id`, `nome`, `periodo`, horários e `totalTurmas`. Para aluno e motorista, traz também `minhaTurma` (id e nome da turma dele naquele grupo), usado em "Sua turma: Van 2 · 2 turmas no grupo".

**Pronto quando:**
- O admin cria, consulta, edita e exclui grupo; não-admin recebe 403 nas rotas `/admin/grupos`.
- Excluir grupo com turma devolve 409; excluir grupo vazio devolve 204 e ele some de `GET /grupos`.
- Horário de ida igual ou posterior ao de volta devolve 400.
- `GET /grupos` devolve todos os grupos para o admin e lista vazia para um aluno sem vínculo.

---

### Etapa 4 — Motoristas e turmas: CRUD (motorista-chefe)

**Objetivo:** cadastrar motoristas e montar as turmas (van + motorista + grupo).

**Endpoints:**

| Método e rota | Uso | Observação |
|---|---|---|
| `POST /admin/motoristas` | Tela D7 | `nome`, `email`, `senha`. Cria usuário com papel `MOTORISTA`, `admin = false` |
| `GET /admin/motoristas` | Seletor das telas D4 e D5 | Motoristas ativos, incluindo o chefe |
| `POST /admin/turmas` | Tela D4 | `grupoId`, `nome`, `numeroVan`, `capacidade`, `motoristaId` |
| `PUT /admin/turmas/{id}` | Tela D5: dados cadastrais e botão "Trocar" | `nome`, `numeroVan`, `capacidade`, `motoristaId` |
| `DELETE /admin/turmas/{id}` | Excluir turma | Apaga a turma e tudo o que depende dela |
| `GET /turmas/{id}` | Telas C4 e D5 | Dados da turma, grupo, motorista e alunos com vínculo ativo, em ordem alfabética |

**Regras:**
- **RN08 para motoristas:** um motorista não pode conduzir duas turmas do mesmo grupo. A validação fica em um componente único, `ValidadorConflitoDeGrupo`, usado na criação da turma e na troca de motorista. A restrição única (`id_grupo`, `id_motorista`) é a segunda barreira. Violação devolve 409 `CONFLITO_GRUPO_MOTORISTA`.
- `motoristaId` precisa ser um usuário ativo com papel `MOTORISTA`.
- `capacidade` é apenas cadastral: nenhuma regra a consulta.
- `nome` é texto livre e obrigatório: sem espaços nas pontas, de 1 a 60 caracteres. Pode se repetir, inclusive no mesmo grupo (ponto 8 da seção 1). É o nome exibido em todas as respostas que trazem uma turma.
- `numeroVan` é apenas cadastral, como a capacidade, e também pode se repetir.
- **Exclusão:** apagar a turma apaga também seus vínculos de alunos, viagens e confirmações (cascata definida na etapa 1). A confirmação antes de excluir é responsabilidade do app. Contas de motorista e de aluno não são afetadas.
- O grupo de uma turma não muda depois de criada.
- `GET /turmas/{id}` respeita a visibilidade da seção 2.4.

**Pronto quando:**
- O admin cadastra motorista, cria turma, altera os dados cadastrais e troca o motorista.
- Excluir uma turma devolve 204, remove seus dependentes e libera o motorista e os alunos para outra turma do mesmo grupo.
- Turma sem nome, com nome em branco ou com mais de 60 caracteres devolve 400.
- Duas turmas com o mesmo `nome` ou o mesmo `numeroVan` no mesmo grupo são aceitas.
- Alterar o nome pela edição reflete em `GET /turmas/{id}`.
- Colocar o mesmo motorista em duas turmas do mesmo grupo devolve 409, tanto na criação quanto na troca.
- O mesmo motorista em turmas de grupos diferentes é aceito (caso do Vanderlei nas telas: Manhã e Noite).

---

### Etapa 5 — Vínculo e remoção de alunos (motorista-chefe)

**Objetivo:** vincular contas de aluno já existentes às turmas.

**Endpoints:**

| Método e rota | Uso | Observação |
|---|---|---|
| `GET /admin/alunos?busca=&turmaId=` | Tela D6 | Alunos ativos, filtrados por nome ou e-mail, com a situação de cada um em relação à turma informada |
| `POST /admin/turmas/{id}/alunos` | Tela D6, "Vincular N alunos" | `alunoIds` (lista). Tudo ou nada, em uma transação |
| `DELETE /admin/turmas/{id}/alunos/{alunoId}` | Tela D5, botão de remover | Desativa o vínculo |

**Regras:**
- **RN08 para alunos:** um aluno não pode ter vínculo ativo em duas turmas do mesmo grupo. Usa o mesmo `ValidadorConflitoDeGrupo` da etapa 4. Violação devolve 409 `CONFLITO_GRUPO_ALUNO`, informando qual aluno causou o conflito.
- Só usuários ativos com papel `ALUNO` podem ser vinculados (RF03).
- Se já existir um vínculo inativo para o par turma e aluno, ele é reativado em vez de criar outro registro.
- Em `GET /admin/alunos`, cada item traz `id`, `nome`, `email`, `turmas` (as turmas atuais, ex.: "Manhã · Van 2") e `situacao` em relação à turma informada:
  - `DISPONIVEL`: pode ser vinculado (tela mostra "Sem turma" ou a turma de outro grupo);
  - `CONFLITO_NO_GRUPO`: já está em outra turma do mesmo grupo (tela mostra "Já está na Van 1 deste grupo", com a caixa desabilitada). Traz o nome da turma em conflito;
  - alunos já vinculados à própria turma não são listados.

**Pronto quando:**
- Vincular dois alunos de uma vez funciona e eles aparecem em `GET /turmas/{id}`.
- Vincular um aluno que já está em outra turma do mesmo grupo devolve 409 e ninguém do lote é vinculado.
- Um aluno pode estar em turmas de grupos diferentes (caso do João nas telas: Manhã · Van 1 e Noite · Van 2).
- Remover e vincular de novo reutiliza o mesmo registro.

---

### Etapa 6 — Plano do dia (aluno)

**Objetivo:** implementar o núcleo do sistema: declarar ida e volta, cancelar e atualizar o status da volta, com todos os limites de horário.

**Endpoints:**

| Método e rota | Uso | Corpo |
|---|---|---|
| `GET /aluno/turmas` | Tela inicial do aluno, A3 e B9 | — |
| `GET /aluno/turmas/{turmaId}/plano` | Telas B1 a B7 | — |
| `PUT /aluno/turmas/{turmaId}/plano/ida` | Marcar ou desmarcar a ida | `{ "incluir": true }` |
| `PUT /aluno/turmas/{turmaId}/plano/volta` | Marcar ou desmarcar a volta | `{ "incluir": true }` |
| `PUT /aluno/turmas/{turmaId}/plano/status-volta` | "Em aula" ou "Liberado" | `{ "status": "LIBERADO" }` |

Os três `PUT` devolvem o plano atualizado, no mesmo formato do `GET`.

**`GET /aluno/turmas`:** lista as turmas em que o aluno tem vínculo ativo, cada uma com nome da turma, grupo, horários, nome do motorista e o resumo do plano de hoje (`incluiIda`, `incluiVolta`). Lista vazia corresponde à tela A3 ("Você ainda não está em uma van").

**Formato do plano:**

```json
{
  "data": "2026-10-05",
  "turma": {
    "id": 2,
    "nome": "Van 2",
    "grupo": { "id": 1, "nome": "Noite", "periodo": "NOITE", "horarioSaidaIda": "18:10", "horarioSaidaVolta": "22:30" }
  },
  "incluiIda": true,
  "incluiVolta": true,
  "statusVolta": "EM_AULA",
  "cancelouAposSaida": false,
  "idaSaiu": true,
  "voltaSaiu": false,
  "acoes": {
    "podeIncluirIda": false,
    "podeCancelarIda": true,
    "cancelamentoIdaSeraTardio": true,
    "podeAlterarVolta": true,
    "podeAlterarStatusVolta": true
  }
}
```

O bloco `acoes` é calculado no servidor para o app não repetir a lógica de horário nem depender do relógio do celular. Mesmo assim, cada `PUT` revalida tudo (RNF08).

**Como o app usa a resposta para escolher a tela:**

| Tela | Condição |
|---|---|
| B1 — sem plano | nada marcado, `idaSaiu = false` |
| B2 — plano declarado | algo marcado, `idaSaiu = false` |
| B3 — depois da saída | `incluiIda = true`, `idaSaiu = true`, `voltaSaiu = false` |
| B4 — confirmação | exibida pelo app antes de desmarcar a ida quando `cancelamentoIdaSeraTardio = true` |
| B5 — ida cancelada | `cancelouAposSaida = true` |
| B6 — ida bloqueada | `incluiIda = false`, `cancelouAposSaida = false`, `idaSaiu = true` |
| B7 — dia encerrado | `voltaSaiu = true` |

**Regras (todas no `PlanoDoDiaService`):**

| Ação | Condição | Resultado |
|---|---|---|
| Incluir ida | antes da saída da ida | `inclui_ida = true` (RN02) |
| Incluir ida | a partir da saída da ida | 422 `IDA_ENCERRADA`. Vale também para quem cancelou depois da saída (RN02) |
| Cancelar ida | ida marcada, antes da saída da ida | `inclui_ida = false`. O aluno sai da lista da ida (RN05) |
| Cancelar ida | ida marcada, entre a saída da ida e a saída da volta | `inclui_ida = false`, `cancelou_apos_saida = true`, publica o evento `IdaCanceladaAposSaida` (RN05) |
| Incluir volta | antes da saída da volta, inclusive depois da saída da ida | `inclui_volta = true`, `status_volta = EM_AULA` (RN03) |
| Cancelar volta | antes da saída da volta | `inclui_volta = false`, `status_volta` volta para `EM_AULA` (RN14) |
| Alterar status da volta | volta marcada, antes da saída da volta | grava o novo status (RN07) |
| Alterar status da volta | volta não marcada | 422 `VOLTA_NAO_INCLUIDA` |
| Qualquer alteração | a partir da saída da volta | 422 `DIA_ENCERRADO` (ponto 2 da seção 1) |

**Detalhes de implementação:**
- **Pré-condição de todas as ações:** o aluno autenticado precisa ter vínculo ativo com a turma. Caso contrário, 403.
- **Viagem sob demanda:** a `VIAGEM` de hoje e a `CONFIRMACAO` do aluno são criadas na primeira vez em que ele marca ida ou volta. O `GET` do plano nunca cria registros: sem confirmação, devolve tudo como falso.
- **Concorrência:** dois alunos podem fazer a primeira confirmação do dia ao mesmo tempo. Se a criação da viagem falhar pela restrição única (`id_turma`, `data`), o service busca a viagem já criada e segue.
- **Ações repetidas não causam efeito:** marcar o que já está marcado, ou desmarcar o que já está desmarcado, devolve 200 sem alterar nada. Em especial, desmarcar a ida que não estava marcada nunca gera cancelamento tardio nem push.
- **Ausência (RN04):** não existe estado "pendente". Uma confirmação com as duas opções falsas equivale a não ter confirmação.
- `atualizado_em` é gravado a cada alteração, usando o `Clock`.
- O evento `IdaCanceladaAposSaida` é apenas publicado nesta etapa. Quem o consome é a etapa 8.

**Pronto quando** (testes unitários do service com relógio fixo, grupo com ida 18:10 e volta 22:30):

| Horário | Ação | Esperado |
|---|---|---|
| 17:00 | incluir ida e volta | plano com as duas marcadas, status `EM_AULA`; viagem criada |
| 17:30 | cancelar ida | ida desmarcada, `cancelouAposSaida = false`, nenhum evento |
| 18:09 | incluir ida | aceito |
| 18:10 | incluir ida | 422 `IDA_ENCERRADA` |
| 18:30 | cancelar ida marcada | `cancelouAposSaida = true`, um evento publicado |
| 18:31 | incluir ida de novo | 422 `IDA_ENCERRADA` |
| 18:30 | cancelar ida não marcada | 200 sem alteração, nenhum evento |
| 19:00 | incluir volta | aceito |
| 21:00 | status `LIBERADO` com volta marcada | aceito |
| 21:00 | status `LIBERADO` sem volta marcada | 422 `VOLTA_NAO_INCLUIDA` |
| 22:29 | cancelar volta | aceito |
| 22:30 | qualquer alteração | 422 `DIA_ENCERRADO` |
| qualquer | aluno sem vínculo ativo com a turma | 403 |

---

### Etapa 7 — Listagens do dia e visibilidade

**Objetivo:** entregar as consultas que o motorista, o motorista-chefe e os alunos usam para acompanhar o dia.

**Endpoints:**

| Método e rota | Uso | Quem acessa |
|---|---|---|
| `GET /motorista/turmas` | Tela C0 | Motorista: suas turmas, com os contadores de hoje |
| `GET /turmas/{id}/hoje` | Telas C1, C2 e C5 | Conforme a seção 2.4 |
| `GET /grupos/{id}/hoje` | Telas B8, B8b e C3 | Conforme a seção 2.4 |
| `GET /admin/turmas/hoje` | Tela D3 | Admin: todas as turmas, agrupadas por grupo, com contadores |

**Formato comum de "turma hoje":**
- Dados da turma: `id`, `nome`, nome do motorista, grupo e horários.
- `contadores`: `vaoNaIda`, `voltam`, `liberados`, `canceladosAposSaida`.
- `semConfirmacoes`: verdadeiro quando não há viagem hoje ou ninguém está marcado (tela C5 e "Sem confirmações hoje" na D3).
- `listaIda`: itens com `alunoId`, `nome` e `situacao` (`CONFIRMADO` ou `CANCELADO_APOS_SAIDA`).
- `listaVolta`: itens com `alunoId`, `nome` e `statusVolta`.

Em `GET /grupos/{id}/hoje`, cada turma traz também `minhaTurma` (verdadeiro ou falso) e cada item de lista traz `voce` quando é o próprio aluno autenticado (selo "Você" nas telas B8 e B8b).

**Regras:**
- **Lista da ida:** confirmações de hoje com `inclui_ida = true` **ou** `cancelou_apos_saida = true`. Quem cancelou depois da saída aparece com `CANCELADO_APOS_SAIDA` (RF06, RN05). Ordem: cancelados primeiro, depois alfabética.
- **Lista da volta:** confirmações de hoje com `inclui_volta = true`. Ordem: liberados primeiro, depois alfabética.
- **Contadores:** `vaoNaIda` conta só `inclui_ida = true` (o cancelado tardio aparece na lista mas não entra na contagem, como na tela C1: 5 vão, 6 linhas). `liberados` conta `inclui_volta = true` com status `LIBERADO`.
- Quem não declarou o plano não aparece em lugar nenhum (RN04).
- Só entram confirmações de vínculos ativos.
- Nenhuma dessas consultas cria viagem.
- Para evitar consultas em cascata (uma por turma, uma por aluno), buscar as confirmações do dia de todas as turmas pedidas em uma única consulta com `JOIN FETCH`.

**Pronto quando:**
- Com os dados da etapa 6, a turma mostra contadores e listas corretos, incluindo o aluno com cancelamento tardio destacado na ida e fora da contagem.
- Turma sem nenhuma confirmação devolve `semConfirmacoes = true` e listas vazias.
- Aluno consulta o próprio grupo e recebe 403 ao consultar um grupo em que não tem vínculo.
- Motorista consulta turmas do grupo em que atua e recebe 403 fora dele.
- Admin consulta qualquer turma e qualquer grupo.

---

### Etapa 8 — Notificações push

**Objetivo:** avisar o motorista do cancelamento tardio e lembrar o aluno de declarar o plano (RNF05).

**Endpoints:**

| Método e rota | Uso |
|---|---|
| `PUT /me/push-token` | O app envia o token do Expo após o login. Corpo: `{ "token": "ExponentPushToken[...]" }` |
| `DELETE /me/push-token` | O app remove o token ao sair da conta |

**Construir:**
- Interface `PushGateway` com o método de envio (token, título, corpo). A implementação real faz `POST https://exp.host/--/api/v2/push/send`. Nos testes, usa-se uma implementação falsa que registra o que foi enviado.
- Falha no envio é registrada em log e nunca derruba a operação que a originou. Se o Expo responder `DeviceNotRegistered`, o token do usuário é apagado.
- **Push de cancelamento tardio:** um listener do evento `IdaCanceladaAposSaida`, executado só depois do commit da transação (`@TransactionalEventListener` com fase `AFTER_COMMIT`), envia somente ao motorista responsável da turma (ponto 3 da seção 1), mesmo que ele não seja o motorista-chefe:
  - Título: "João Gabriel cancelou a ida para a faculdade!"
  - Corpo: "Van 2 · Noite · depois da saída das 18:10", montado com o nome da turma, o nome do grupo e o horário de saída da ida
- **Lembrete do plano do dia (RF13):** tarefa agendada que roda a cada minuto. Para cada grupo cujo horário de saída da ida menos a antecedência coincide com o minuto atual, nos dias configurados, envia aos alunos com vínculo ativo nas turmas do grupo que **não têm** `inclui_ida` nem `inclui_volta` marcados hoje naquela turma:
  - Título: "Vai de van hoje?"
  - Corpo: "Marque ida e volta até 18:10 para entrar na lista do motorista."
- Configuração: `app.lembrete.antecedencia-minutos` (padrão 120, ou seja, 2 horas) e `app.lembrete.dias` (padrão segunda a sexta).
- Usuários sem token são simplesmente ignorados.

**Limitação conhecida:** o lembrete depende de o servidor estar no ar no minuto do disparo. Se estiver fora do ar, o lembrete daquele dia não é enviado. É aceitável para o contexto do projeto e evita uma tabela de controle que não existe no MER.

**Pronto quando:**
- Cancelar a ida depois da saída gera exatamente um envio ao motorista da turma; cancelar antes da saída não gera nenhum.
- Se o gateway lançar erro, o cancelamento continua gravado.
- Com relógio fixo em 16:10 e grupo com ida às 18:10 (2 horas antes), o lembrete vai só para quem não marcou nada; quem já marcou ida ou volta não recebe.
- Fora dos dias configurados, nada é enviado.

---

### Etapa 9 — Dados de demonstração e fechamento

**Objetivo:** deixar o backend pronto para o time do app consumir e para a apresentação.

**Construir:**
- **Carga de dados no perfil `dev`** reproduzindo o cenário das telas: grupos Manhã (06:20 / 12:10) e Noite (18:10 / 22:30); motoristas Carlos Menezes e Vanderlei Dias; turmas com os nomes "Van 1" e "Van 2" em cada grupo; alunos das telas (João Gabriel, Mariana Alves, Lucas Pereira e os demais) com seus vínculos. Assim o app exibe o mesmo que o design.
- **Teste de integração de ponta a ponta** com MockMvc e Testcontainers: admin cria grupo, motorista e turma; aluno se cadastra; admin vincula; aluno declara o plano; motorista vê a lista; aluno cancela depois da saída; a lista mostra o cancelado.
- **Revisão da RNF08:** conferir que cada regra das etapas 4 a 6 tem um teste que chama a API diretamente, sem depender do app.
- **README** com: como subir o banco e a API, variáveis de ambiente, endereço do Swagger, usuários da carga de demonstração e como usar `app.relogio.fixo`.
- **Acesso pelo celular:** a API precisa escutar em `0.0.0.0`, e o app usa o IP da máquina na rede local como endereço base.

**Pronto quando:**
- `./gradlew test` passa por completo.
- Com o perfil `dev`, é possível percorrer todas as telas do design usando apenas o Swagger.

---

## 5. Mapa de telas para endpoints

| Tela | Endpoint(s) |
|---|---|
| A1 Entrar | `POST /auth/login` |
| A2 Criar conta | `POST /auth/cadastro` |
| A3 Aluno sem turma | `GET /me`, `GET /aluno/turmas` (lista vazia) |
| Tela inicial do aluno | `GET /aluno/turmas` |
| B1 a B7 Plano de hoje | `GET /aluno/turmas/{id}/plano` e os três `PUT` do plano |
| B8a Grupos do aluno | `GET /grupos` |
| B8 e B8b Grupo, ida e volta | `GET /grupos/{id}/hoje` |
| B9 Conta | `GET /me`, `GET /aluno/turmas`, `DELETE /me/push-token` ao sair |
| C0 Turmas do motorista | `GET /motorista/turmas` |
| C1, C2 e C5 Listas do dia | `GET /turmas/{id}/hoje` |
| C3a Grupos do motorista | `GET /grupos` |
| C3 Demais turmas do grupo | `GET /grupos/{id}/hoje` |
| C4 Alunos da turma | `GET /turmas/{id}` |
| D1 Gestão | `GET /grupos` |
| D2 Novo grupo e edição | `POST /admin/grupos`, `GET /grupos/{id}`, `PUT /admin/grupos/{id}`, `DELETE /admin/grupos/{id}` |
| D3 Todas as turmas | `GET /admin/turmas/hoje` |
| D4 Nova turma | `GET /grupos`, `GET /admin/motoristas`, `POST /admin/turmas` |
| D5 Gerenciar turma | `GET /turmas/{id}`, `PUT /admin/turmas/{id}`, `DELETE /admin/turmas/{id}`, `DELETE /admin/turmas/{id}/alunos/{alunoId}` |
| D6 Vincular alunos | `GET /admin/alunos`, `POST /admin/turmas/{id}/alunos` |
| D7 Novo motorista | `POST /admin/motoristas` |
| E1 Notificações | `PUT /me/push-token` e os envios da etapa 8 |

A aba "Conta" do motorista não tem tela desenhada. `GET /me` e `GET /motorista/turmas` cobrem o que ela precisaria exibir.

**Ajustes que as decisões da seção 1 pedem nas telas (trabalho do app, não do backend):**

- B5 e B6: retirar o botão "Abrir WhatsApp" e manter só o aviso (ponto 1).
- D2 (modo edição) e D5: incluir a ação de excluir grupo e de excluir turma, com confirmação (ponto 6). D5 também precisa permitir editar nome, número da van e capacidade.
- D4 e D5: incluir o campo "Nome da turma" (ponto 8). Nas demais telas, onde hoje aparece "Van N", passa a aparecer o nome da turma.
- D2: tratar o erro `GRUPO_COM_TURMAS` com uma mensagem orientando a excluir as turmas antes.

---

## 6. Rastreabilidade: requisitos e regras por etapa

| Item | Etapa | | Item | Etapa |
|---|---|---|---|---|
| RF01 Cadastrar turmas | 4 e 5 | | RN01 Ida e volta independentes | 6 |
| RF02 Motoristas | 4 | | RN02 Limite da ida | 6 |
| RF03 Vincular alunos | 5 | | RN03 Limite da volta | 6 |
| RF04 Status de qualquer turma | 7 | | RN04 Ausência sem declaração | 6 e 7 |
| RF05 Ver turmas e alunos | 4 e 7 | | RN05 Cancelamento da ida | 6, 7 e 8 |
| RF06 Status de presença | 7 | | RN06 Aviso de WhatsApp | app (só o aviso, ponto 1) |
| RF07 Cadastro do aluno | 2 | | RN07 Status da volta | 6 |
| RF08 Plano do dia | 6 | | RN08 Conflito no grupo | 4 e 5 |
| RF09 Status da volta | 6 | | RN09 Só o chefe gerencia | 2 a 5 |
| RF10 Cancelar ida | 6 e 8 | | RN10 Visibilidade do aluno | 7 |
| RF11 Cancelar volta | 6 | | RN11 Visibilidade do motorista | 7 |
| RF12 Status do grupo | 7 | | RN12 Visibilidade do chefe | 7 |
| RF13 Lembrete | 8 | | RN13 Sem ação manual do motorista | 6 |
| RF14 Cadastrar grupos | 3 | | RN14 Cancelamento da volta | 6 |
| RF16 Demais turmas do grupo | 7 | | RNF08 Validação no backend | todas |
| | | | RN15 Encerramento do dia | 6 |
| | | | RN16 Exclusão de grupo | 3 |
| | | | RN17 Exclusão de turma | 4 |
| | | | RN18 Remoção de aluno | 5 |

---

## 7. Fora deste plano

- Geolocalização e integração com Google Maps (nice to have do escopo). Se for retomada, entra como etapa adicional depois da 9, começando por uma prova de conceito.
- Unificação de turmas (seção 9.1 do escopo). A coluna `id_viagem_absorvida_por` existe, sem uso.
- Exclusão e edição de contas de usuário (alunos e motoristas) e recuperação de senha. A exclusão de grupos e turmas está no plano (etapas 3 e 4).
- Telefone do motorista e abertura do WhatsApp pelo app (ponto 1 da seção 1).
- Histórico e relatórios de dias anteriores. Os registros ficam no banco, mas nenhuma rota os expõe.
- Tudo o que está na seção 8 do escopo.

---

## 8. Instruções para a execução com o Claude Code

1. Executar **uma etapa por vez**, na ordem. Não iniciar a próxima enquanto os critérios de "Pronto quando" da atual não estiverem cobertos por testes que passam.
2. Seguir as convenções da seção 2.3 em todo o código, em especial o uso do `Clock` injetado e a regra de negócio no service.
3. Não alterar o esquema do banco fora do que a etapa 1 descreve. Se uma mudança for necessária, parar e avisar, porque ela altera o MER aprovado.
4. Não implementar nada da seção 7.
5. Ao fim de cada etapa, informar: o que foi criado, como testar manualmente pelo Swagger e qualquer decisão tomada que não estava no plano.
6. Em caso de conflito entre este plano e o documento de escopo, parar e perguntar em vez de escolher.
7. Ao concluir cada item, marcar a caixa correspondente em `checklist-backend.md` e, ao fechar a etapa, atualizar a tabela de progresso do mesmo arquivo. Só marcar um critério de aceite quando houver um teste automatizado que o comprove; os itens indicados como verificação manual ficam para a equipe marcar.

---

## Changelog — o que mudou da versão 2 para a versão 3

| # | Mudança | Motivo |
|---|---|---|
| 1 | Nova coluna `nome` em `TURMA` (texto livre, obrigatório, até 60 caracteres), incluída na etapa 1 e nos corpos de `POST` e `PUT /admin/turmas` | A equipe confirmou que o nome da turma é texto livre. Altera o MER. |
| 2 | Todas as respostas que trazem uma turma passam a identificá-la por `nome`; `numero_van` fica apenas como dado cadastral | Consequência da mudança 1. |
| 3 | Texto do push de cancelamento tardio montado com o nome da turma | Consequência da mudança 1. |
| 4 | Referência ao escopo atualizada para a versão 7, que incorpora as decisões da seção 1 | Plano e escopo voltam a dizer a mesma coisa. |

## Changelog — o que mudou da versão 1 para a versão 2

| # | Mudança | Motivo |
|---|---|---|
| 1 | Seção 1 passou de "pontos a validar" para "decisões confirmadas" | Os dez pontos foram respondidos pela equipe. |
| 2 | Grupos e turmas com CRUD completo: novas rotas `DELETE /admin/grupos/{id}`, `DELETE /admin/turmas/{id}` e `GET /grupos/{id}` | Decisão do ponto 6. |
| 3 | Regras de exclusão definidas (grupo só sem turmas; turma em cascata) e cascatas adicionadas às chaves estrangeiras da etapa 1 | Consequência do ponto 6. |
| 4 | Removido o bloqueio de número de van repetido no grupo e o erro `VAN_JA_CADASTRADA_NO_GRUPO` | Decisão do ponto 8. |
| 5 | Antecedência do lembrete alterada de 60 para 120 minutos | Decisão do ponto 4. |
| 6 | Validade do token alterada de 30 dias para 1 dia | Decisão do ponto 10. |
| 7 | Botão "Abrir WhatsApp" retirado das telas B5 e B6; telefone do motorista registrado como fora do plano | Decisão do ponto 1. |
| 8 | Lista de ajustes de tela adicionada à seção 5 e instrução de uso do checklist adicionada à seção 8 | Acompanhamento. |
