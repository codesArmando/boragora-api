# Documento de Escopo do Projeto
## App de Controle de Passageiros de Van

**Disciplina:** Projeto Acadêmico — 5º Semestre

**Cliente real:** Motorista de van (transporte de estudantes)

**Equipe:** Armando Fernandes, Eduardo Soares, Guilherme Lins e Pedro Tozaki.

**Entrega final:** mês de novembro

**Versão:** 7 — revisada após a validação do plano de implementação do backend (ver changelog no final)

**Documentos relacionados:** plano de implementação do backend (`plano-implementacao-backend.md`), modelo de dados (`mer_ajustado_app_van_dark.html`) e projeto de telas "Boragora Viagens".

---

## 1. Contexto e Justificativa

Atualmente, o controle de passageiros da van é feito manualmente via WhatsApp. Os alunos enviam mensagens de texto livre ("Eu vou", "Só volto", "Não volto") e o motorista precisa ler essas mensagens e montar o itinerário do dia manualmente.

Esse processo manual gera falhas recorrentes:

- Alunos avisam em cima da hora que não vão mais, sem tempo hábil de reação do motorista;
- O motorista pode esquecer de incluir um aluno na rota, ou incluir por engano alguém que cancelou;
- Alunos esquecem de confirmar presença (ida ou volta), pois não existe nenhum lembrete automático.

Se o projeto não for adiante, o WhatsApp continuará sendo o único canal de controle, mantendo o risco de erro humano e desencontros entre motorista e passageiros.

## 2. Objetivo do Projeto

Desenvolver um aplicativo mobile (React Native + Expo) que substitua o controle manual via WhatsApp por um sistema estruturado de confirmação de presença, cadastro de turmas e com possibilidade de expansão com acompanhamento de localização do motorista em tempo real, reduzindo erros de comunicação entre motorista e passageiros.

## 3. Papéis e Perfis de Usuário

| Papel | Descrição |
|---|---|
| **Motorista-chefe (admin)** | Dono do sistema. É um motorista como qualquer outro, podendo inclusive conduzir uma turma, mas com permissões adicionais: cadastra, edita e exclui grupos e turmas, cadastra motoristas, vincula e remove alunos, e visualiza todas as turmas. |
| **Motorista responsável** | Cadastrado pelo motorista-chefe para conduzir uma turma específica. Acompanha sua(s) turma(s) e as demais turmas do mesmo grupo. |
| **Aluno (passageiro)** | Realiza seu próprio cadastro (credenciais). Declara o plano do dia, cancela a ida ou a volta, atualiza o status da volta e acompanha o status do seu grupo. |

**Regra de identidade:** cada pessoa possui exatamente um acesso ao sistema, e seu papel é fixo — não varia conforme a turma. O papel é atributo do usuário, não do vínculo com a turma. O motorista-chefe não é um papel separado: é um usuário com papel de motorista que possui a permissão adicional de administrador. O motorista responsável apenas visualiza; o motorista-chefe visualiza e gerencia.

## 4. Conceitos centrais: "Grupo" e "Turma"

O sistema organiza o transporte em duas camadas.

**Grupo** — corresponde a um turno de serviço das vans (ex: noite). Guarda as informações comuns ao turno: período, horário de saída da ida e horário de saída da volta. Os horários de saída pertencem ao grupo, e não à turma: todas as vans do turno saem no mesmo horário. Um grupo pode conter várias turmas, pois um mesmo turno pode ser atendido por mais de uma van.

Os horários de saída são também os **marcos temporais** do sistema: o horário de saída da ida é o limite para incluir a ida e define o que é um cancelamento tardio; o horário de saída da volta é o limite para incluir ou cancelar a volta e alterar seu status, e encerra o plano do dia: depois dele, nenhuma alteração é aceita (ver RN15). Nenhum desses marcos depende de ação manual do motorista (ver RN13).

**Turma** — vincula uma van + um motorista responsável + um conjunto de alunos, dentro de um grupo. É a unidade operacional: o controle de presença acontece no contexto da turma. Cada turma tem um **nome em texto livre**, definido pelo motorista-chefe (ex.: "Van 2" ou "Van da Zona Sul"), que a identifica nas telas e nas notificações. Nomes de turma podem se repetir.

**Van** — pode ter o número e a capacidade registrados como dados cadastrais, mas o sistema não controla lotação nem bloqueia confirmações por falta de lugar.

