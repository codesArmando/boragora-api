# Checklist de Acompanhamento do Backend
## Boragora Viagens — App de Controle de Passageiros de Van

**Referência:** `plano-implementacao-backend.md` (versão 3). Cada item abaixo corresponde a algo descrito na etapa de mesmo número do plano.

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
| 1 | Banco de dados e entidades | Não iniciada | |
| 2 | Autenticação e usuários | Não iniciada | |
| 3 | Grupos (CRUD) | Não iniciada | |
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
- [ ] Migração `V1__criar_esquema.sql` com as tabelas `usuario`, `grupo`, `turma`, `turma_aluno`, `viagem` e `confirmacao`
- [ ] Coluna `turma.nome` (`VARCHAR(60)`, obrigatória, sem unicidade)
- [ ] Restrições de unicidade: `usuario.email`; `turma` (`id_grupo`, `id_motorista`); `turma_aluno` (`id_turma`, `id_aluno`); `viagem` (`id_turma`, `data`); `confirmacao` (`id_viagem`, `id_turma_aluno`)
- [ ] Chaves estrangeiras em todas as colunas `id_*`
- [ ] `ON DELETE CASCADE` em `turma_aluno.id_turma`, `viagem.id_turma`, `confirmacao.id_viagem` e `confirmacao.id_turma_aluno`
- [ ] `ON DELETE SET NULL` em `viagem.id_viagem_absorvida_por`
- [ ] Valores padrão das colunas (`admin`, `ativo`, `inclui_ida`, `inclui_volta`, `status_volta`, `cancelou_apos_saida`)
- [ ] Enums `Papel`, `Periodo` e `StatusVolta`, gravados como texto
- [ ] Entidades JPA das seis tabelas
- [ ] Um repositório Spring Data por entidade
- [ ] Hibernate configurado com `ddl-auto=validate`

**Critérios de aceite**
- [ ] A aplicação sobe com `ddl-auto=validate` sem erro
- [ ] Teste grava e lê cada entidade
- [ ] Teste confirma que cada restrição de unicidade rejeita duplicata
- [ ] Teste confirma que apagar uma turma apaga seus vínculos, viagens e confirmações
- [ ] Teste confirma que o banco rejeita apagar um grupo que tem turmas

---

## Etapa 2 — Autenticação e usuários

**Construção**
- [ ] `POST /auth/cadastro` (cria sempre aluno, sem vínculo com turma)
- [ ] `POST /auth/login`
- [ ] `GET /me`
- [ ] Senha gravada com BCrypt e e-mail normalizado em minúsculas
- [ ] Emissão e validação de JWT (HS256) com validade de 1 dia (`app.jwt.validade-horas`)
- [ ] Usuário recarregado do banco a cada requisição
- [ ] Autoridades `ROLE_ALUNO`, `ROLE_MOTORISTA` e `ROLE_ADMIN`
- [ ] Regras de rota por prefixo (`/auth`, `/me`, `/aluno`, `/motorista`, `/admin`)
- [ ] Componente `ControleDeAcesso` criado (regras de visibilidade serão usadas nas etapas 3, 4 e 7)
- [ ] Criação do motorista-chefe inicial a partir de `ADMIN_NOME`, `ADMIN_EMAIL` e `ADMIN_SENHA`

**Critérios de aceite**
- [ ] Cadastro seguido de login devolve token válido, e `GET /me` devolve o usuário
- [ ] Cadastro com e-mail repetido devolve 409 `EMAIL_JA_CADASTRADO`
- [ ] Cadastro ignora qualquer tentativa de enviar papel ou `admin` no corpo
- [ ] Login com senha errada, e-mail inexistente ou usuário inativo devolve o mesmo 401
- [ ] Rota protegida sem token devolve 401
- [ ] Token expirado devolve 401
- [ ] Aluno em rota `/admin/**` recebe 403
- [ ] O admin inicial é criado uma única vez

---

## Etapa 3 — Grupos: CRUD

