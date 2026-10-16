# Checklist de Acompanhamento do Backend
## Boragora Viagens — App de Controle de Passageiros de Van

**Referência:** `plano-implementacao-backend.md` (versão 4). Cada item abaixo corresponde a algo descrito na etapa de mesmo número do plano.

**Como usar:**
- Marcar `[x]` quando o item estiver concluído. As etapas devem ser feitas na ordem.
- Itens de **Construção** são marcados quando o código existe e compila.
- Itens de **Critérios de aceite** só são marcados quando houver um teste automatizado passando que comprove o comportamento. A exceção são os itens com a indicação *(verificação manual)*, conferidos por uma pessoa da equipe.
- Uma etapa está concluída quando todos os seus itens estão marcados. Nesse momento, atualizar a tabela de progresso.
- Qualquer decisão tomada durante a execução que não esteja no plano vai para a tabela do final.

---

## Progresso geral

| Etapa | Entrega | Status | Concluída em |
|---|---|---|---|
| 0 | Fundação | Concluída | 2026-10-07 |
| 1 | Banco de dados e entidades | Concluída | 2026-10-07 |
| 2 | Autenticação e usuários | Concluída | 2026-10-07 |
| 3 | Grupos (CRUD) | Concluída | 2026-10-08 |
| 4 | Motoristas e turmas (CRUD) | Não iniciada | |
| 5 | Vínculo e remoção de alunos | Não iniciada | |
| 6 | Plano do dia do aluno | Não iniciada | |
| 7 | Listagens do dia e visibilidade | Não iniciada | |
| 8 | Notificações push | Não iniciada | |
| 9 | Dados de demonstração e fechamento | Não iniciada | |

Valores de status: Não iniciada, Em andamento, Concluída.

---

## Etapa 0 — Fundação

**Construção**
- [x] Projeto Spring Boot com Gradle e Java 21 gerado, com as dependências da seção 2.1 do plano
- [x] `docker-compose.yml` com MySQL 8
- [x] `application.yml` com perfis `dev` e `test`, e segredos lidos de variáveis de ambiente
- [x] Bean `Clock` no fuso `America/Sao_Paulo`
- [x] Propriedade `app.relogio.fixo` funcionando no perfil `dev`
- [x] `RegraDeNegocioException` com código e status HTTP
- [x] `@RestControllerAdvice` devolvendo erros no formato padrão (`codigo`, `mensagem`, `campos`)
- [x] Swagger UI habilitado
- [x] `GET /health` público

**Critérios de aceite**
- [x] `docker compose up` e `./gradlew bootRun` sobem sem erro *(verificação manual)*
- [x] `GET /health` responde 200
- [x] Rota inexistente responde 404 no formato padrão de erro

---

## Etapa 1 — Banco de dados e entidades

**Construção**
- [x] Migração `V1__criar_esquema.sql` com as tabelas `usuario`, `grupo`, `turma`, `turma_aluno`, `viagem` e `confirmacao`
- [x] Coluna `turma.nome` (`VARCHAR(60)`, obrigatória, sem unicidade)
- [x] Restrições de unicidade: `usuario.email`; `turma` (`id_grupo`, `id_motorista`); `turma_aluno` (`id_turma`, `id_aluno`); `viagem` (`id_turma`, `data`); `confirmacao` (`id_viagem`, `id_turma_aluno`)
- [x] Chaves estrangeiras em todas as colunas `id_*`
- [x] `ON DELETE CASCADE` em `turma_aluno.id_turma`, `viagem.id_turma`, `confirmacao.id_viagem` e `confirmacao.id_turma_aluno`
- [x] `ON DELETE SET NULL` em `viagem.id_viagem_absorvida_por`
- [x] Valores padrão das colunas (`admin`, `ativo`, `inclui_ida`, `inclui_volta`, `status_volta`, `cancelou_apos_saida`)
- [x] Enums `Papel`, `Periodo` e `StatusVolta`, gravados como texto
- [x] Entidades JPA das seis tabelas
- [x] Um repositório Spring Data por entidade
- [x] Hibernate configurado com `ddl-auto=validate`