**Viagem do dia** — registro diário de uma turma, criado sob demanda na primeira confirmação de plano do dia de um aluno daquela turma. Se nenhum aluno confirmar, nenhuma viagem é criada.

**Por que a camada de grupo existe:** na prática, alunos e motoristas de um mesmo turno precisam enxergar o status das demais turmas do turno, porque em alguns dias duas turmas são unidas em uma única van para o retorno da faculdade. Um modelo baseado apenas em turmas isoladas limitaria essa visualização. Este caso não havia sido levantado no escopo original e foi identificado durante a modelagem.

## 5. Requisitos Funcionais (RF)

### 5.1 Motorista-chefe (admin)
- RF01 — Cadastrar, consultar, editar e excluir turmas (nome, van, grupo, motorista responsável, lista de alunos). A exclusão segue a RN17.
- RF02 — Cadastrar motoristas e definir ou trocar o motorista responsável de uma turma.
- RF03 — Cadastrar/vincular alunos a turmas. O vínculo é feito exclusivamente pelo admin, a partir de usuários já cadastrados. O admin também remove alunos da turma (ver RN18).
- RF04 — Acompanhar o status de qualquer turma (quantos confirmaram ida, quantos confirmaram volta).
- RF14 — Cadastrar, consultar, editar e excluir grupos (turno de serviço), definindo nome, período e horários de saída da ida e da volta. A exclusão segue a RN16.

### 5.2 Motorista responsável
- RF05 — Visualizar sua(s) turma(s) e os alunos vinculados.
- RF06 — Acompanhar o status de presença da turma (quem vai, quem volta, quem está liberado). Alunos que não declararam o plano do dia não aparecem na listagem. Alunos que cancelaram a ida após o horário de saída permanecem na listagem da ida, destacados com o status "cancelado" (ver RN05).
- RF16 — Visualizar o status das demais turmas do mesmo grupo.

### 5.3 Aluno (passageiro)
- RF07 — Realizar o próprio cadastro de acesso (login por e-mail e senha). O cadastro cria apenas a conta do usuário; **não** vincula o aluno a nenhuma turma — esse vínculo é feito pelo admin (RF03).
- RF08 — Declarar o plano do dia marcando as opções **Ida** e/ou **Volta**. A ida só pode ser incluída até o horário de saída da ida do grupo, e a volta até o horário de saída da volta (ver RN02 e RN03). O aluno que não declara o plano do dia é considerado ausente (ver RN04).
- RF09 — Atualizar o status da volta ao longo do dia. Valores possíveis: "em aula" (padrão) e "liberado". Disponível sempre que a volta estiver no plano do dia, até o horário de saída da volta do grupo, sem depender de nenhuma ação do motorista. O aluno que não vai voltar não usa o status: ele cancela a volta (ver RF11).
- RF10 — Cancelar a ida até o horário de saída da volta do grupo. Se o cancelamento ocorrer após o horário de saída da ida, é tratado como cancelamento tardio: o app pede uma confirmação antes de concluir, o motorista responsável pela turma recebe uma notificação push e o aluno vê um aviso para comunicar o motorista também pelo WhatsApp (ver RN05, RN06 e RN15).
- RF11 — Cancelar a volta desmarcando a opção **Volta** do plano do dia (campo `inclui_volta`), até o horário de saída da volta do grupo. O aluno deixa de aparecer na listagem da volta e pode incluí-la novamente enquanto esse horário não tiver passado (ver RN03 e RN14).
- RF12 — Acompanhar o status da própria turma e das demais turmas do mesmo grupo (quem vai, quem volta, quem está liberado).
- RF13 — Receber notificação de lembrete para declarar o plano do dia. O envio é controlado pelos campos `inclui_ida` e `inclui_volta`: o lembrete é direcionado aos alunos que ainda não incluíram nenhuma das opções. É enviado 2 horas antes do horário de saída da ida do grupo.

## 6. Regras de Negócio (RN)