**Construção**
- [ ] `POST /admin/grupos`
- [ ] `PUT /admin/grupos/{id}`
- [ ] `DELETE /admin/grupos/{id}`
- [ ] `GET /grupos` (filtrado pela visibilidade de quem chama, com `totalTurmas` e `minhaTurma`)
- [ ] `GET /grupos/{id}`
- [ ] Validação: campos obrigatórios e `horarioSaidaIda` anterior a `horarioSaidaVolta`

**Critérios de aceite**
- [ ] Admin cria, consulta, edita e exclui grupo
- [ ] Não-admin recebe 403 nas rotas `/admin/grupos`
- [ ] Horário de ida igual ou posterior ao de volta devolve 400
- [ ] Excluir grupo com turma devolve 409 `GRUPO_COM_TURMAS` *(teste completado na etapa 4, quando turmas existirem)*
- [ ] Excluir grupo vazio devolve 204 e ele some de `GET /grupos`
- [ ] `GET /grupos` devolve todos os grupos para o admin e lista vazia para aluno sem vínculo

---

## Etapa 4 — Motoristas e turmas: CRUD

**Construção**
- [ ] `POST /admin/motoristas`
- [ ] `GET /admin/motoristas`
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

---

## Etapa 5 — Vínculo e remoção de alunos

**Construção**
- [ ] `GET /admin/alunos?busca=&turmaId=` com `situacao` (`DISPONIVEL` ou `CONFLITO_NO_GRUPO`) e turmas atuais de cada aluno
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
- [ ] `GET /admin/turmas/hoje` (todas as turmas agrupadas por grupo)
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

---

## Etapa 8 — Notificações push

**Construção**
- [ ] `PUT /me/push-token`
- [ ] `DELETE /me/push-token`
- [ ] Interface `PushGateway` e implementação que chama o Expo Push Service
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

---

## Etapa 9 — Dados de demonstração e fechamento

**Construção**
- [ ] Carga de dados no perfil `dev` com o cenário das telas (grupos Manhã e Noite, motoristas, turmas, alunos e vínculos)
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

Itens que as decisões da equipe geraram para o design e para o app. Não bloqueiam o backend, mas precisam de dono.

- [ ] Telas B5 e B6: retirar o botão "Abrir WhatsApp" e manter apenas o aviso
- [ ] Tela D2 (edição): incluir a ação de excluir grupo, com confirmação e tratamento do erro `GRUPO_COM_TURMAS`
- [ ] Tela D5: incluir a ação de excluir turma, com confirmação
- [ ] Tela D5: permitir editar nome, número da van e capacidade
- [ ] Tela D4: incluir o campo "Nome da turma"
- [ ] Demais telas: exibir o nome da turma onde hoje aparece "Van N"
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
| 2026-10-06 | 0 | Perfil `dev` é o padrão e tem usuário e senha do banco com valor padrão igual ao do `docker-compose.yml`; fora do `dev` (e para JWT e admin em qualquer perfil) as variáveis de ambiente são obrigatórias | Subir o ambiente local sem configurar nada, sem colocar segredos reais no código |
| 2026-10-06 | 0 | `app.relogio.fixo` só é lido no perfil `dev`; nos demais perfis o relógio é sempre o real | Evitar que um relógio congelado vá para outro ambiente por engano |
| 2026-10-06 | 0 | Segurança provisória com todas as rotas liberadas, sem sessão e sem CSRF | As regras por prefixo e o JWT são da Etapa 2; sem isso a rota inexistente devolveria 401 em vez de 404 |
| 2026-10-06 | 0 | Códigos de erro extras: `ERRO_INTERNO` (500, sem expor detalhes) e `REQUISICAO_INVALIDA` (outros 4xx do Spring, ex.: 405 e 415). JSON mal formado e parâmetro de tipo errado devolvem 400 `VALIDACAO` | A tabela da seção 2.3 não cobre esses casos, e todos os erros precisam sair no formato padrão |
| 2026-10-06 | 0 | Testes usam Testcontainers com MySQL real (`TestcontainersConfiguration`), perfil `test` | Mesmo banco da execução real, como prevê a seção 2.1 |