**Critérios de aceite**
- [x] A aplicação sobe com `ddl-auto=validate` sem erro
- [x] Teste grava e lê cada entidade
- [x] Teste confirma que cada restrição de unicidade rejeita duplicata
- [x] Teste confirma que apagar uma turma apaga seus vínculos, viagens e confirmações
- [x] Teste confirma que o banco rejeita apagar um grupo que tem turmas

---

## Etapa 2 — Autenticação e usuários

**Construção**
- [x] `POST /auth/cadastro` (cria sempre aluno, sem vínculo com turma)
- [x] `POST /auth/login`
- [x] `GET /me`
- [x] Senha gravada com BCrypt e e-mail normalizado em minúsculas
- [x] Emissão e validação de JWT (HS256) com validade de 1 dia (`app.jwt.validade-horas`)
- [x] Usuário recarregado do banco a cada requisição
- [x] Autoridades `ROLE_ALUNO`, `ROLE_MOTORISTA` e `ROLE_ADMIN`
- [x] Regras de rota por prefixo (`/auth`, `/me`, `/aluno`, `/motorista`, `/admin`)
- [x] Componente `ControleDeAcesso` criado (regras de visibilidade serão usadas nas etapas 3, 4 e 7)
- [x] Criação do motorista-chefe inicial a partir de `ADMIN_NOME`, `ADMIN_EMAIL` e `ADMIN_SENHA`

**Critérios de aceite**
- [x] Cadastro seguido de login devolve token válido, e `GET /me` devolve o usuário
- [x] Cadastro com e-mail repetido devolve 409 `EMAIL_JA_CADASTRADO`
- [x] Cadastro ignora qualquer tentativa de enviar papel ou `admin` no corpo
- [x] Login com senha errada, e-mail inexistente ou usuário inativo devolve o mesmo 401
- [x] Rota protegida sem token devolve 401
- [x] Token expirado devolve 401
- [x] Aluno em rota `/admin/**` recebe 403
- [x] O admin inicial é criado uma única vez

---

## Etapa 3 — Grupos: CRUD

**Construção**
- [x] `POST /admin/grupos`
- [x] `PUT /admin/grupos/{id}`
- [x] `DELETE /admin/grupos/{id}`
- [x] `GET /grupos` (filtrado pela visibilidade de quem chama, com `totalTurmas` e `minhaTurma`)
- [x] `GET /grupos/{id}`
- [x] Validação: campos obrigatórios e `horarioSaidaIda` anterior a `horarioSaidaVolta`

**Critérios de aceite**
- [x] Admin cria, consulta, edita e exclui grupo
- [x] Não-admin recebe 403 nas rotas `/admin/grupos`
- [x] Horário de ida igual ou posterior ao de volta devolve 400
- [x] Excluir grupo com turma devolve 409 `GRUPO_COM_TURMAS` *(teste completado na etapa 4, quando turmas existirem)*
- [x] Excluir grupo vazio devolve 204 e ele some de `GET /grupos`
- [x] `GET /grupos` devolve todos os grupos para o admin e lista vazia para aluno sem vínculo

---

## Etapa 4 — Motoristas e turmas: CRUD

**Construção**
- [ ] `POST /admin/motoristas`
- [ ] `GET /admin/motoristas`
- [ ] `GET /admin/turmas` (lista cadastral de todas as turmas, com `totalAlunos`)
- [ ] `POST /admin/turmas` (com `nome`)
- [ ] `PUT /admin/turmas/{id}` (nome, número da van, capacidade e motorista)
- [ ] Validação do `nome` da turma: obrigatório, sem espaços nas pontas, de 1 a 60 caracteres
- [ ] `DELETE /admin/turmas/{id}`
- [ ] `GET /turmas/{id}` (dados da turma, grupo, motorista e alunos ativos em ordem alfabética)
- [ ] `ValidadorConflitoDeGrupo` com a regra da RN08 para motoristas
- [ ] Validação de que `motoristaId` é um usuário ativo com papel `MOTORISTA`