### 6.1 Plano do dia
- RN01 — O aluno declara o plano do dia marcando as opções Ida e/ou Volta, de forma independente.
- RN02 — A ida só pode ser incluída no plano até o horário de saída da ida do grupo. Após esse horário, a tentativa de incluir a ida exibe uma mensagem informando que a inclusão não é mais possível e orientando o aluno a falar com o motorista pelo WhatsApp. A regra vale também para o aluno que cancelou a ida após a saída e tenta incluí-la novamente.
- RN03 — A volta pode ser incluída no plano até o horário de saída da volta do grupo, inclusive após o horário de saída da ida. Após o horário de saída da volta, a inclusão é bloqueada.
- RN04 — O aluno que não declara o plano do dia é considerado ausente (não vai e não volta) e não é exibido na listagem do dia. Não existe confirmação explícita de ausência nem estado "pendente".

### 6.2 Cancelamentos e status da volta
- RN05 — O cancelamento da ida está disponível até o horário de saída da volta do grupo (ver RN15). Antes do horário de saída da ida, o aluno é removido da listagem da ida. Após esse horário, o cancelamento é tardio: é registrado no campo `cancelou_apos_saida`, o aluno permanece na listagem da ida destacado com o status "cancelado", e apenas o motorista responsável pela turma do aluno recebe uma notificação push (ex.: "João Gabriel cancelou a ida para a faculdade!").
- RN06 — Antes de concluir um cancelamento tardio, o app exibe uma confirmação, pois a ação não pode ser desfeita (RN02). Essa confirmação é um passo da interface, não uma validação do backend. Depois do cancelamento, o app exibe um aviso em texto para que o aluno comunique o motorista também pelo WhatsApp; nesta entrega não há botão que abra o WhatsApp. Não há pergunta sobre a volta: se o aluno também não voltará, ele cancela a volta separadamente, desmarcando-a do plano do dia (ver RN14).
- RN07 — O status da volta ("em aula" ou "liberado") está disponível sempre que a volta estiver no plano do dia, com "em aula" como valor padrão, e pode ser alterado até o horário de saída da volta do grupo. Após esse horário, a edição é bloqueada.
- RN14 — O cancelamento da volta é feito desmarcando a opção Volta do plano do dia (`inclui_volta` passa a falso) e está disponível até o horário de saída da volta do grupo; após esse horário, a volta não pode mais ser desmarcada. O aluno é removido da listagem da volta. Não existe status "não volto" nem cancelamento tardio da volta. Se o aluno ficar sem nenhuma opção marcada, passa a ser tratado como ausente (RN04).
- RN15 — O horário de saída da volta do grupo encerra o plano do dia. A partir dele, nenhuma alteração é aceita: incluir ou cancelar a volta, alterar o status da volta e cancelar a ida.

### 6.3 Vínculos
- RN08 — Motoristas (incluindo o motorista-chefe) e alunos não podem estar vinculados a duas turmas do mesmo grupo. A validação ocorre no vínculo do aluno a uma turma e na definição do motorista responsável.
- RN09 — Apenas o motorista-chefe cria, edita e exclui grupos e turmas, cadastra motoristas, define o motorista responsável e vincula ou remove alunos.
- RN18 — A remoção de um aluno da turma desativa o vínculo, sem apagá-lo. Se o aluno for vinculado novamente à mesma turma, o vínculo é reativado.

### 6.4 Visibilidade
- RN10 — O aluno visualiza o status da própria turma e das demais turmas do seu grupo.
- RN11 — O motorista responsável visualiza suas turmas e as demais turmas dos grupos em que atua.
- RN12 — O motorista-chefe visualiza todas as turmas.

### 6.5 Princípio de funcionamento
- RN13 — Nenhuma ação manual do motorista é pré-condição para ações do aluno. Os marcos do dia (limites de inclusão, cancelamento tardio, bloqueio da volta e encerramento do dia) são derivados dos horários de saída cadastrados no grupo.

### 6.6 Exclusão de cadastros
- RN16 — Um grupo só pode ser excluído se não tiver turmas.
- RN17 — A exclusão de uma turma remove também os vínculos de alunos, as viagens e as confirmações dela. As contas do motorista e dos alunos não são afetadas. O app pede confirmação antes de excluir.

## 7. Requisitos Não Funcionais (RNF)

- RNF01 — Aplicativo mobile, desenvolvido em **React Native** com **Expo**, com telas e rotas criadas pelo **expo-router**.
- RNF02 — Backend construído em **Java com Spring Boot**, com **Gradle** como gerenciador de dependências e banco de dados **MySQL**. O app se comunica com a API por uma camada de repositório, e apenas a API acessa o banco.
- RNF03 — Autenticação simples via e-mail e senha (sem verificação adicional, dado o contexto acadêmico). A sessão vale por 1 dia; depois disso, o usuário faz login novamente.
- RNF04 — Integração obrigatória com **API externa**. Nesta entrega, a integração é com o serviço de notificações push do Expo. A integração com Google Maps para localização em tempo real permanece como nice to have (seção 10).
- RNF05 — Uso de recurso nativo do dispositivo (notificações de push) que justifique a natureza mobile do app.
- RNF06 — O app depende de conexão com a internet para funcionar — não há suporte a modo offline.
- RNF08 — Todas as regras de negócio são validadas no backend. Restrições aplicadas apenas na interface do app (ocultar ou desabilitar botões) não são consideradas suficientes.

## 8. Fora de Escopo (explicitamente excluído desta entrega)

- Controle ou processamento de pagamentos.
- Chat ou canal de comunicação dentro do app (permanece no WhatsApp; nesta entrega o app apenas exibe avisos orientando o contato, sem botão de redirecionamento).
- Avaliação/rating de motoristas ou alunos.
- Notificação por proximidade / geofencing ("avisar quando o motorista está perto").
- Versão web ou painel administrativo web.
- Histórico de meses anteriores / relatórios históricos.
- Cadastro de rotas alternativas ou rastreamento de trajeto (traçado de rota estilo Uber).
- Fluxo de solicitação/aprovação de entrada em turma. O aluno cria a própria conta (RF07), mas não solicita nem escolhe turma; o vínculo é feito unicamente pelo admin.
- Registro manual de início e fim de rota pelo motorista (substituído pelos horários do grupo).
- Controle de lotação da van (a capacidade pode ser cadastrada, mas sem uso no sistema).
- Horários de saída diferentes por turma dentro do mesmo grupo.
- Conformidade formal com LGPD (tratada apenas informalmente, sem armazenamento persistente de localização).
- Funcionamento offline.
- Cadastro do telefone do motorista e abertura do WhatsApp a partir do app.
- Edição e exclusão de contas de usuário (alunos e motoristas) e recuperação de senha.

## 9. Casos identificados e despriorizados

Casos reais de uso que foram levantados durante a análise, têm suporte previsto no modelo de dados, mas **não serão implementados nesta entrega**.

### 9.1 Unificação de turmas em uma única van

Em alguns dias, duas turmas do mesmo grupo são unidas em uma única van para o retorno. O modelo de dados já prevê o registro dessa unificação (a viagem de uma turma pode apontar para a viagem que a absorveu), mas a funcionalidade não será construída nesta entrega.

**Frequência levantada:** pelo menos duas vezes por mês.

**Decisão:** não será implementado, por não ser um controle essencial. A visualização compartilhada entre turmas do mesmo grupo (RF16) já resolve a necessidade prática de coordenação, sem exigir registro formal da unificação.

**Dependências caso seja retomado:** definir se a unificação é por turma inteira ou por aluno; definir quem autoriza (motorista ou admin); definir o comportamento quando a capacidade da van é excedida.

## 10. Nice to haves

- Acompanhamento do motorista por geolocalização/compartilhamento de localização em tempo real;
- Notificações de aviso de proximidade do motorista/chegada em determinado local;
- Acompanhamento de rotas por parte dos pais (em caso de passageiro menor de idade);
- Botão para abrir a conversa do WhatsApp com o motorista a partir do app (exige o cadastro do telefone).

## 11. Riscos Identificados