**Critérios de aceite**
- [ ] Admin cadastra motorista, cria turma, altera dados cadastrais e troca o motorista
- [ ] Mesmo motorista em duas turmas do mesmo grupo devolve 409 `CONFLITO_GRUPO_MOTORISTA` na criação
- [ ] O mesmo conflito devolve 409 na troca de motorista
- [ ] Mesmo motorista em turmas de grupos diferentes é aceito
- [ ] Turma sem nome, com nome em branco ou com mais de 60 caracteres devolve 400
- [ ] Duas turmas com o mesmo `nome` ou o mesmo `numeroVan` no mesmo grupo são aceitas
- [ ] Alterar o nome pela edição reflete em `GET /turmas/{id}`
- [ ] Excluir turma devolve 204 e remove vínculos, viagens e confirmações dela
- [ ] Depois da exclusão, o motorista e os alunos podem entrar em outra turma do mesmo grupo
- [ ] `GET /turmas/{id}` respeita a visibilidade (403 para quem não pode ver)
- [ ] `GET /admin/turmas` devolve todas as turmas para o admin, com `totalAlunos` contando só vínculos ativos, e 403 para não-admin

---

## Etapa 5 — Vínculo e remoção de alunos

**Construção**
- [ ] `GET /admin/alunos?busca=&turmaId=` com `situacao` (`DISPONIVEL` ou `CONFLITO_NO_GRUPO`) e turmas atuais de cada aluno
- [ ] `turmaId` opcional em `GET /admin/alunos`: sem ele, listagem geral de alunos ativos (tela D10), sem `situacao`
- [ ] `POST /admin/turmas/{id}/alunos` (lote, tudo ou nada)
- [ ] `DELETE /admin/turmas/{id}/alunos/{alunoId}` (desativa o vínculo)
- [ ] RN08 para alunos no mesmo `ValidadorConflitoDeGrupo`
- [ ] Validação de que só usuários ativos com papel `ALUNO` são vinculados
- [ ] Reativação do vínculo inativo em vez de criar outro registro

**Critérios de aceite**
- [ ] Vincular dois alunos de uma vez funciona, e eles aparecem em `GET /turmas/{id}`
- [ ] Aluno já vinculado a outra turma do mesmo grupo devolve 409 `CONFLITO_GRUPO_ALUNO`, e ninguém do lote é vinculado
- [ ] Aluno pode estar em turmas de grupos diferentes
- [ ] Tentar vincular um motorista como aluno é rejeitado
- [ ] Remover e vincular de novo reutiliza o mesmo registro
- [ ] Aluno removido deixa de aparecer em `GET /turmas/{id}`
- [ ] A busca filtra por nome e por e-mail, e não lista quem já está na turma
- [ ] Sem `turmaId`, a lista traz alunos com e sem vínculo, com `turmas` vazia para quem não tem vínculo ativo e `situacao` nula

---

## Etapa 6 — Plano do dia do aluno

**Construção**
- [ ] `GET /aluno/turmas` (turmas do aluno com resumo do plano de hoje)
- [ ] `GET /aluno/turmas/{turmaId}/plano` (com o bloco `acoes`)
- [ ] `PUT /aluno/turmas/{turmaId}/plano/ida`
- [ ] `PUT /aluno/turmas/{turmaId}/plano/volta`
- [ ] `PUT /aluno/turmas/{turmaId}/plano/status-volta`
- [ ] `PlanoDoDiaService` usando o `Clock` injetado
- [ ] Criação da `VIAGEM` e da `CONFIRMACAO` sob demanda, na primeira marcação
- [ ] Tratamento da criação simultânea da viagem (restrição única)
- [ ] Publicação do evento `IdaCanceladaAposSaida`
- [ ] `atualizado_em` gravado a cada alteração