| Risco | Impacto | Observação |
|---|---|---|
| Complexidade da integração de geolocalização em tempo real | Alto | Aplica-se apenas se o nice to have de geolocalização for retomado. Time ainda não tem clareza da dificuldade de implementação; recomenda-se prova de conceito (spike) cedo no cronograma, em paralelo ao design. |
| Prazo apertado (4 pessoas até início de novembro) | Alto | Recomenda-se dividir em sprints/marcos logo após o fechamento deste escopo. |
| Dependência de API externa | Médio | O RNF04 passa a ser atendido pelo serviço de push do Expo; é preciso confirmar com o professor se isso é suficiente (ver seção 12). Se a geolocalização for retomada, definir e validar a API de mapas antes de iniciar as telas de localização. |
| Regra de conflito de período entre turmas (RN08) | Médio | Precisa de validação clara no backend, aplicada nos dois pontos de entrada (vínculo de aluno e definição de motorista). Recomenda-se extrair para um único validador. |
| Aluno que esquece de declarar o plano do dia | Médio | Como a ausência de resposta é tratada como ausência (RN04) e a inclusão da ida é bloqueada após o horário de saída (RN02), o aluno que esquece fica sem ida pelo app naquele dia e precisa recorrer ao WhatsApp. O lembrete (RF13) passa a ser a principal proteção contra esse caso e é enviado 2 horas antes do horário de saída da ida. |
| Cancelamento tardio feito por engano | Baixo | O cancelamento após a saída não pode ser desfeito pelo app (RN02). O risco é reduzido pela confirmação exibida antes de concluir a ação (RN06). |
| Volta esquecida após cancelar a ida | Baixo | Sem a pergunta sobre a volta (RN06), o aluno que cancela a ida e também não voltará pode esquecer de desmarcar a volta e continuar na listagem como "em aula", e o motorista pode esperar por ele. |
| Horário de saída previsto diferente do real | Baixo | Como o início da rota é derivado do horário do grupo, um atraso da van faz com que cancelamentos feitos antes da saída real sejam classificados como tardios. O efeito é apenas uma notificação mais destacada ao motorista, sem bloqueio de nenhuma ação. |
| Exclusão de turma por engano | Médio | A exclusão apaga os vínculos, as viagens e as confirmações da turma e não pode ser desfeita (RN17). O app pede confirmação antes de excluir. |
| Sessão de 1 dia | Baixo | O aluno precisa fazer login novamente a cada dia para declarar o plano (RNF03). O lembrete continua chegando mesmo com a sessão expirada. |

## 12. Pontos em Aberto

Os pontos sobre o destinatário do push de cancelamento tardio, a confirmação antes do cancelamento tardio e a antecedência do lembrete foram fechados na versão 7 (ver changelog).

**Sobre planejamento:**
- Definição de marcos/sprints até a entrega de início de novembro.
- Confirmar com o professor se a integração com o serviço de push do Expo atende ao RNF04.
- Definição da API de geolocalização, apenas se o nice to have for retomado (Google Maps Platform é a opção considerada).

**Sobre regras de negócio:**
- Dias da semana em que o lembrete do plano do dia é enviado. O plano de implementação adota segunda a sexta como padrão configurável.
- Confirmar as regras de exclusão de grupos e turmas (RN16 e RN17), propostas no plano de implementação do backend.

---

## Changelog — o que mudou da versão 6 para a versão 7

| # | Mudança | Motivo |
|---|---|---|
| 1 | RF01 e RF14 ampliados para cadastro, consulta, edição e exclusão de turmas e grupos; RF02, RF03 e RN09 ajustados (troca de motorista e remoção de alunos) | A equipe definiu que o gerenciamento de grupos e turmas é um CRUD completo. |
| 2 | Novas RN16 e RN17 (seção 6.6): regras de exclusão de grupo e de turma | Consequência da mudança 1. Propostas no plano de implementação e ainda a confirmar (seção 12). |
| 3 | Nova RN18: remover aluno desativa o vínculo, que pode ser reativado | Preserva as confirmações já registradas e segue o campo `ativo` do modelo de dados. |
| 4 | Turma passa a ter nome em texto livre (seção 4 e RF01); número e capacidade da van ficam como dados cadastrais | Decisão da equipe. O modelo de dados ganhou a coluna `nome` em `TURMA`. |
| 5 | Nova RN15: o horário de saída da volta encerra o plano do dia; RF10, RN05 e RN13 ajustados | O cancelamento da ida deixa de valer "a qualquer momento" e passa a valer até a saída da volta, como na tela de dia encerrado. |
| 6 | RN05 esclarecida: o push de cancelamento tardio vai apenas para o motorista responsável pela turma | Ponto em aberto da versão 6, fechado. |
| 7 | RN06 reescrita: confirmação antes do cancelamento tardio, feita só na interface; aviso do WhatsApp apenas em texto, sem botão | Ponto em aberto da versão 6, fechado. O botão de abrir o WhatsApp foi adiado. |
| 8 | RF13 detalhado: lembrete enviado 2 horas antes da saída da ida | Ponto em aberto da versão 6, fechado. |
| 9 | RNF03 detalhado: sessão com validade de 1 dia | Decisão da equipe. |
| 10 | RNF04 reescrito: a API externa desta entrega é o serviço de push do Expo | A geolocalização permanece como nice to have. Depende de confirmação com o professor (seção 12). |
| 11 | RNF01 e RNF02 detalhados: expo-router, Gradle, MySQL e camada de repositório no app | Registra a arquitetura definida pela equipe. |
| 12 | Itens adicionados à seção 8 (telefone do motorista e abertura do WhatsApp; edição e exclusão de contas; recuperação de senha) e à seção 10 (botão do WhatsApp) | Deixa explícito o que foi adiado. |
| 13 | Riscos e pontos em aberto atualizados | Novos riscos de exclusão de turma por engano e de sessão de 1 dia; pontos fechados removidos; novos pontos sobre dias do lembrete, regras de exclusão e RNF04. |

## Changelog — o que mudou da versão 5 para a versão 6

| # | Mudança | Motivo |
|---|---|---|
| 1 | Status "não volto" removido do status da volta (RF09 e RN07) | O status da volta passa a ter apenas "em aula" e "liberado". Não voltar deixa de ser um status e passa a ser a ausência da volta no plano do dia. |
| 2 | RF11 restabelecido: cancelar a volta desmarcando a opção Volta (`inclui_volta`) | O RF11 havia sido incorporado ao RF09 na versão 3 por duplicar o status "não volto". Com a remoção do status, o cancelamento da volta volta a ser um requisito próprio. |
| 3 | Nova RN14: regra do cancelamento da volta | Define o limite (horário de saída da volta), o efeito na listagem e a ausência de cancelamento tardio da volta. |
| 4 | Seções 3 e 4, RN06 e risco "Volta esquecida após cancelar a ida" ajustados | Consistência com a nova regra. |

## Changelog — o que mudou da versão 4 para a versão 5

| # | Mudança | Motivo |
|---|---|---|
| 1 | Prazo limite configurável removido (seção 4, RF14, RNF07) | O limite da ida passa a ser o próprio horário de saída da ida do grupo. |
| 2 | RN02 reescrita: inclusão da ida até o horário de saída, com mensagem de bloqueio orientando o contato pelo WhatsApp | Definido na revisão dos fluxos; vale também para quem cancelou após a saída e tenta incluir novamente. |
| 3 | RN03 reescrita: inclusão da volta até o horário de saída da volta | Define quando a volta "fecha". |
| 4 | RN05 detalhada: campo `cancelou_apos_saida`, aluno exibido como "cancelado" e notificação push ao motorista | Diferencia o cancelamento tardio na listagem e no banco de dados. |
| 5 | RN06 substituída: aviso para comunicar o motorista pelo WhatsApp, sem pergunta sobre a volta | Simplifica o fluxo de cancelamento. |
| 6 | RN07 detalhada: status da volta editável até o horário de saída da volta | Após a saída, a edição é bloqueada. |
| 7 | RF06, RF08, RF09, RF10 e RN13 ajustados | Consistência com as novas regras. |
| 8 | Riscos atualizados e novos pontos em aberto | Cancelamento tardio irreversível, volta esquecida após cancelar a ida e destinatário do push. |

## Changelog — o que mudou da versão 3 para a versão 4

| # | Mudança | Motivo |
|---|---|---|
| 1 | RN04, RF06 e RF08 reescritos: quem não declara o plano do dia é considerado ausente e não aparece na listagem | Elimina o estado "pendente" e a necessidade de confirmar ausência explicitamente. |
| 2 | RF13 detalhado | O lembrete passa a ser controlado pelos campos `inclui_ida` e `inclui_volta`. |
| 3 | RN02 confirmada | A inclusão da ida após o prazo limite permanece bloqueada. |
| 4 | Motorista-chefe definido como motorista com permissão de administrador (seção 3) | O motorista-chefe pode conduzir uma turma; não é necessário um papel separado. |
| 5 | Horários de saída confirmados no grupo (seção 4) | Todas as vans do turno saem no mesmo horário. |
| 6 | Conceitos de Van e Viagem do dia adicionados à seção 4 | Capacidade apenas cadastral; viagem criada na primeira confirmação do dia. |
| 7 | Itens adicionados à seção 8 (Fora de Escopo) | Controle de lotação e horários de saída por turma. |
| 8 | Frequência da unificação registrada no item 9.1 | Cerca de duas vezes por mês; decisão de não implementar mantida. |
| 9 | Novo risco na seção 11: aluno que esquece de declarar o plano | Consequência direta das decisões sobre ausência de resposta e bloqueio após o prazo. |
| 10 | Seção 12 reduzida aos pontos de planejamento | Pontos de regra de negócio e implementação fechados; tópicos de nomenclatura e modelo de dados removidos a pedido da equipe. |