**Critérios de aceite** (grupo com ida 18:10 e volta 22:30, relógio fixo)
- [ ] 17:00, incluir ida e volta: as duas marcadas, status `EM_AULA`, viagem criada
- [ ] 17:30, cancelar ida: ida desmarcada, `cancelouAposSaida = false`, nenhum evento
- [ ] 18:09, incluir ida: aceito
- [ ] 18:10, incluir ida: 422 `IDA_ENCERRADA`
- [ ] 18:30, cancelar ida marcada: `cancelouAposSaida = true`, um evento publicado
- [ ] 18:31, incluir ida de novo: 422 `IDA_ENCERRADA`
- [ ] 18:30, cancelar ida não marcada: 200 sem alteração, nenhum evento
- [ ] 19:00, incluir volta: aceito
- [ ] 21:00, status `LIBERADO` com volta marcada: aceito
- [ ] 21:00, status `LIBERADO` sem volta marcada: 422 `VOLTA_NAO_INCLUIDA`
- [ ] 22:29, cancelar volta: aceito
- [ ] 22:30, qualquer alteração (inclusive cancelar a ida): 422 `DIA_ENCERRADO`
- [ ] Aluno sem vínculo ativo com a turma: 403
- [ ] `GET` do plano não cria viagem nem confirmação
- [ ] Marcar o que já está marcado devolve 200 sem alteração
- [ ] O bloco `acoes` está correto em cada um dos estados das telas B1 a B7
- [ ] `GET /aluno/turmas` devolve lista vazia para aluno sem vínculo

---

## Etapa 7 — Listagens do dia e visibilidade

**Construção**
- [ ] `GET /motorista/turmas` (turmas do motorista com contadores)
- [ ] `GET /turmas/{id}/hoje` (contadores, `listaIda`, `listaVolta`, `semConfirmacoes`)
- [ ] `GET /grupos/{id}/hoje` (todas as turmas do grupo, com `minhaTurma` e `voce`)
- [ ] Regras de visibilidade RN10, RN11 e RN12 no `ControleDeAcesso`
- [ ] Consulta única das confirmações do dia, sem consultas em cascata

**Critérios de aceite**
- [ ] Lista da ida traz quem tem ida marcada e quem cancelou depois da saída, este com `CANCELADO_APOS_SAIDA`
- [ ] `vaoNaIda` não conta o cancelado tardio; `canceladosAposSaida` conta
- [ ] Lista da volta traz só quem tem volta marcada, com liberados primeiro
- [ ] `liberados` conta apenas volta marcada com status `LIBERADO`
- [ ] Quem não declarou o plano não aparece em nenhuma lista
- [ ] Confirmação de vínculo inativo não aparece
- [ ] Turma sem confirmações devolve `semConfirmacoes = true` e listas vazias
- [ ] Nenhuma consulta cria viagem
- [ ] Aluno consulta o próprio grupo e recebe 403 em grupo sem vínculo
- [ ] Motorista consulta turmas do grupo em que atua e recebe 403 fora dele
- [ ] Admin consulta qualquer turma e qualquer grupo
- [ ] O selo `voce` aparece só no item do aluno autenticado
- [ ] Motorista-chefe que não conduz turma recebe lista vazia em `GET /motorista/turmas`

---

## Etapa 8 — Notificações push

**Construção**
- [ ] `PUT /me/push-token`
- [ ] `DELETE /me/push-token`
- [ ] Interface `PushGateway` e implementação que chama o Expo Push Service
- [ ] Campo `data` (`tipo` e `turmaId`) no push de cancelamento tardio e no lembrete
- [ ] Implementação falsa do gateway para os testes
- [ ] Token apagado quando o Expo responde `DeviceNotRegistered`
- [ ] Listener do evento `IdaCanceladaAposSaida`, executado depois do commit
- [ ] Texto do push de cancelamento tardio conforme a tela E1, montado com o nome da turma
- [ ] Tarefa agendada do lembrete, a cada minuto
- [ ] Propriedades `app.lembrete.antecedencia-minutos` (120) e `app.lembrete.dias` (segunda a sexta)
- [ ] Texto do lembrete conforme a tela E1