## Changelog — o que mudou da versão 2 para a versão 3

| # | Mudança | Motivo |
|---|---|---|
| 1 | Nova seção 6 — Regras de Negócio (RN01 a RN13) | Formaliza as regras definidas na etapa de design do produto. As seções seguintes foram renumeradas. |
| 2 | Regra de conflito de período movida da seção 3 para a RN08 | Centraliza todas as regras de negócio em uma única seção. |
| 3 | RF08 reescrito: plano do dia com as opções Ida e Volta independentes | Substitui os três valores fixos ("vai e volta", "somente vai", "somente volta") por duas escolhas independentes, mais simples na interface. |
| 4 | Inclusão da volta permitida após o prazo limite (RN03) | O prazo existe para o motorista planejar a ida; a volta não depende dele. |
| 5 | RF15 removido (registro de início de rota) | Uma ação manual do motorista que bloqueia etapas do aluno torna o fluxo frágil: se o motorista esquece, o aluno fica travado. O início da rota passa a ser o horário de saída da ida do grupo. |
| 6 | RF11 removido e incorporado ao RF09 | "Cancelar volta" e o status "não volto" representavam o mesmo estado por dois caminhos diferentes. |
| 7 | RF09 reescrito | Status da volta disponível sempre que a volta estiver no plano, com "em aula" como padrão, sem depender do fim da rota de ida. |
| 8 | RF10 reescrito | Cancelamento da ida disponível a qualquer momento, com notificação prioritária quando tardio. |
| 9 | Nova RN13 (princípio de funcionamento) | Registra a decisão de que nenhuma ação manual do motorista é pré-condição para ações do aluno. |
| 10 | RNF08 adicionado | Regras validadas no backend, não apenas na interface. |
| 11 | Item adicionado à seção 8 (Fora de Escopo): registro manual de início e fim de rota | Deixa explícito que a remoção do RF15 foi uma decisão consciente. |
| 12 | Riscos e pontos em aberto atualizados | Removido o risco de combinações contraditórias entre os status (resolvido pelo RF09); adicionado o risco de atraso em relação ao horário previsto; novos pontos em aberto sobre prazo, pendentes e modelo de dados. |

## Changelog — o que mudou da versão 1 para a versão 2

As referências de seção desta tabela seguem a numeração da versão 3.

| # | Mudança | Motivo |
|---|---|---|
| 1 | Nova camada **Grupo** (seção 4) | Caso real de unificação de turmas no retorno, não levantado originalmente. Também simplifica a validação de conflito de período. |
| 2 | RF07 esclarecido | O texto original permitia ler que o aluno se vinculava a turmas ao se cadastrar, contradizendo a seção 8. Agora separa criação de conta de vínculo com turma. |
| 3 | Item "autoinscrição" reescrito na seção 8 | Mesma ambiguidade do item anterior, vista do outro lado. |
| 4 | Nova seção 9 — casos identificados e despriorizados | Registra a unificação de turmas como caso conhecido e conscientemente adiado, em vez de omissão. |
| 5 | RF08 e RF09 detalhados com valores possíveis | Os requisitos originais diziam apenas "confirmar presença", sem definir o que era registrado. |
| 6 | RF14, RF15, RF16 adicionados | Cadastro de grupo, registro de início de rota e visualização entre turmas do grupo. (O RF15 foi removido na versão 3.) |
| 7 | RNF07 adicionado | Prazo de confirmação configurável por grupo. |
| 8 | Item removido da seção 8: "múltiplas vans do mesmo motorista-chefe" | Passou a ser suportado pelo modelo de grupo. |
| 9 | Riscos e pontos em aberto atualizados | Refletem as decisões e dúvidas surgidas na modelagem de dados. |

---

*Documento elaborado como parte do processo de definição de escopo do projeto acadêmico, com base em levantamento de requisitos junto à equipe, revisado após a modelagem do banco de dados, após a definição das regras de negócio, após o fechamento dos pontos em aberto, após a revisão dos fluxos de uso, após a redefinição do cancelamento da volta e após a validação do plano de implementação do backend.*