**Critérios de aceite**
- [ ] Cancelar a ida depois da saída gera exatamente um envio, para o motorista responsável da turma
- [ ] O motorista-chefe não recebe o push quando não é o responsável pela turma
- [ ] Cancelar a ida antes da saída não gera envio
- [ ] Se o gateway lançar erro, o cancelamento continua gravado
- [ ] Com relógio em 16:10 e ida às 18:10, o lembrete vai só para quem não marcou nada
- [ ] Quem já marcou ida ou volta não recebe o lembrete
- [ ] Um minuto antes ou depois do horário de disparo, nada é enviado
- [ ] Fora dos dias configurados, nada é enviado
- [ ] Usuário sem token é ignorado sem erro
- [ ] O gateway falso registra o `data` correto nos dois tipos de push

---

## Etapa 9 — Dados de demonstração e fechamento

**Construção**
- [ ] Carga de dados no perfil `dev` com o cenário das telas (grupos Manhã e Noite, motoristas, turmas, alunos e vínculos, e alunos sem vínculo para as telas D6 e D10)
- [ ] Teste de ponta a ponta com MockMvc e Testcontainers (do cadastro do grupo até a lista com cancelamento tardio)
- [ ] Revisão da RNF08: cada regra das etapas 4 a 6 tem teste que chama a API diretamente
- [ ] README com instruções de execução, variáveis de ambiente, Swagger, usuários de demonstração e `app.relogio.fixo`
- [ ] API escutando em `0.0.0.0` para acesso pelo celular na rede local

**Critérios de aceite**
- [ ] `./gradlew test` passa por completo
- [ ] Com o perfil `dev`, todas as telas do design podem ser percorridas usando apenas o Swagger *(verificação manual)*
- [ ] O app, rodando no celular, consegue fazer login na API da máquina local *(verificação manual)*

---

## Ajustes fora do backend

Itens que as decisões da equipe geraram para o design e para o app. Não bloqueiam o backend, mas precisam de dono. Os itens de tela abaixo estão marcados porque já foram aplicados no protótipo (2026-10-13); a implementação no app é acompanhada em `checklist-frontend.md`.

- [x] Telas B5 e B6: retirar o botão "Abrir WhatsApp" e manter apenas o aviso
- [x] Tela D2 (edição): incluir a ação de excluir grupo, com confirmação e tratamento do erro `GRUPO_COM_TURMAS` *(no protótipo, tela D2b)*
- [x] Tela D5: incluir a ação de excluir turma, com confirmação *(no protótipo, tela D5a)*
- [x] Tela D5: permitir editar nome, número da van e capacidade *(no protótipo, tela D5a)*
- [x] Tela D4: incluir o campo "Nome da turma"
- [x] Demais telas: exibir o nome da turma onde hoje aparece "Van N"
- [x] Protótipo: abas do chefe iguais às do motorista mais Gestão; D1 com quatro atalhos; D3 como lista cadastral; telas novas C0b, C6, D8, D9 e D10
- [x] Plano de implementação do frontend e `checklist-frontend.md` criados
- [x] MER: coluna `nome` adicionada em `TURMA`
- [x] Documento de escopo: pontos em aberto já decididos fechados na versão 7 (destinatário do push, confirmação do cancelamento tardio, antecedência do lembrete)
- [x] Documento de escopo: exclusão de grupos e turmas, nome da turma em texto livre, bloqueio após a saída da volta e remoção do botão do WhatsApp registrados na versão 7
- [ ] Confirmar com o professor se o Expo Push Service atende ao RNF04

---

## Pendências de conferência da equipe

- [ ] Regras de exclusão: grupo só sem turmas; turma apaga vínculos, viagens e confirmações (plano, seção 1)
- [ ] Dias de envio do lembrete: segunda a sexta (plano, seção 1, ponto 4)

---

## Decisões tomadas durante a execução

Registrar aqui tudo o que for decidido na implementação e não estiver no plano.

| Data | Etapa | Decisão | Motivo |
|---|---|---|---|
| 2026-10-06 | 0 | MySQL do Docker Compose exposto na porta 3307 do host (configurável por `DB_PORTA`); imagem `mysql:8` (8.4) | Evitar conflito com um MySQL local que já esteja usando a 3306 |
| 2026-10-06 | 0 | Perfil `dev` é o padrão e tem usuário e senha do banco com valor padrão igual ao do `docker-compose.yml`; fora do `dev` as variáveis de ambiente são obrigatórias | Subir o ambiente local sem configurar nada, sem colocar segredos reais no código. Na Etapa 2 o mesmo passou a valer para JWT e admin (ver abaixo) |
| 2026-10-06 | 0 | `app.relogio.fixo` só é lido no perfil `dev`; nos demais perfis o relógio é sempre o real | Evitar que um relógio congelado vá para outro ambiente por engano |
| 2026-10-06 | 0 | Segurança provisória com todas as rotas liberadas, sem sessão e sem CSRF | As regras por prefixo e o JWT são da Etapa 2; sem isso a rota inexistente devolveria 401 em vez de 404 |
| 2026-10-06 | 0 | Códigos de erro extras: `ERRO_INTERNO` (500, sem expor detalhes) e `REQUISICAO_INVALIDA` (outros 4xx do Spring, ex.: 405 e 415). JSON mal formado e parâmetro de tipo errado devolvem 400 `VALIDACAO` | A tabela da seção 2.3 não cobre esses casos, e todos os erros precisam sair no formato padrão |
| 2026-10-06 | 0 | Testes usam Testcontainers com MySQL real (`TestcontainersConfiguration`), perfil `test` | Mesmo banco da execução real, como prevê a seção 2.1 |
| 2026-10-07 | 1 | Tamanhos das colunas de texto: `nome_pessoa` e `grupo.nome` 100, `email` 255, `senha_hash` 100, `expo_push_token` 255, `role` 20, `periodo` e `status_volta` 10 | O MER e o plano só definem o tamanho de `turma.nome` (60) |
| 2026-10-07 | 1 | Colunas anuláveis: `expo_push_token`, `id_viagem_absorvida_por`, `turma.numero_van` e `turma.capacidade`; as demais são `NOT NULL` | Decisão da equipe: número da van e capacidade são dados cadastrais opcionais (o escopo diz que a van "pode ter" esses dados). Na Etapa 4, `numeroVan` e `capacidade` serão opcionais em `POST` e `PUT /admin/turmas` |
| 2026-10-07 | 1 | `atualizado_em` sem valor padrão no banco; quem preenche é o service, com o `Clock` injetado | Seguir a convenção 2 da seção 2.3 (nada de `now()` fora do relógio) |
| 2026-10-07 | 1 | Coluna `role` mapeada no campo `papel` (enum `Papel`) da entidade `Usuario` | Nome da coluna vem do MER; nome do enum vem do plano |
| 2026-10-07 | 1 | Restrições com nome explícito (`uk_*`, `fk_*`), tabelas InnoDB em `utf8mb4` | Mensagens de erro identificáveis nos testes e suporte a acentos |
| 2026-10-07 | 2 | No perfil `dev`, `JWT_SEGREDO` e `ADMIN_*` têm valores padrão (admin `admin@boragora.local` / `admin123`). Fora do `dev`, `JWT_SEGREDO` é obrigatório (mínimo 32 bytes) e `ADMIN_*` só é exigido enquanto não existir nenhum admin | Rodar `bootRun` sem configurar variáveis; a aplicação não sobe sem um jeito de existir o primeiro admin |
| 2026-10-07 | 2 | Rotas fora dos prefixos públicos exigem token; rota inexistente sem token devolve 401 e com token devolve 404 | Proteger por padrão qualquer rota nova; o critério de 404 da Etapa 0 passou a ser testado com token |
| 2026-10-07 | 2 | `POST /auth/cadastro` devolve 201; `POST /auth/login` devolve 200 | Cadastro cria um recurso |
| 2026-10-07 | 2 | E-mail normalizado com `trim` e minúsculas já na leitura do corpo, antes da validação de formato | Espaço digitado nas pontas (comum no teclado do celular) não deve invalidar o e-mail |
| 2026-10-07 | 2 | Senha de 6 a 72 caracteres | 72 bytes é o limite do BCrypt |
| 2026-10-07 | 2 | Login com e-mail inexistente compara a senha com um hash fictício | Mesmo tempo de resposta nos três casos de 401, para não revelar quais e-mails existem |
| 2026-10-07 | 2 | Token com `sub` = id do usuário e `iss` = `boragora-api`; validade conferida com o mesmo `Clock` da aplicação | Com `app.relogio.fixo` no `dev`, o token emitido no horário congelado continua válido |
| 2026-10-07 | 2 | Token de usuário apagado ou inativo devolve 401 `NAO_AUTENTICADO` | Usuário é recarregado a cada requisição; inativo perde o acesso na hora |
| 2026-10-07 | 2 | `ROLE_ADMIN` só é dado quando o papel é `MOTORISTA` e `admin = true` | O plano define o admin como motorista com `admin = true` |
| 2026-10-07 | 2 | `ControleDeAcesso` já tem as regras RN10, RN11 e RN12 (`podeVerGrupo`, `podeVerTurma`, `exigirAcesso...`) com testes; nega com 403 `ACESSO_NEGADO`. O item da Etapa 7 fica para ser marcado quando as listagens usarem o componente | As etapas 3 e 4 já precisam da visibilidade em `GET /grupos` e `GET /turmas/{id}` |
| 2026-10-07 | 2 | Swagger com botão "Authorize" (token Bearer) | Testar as rotas protegidas pelo Swagger |
| 2026-10-08 | 3 | O teste de 409 `GRUPO_COM_TURMAS` já foi feito nesta etapa, criando a turma direto pelo repositório | Não precisa esperar as rotas de turma da Etapa 4 |
| 2026-10-08 | 3 | `POST /admin/grupos` devolve 201; `PUT` e `GET /grupos/{id}` devolvem o mesmo formato de item de `GET /grupos` (com `totalTurmas` e `minhaTurma`) | Um único formato de grupo para o app |
| 2026-10-08 | 3 | `minhaTurma` vem para qualquer usuário que tenha turma no grupo (inclusive o motorista-chefe, se conduzir uma); senão vem `null` | O chefe também é motorista e pode conduzir turma |
| 2026-10-08 | 3 | `GET /grupos` ordenado por período (manhã, tarde, noite), horário de ida e nome | Ordem previsível nas telas |
| 2026-10-08 | 3 | Ida igual ou posterior à volta devolve 400 `VALIDACAO` com `campos.horarioSaidaVolta`; a checagem fica no service | Regra de negócio no service (RNF08), mantendo o formato de erro de validação |
| 2026-10-08 | 3 | Horários aceitos e devolvidos só como `HH:mm` (ex.: `18:10`; `18:10:00` é rejeitado) | Convenção 6 da seção 2.3 |
| 2026-10-08 | 3 | Em `GET /grupos/{id}`, a existência é conferida antes da visibilidade (inexistente → 404, existente sem acesso → 403) | Mesma ordem prevista no `ControleDeAcesso` |
| 2026-10-13 | 4, 5, 7, 8 e 9 | Plano revisado para a versão 4: `GET /admin/turmas` na etapa 4; `turmaId` opcional em `GET /admin/alunos` na etapa 5; `GET /admin/turmas/hoje` removido da etapa 7; campo `data` no push na etapa 8; alunos sem vínculo na carga da etapa 9. Itens correspondentes acrescentados acima, e o item de `GET /admin/turmas/hoje` retirado | Revisão das telas feita junto com o plano do frontend (pontos 11 a 14 do plano). As etapas concluídas (0 a 3) não mudam |
