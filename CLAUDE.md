# Boragora Viagens — API

Backend em Java 21 + Spring Boot + Gradle, banco MySQL. Este repositório é só o backend; o app fica no repositório `boragora-mobile`, clonado ao lado deste (`../boragora-mobile`).

## Documentos
- docs/plano-implementacao-backend.md — ler a etapa em execução e as seções 1 e 2
- docs/checklist-backend.md — marcar os itens conforme forem concluídos
- docs/documento-escopo-app-van-v7.md — consultar só em caso de dúvida sobre uma regra
- docs/mer_ajustado_app_van_dark.html — consultar na Etapa 1 ou ao mexer no esquema (o que importa é o bloco erDiagram; o resto do arquivo é estilo)

O app lê estes documentos (principalmente o checklist e o formato das respostas no plano). Manter o checklist em dia é o que diz ao time do app o que já pode ser integrado.

## Regras de trabalho
- Executar uma etapa por vez, na ordem do plano, seguindo a seção 8 dele.
- Não alterar o esquema do banco nem implementar itens fora do plano sem perguntar.
- Não alterar nada em ../boragora-mobile: é outro repositório.
- Em conflito entre plano e escopo, parar e perguntar.

## Comandos
- Subir o banco: docker compose up -d
- Rodar a API: ./gradlew bootRun
- Rodar os testes: ./gradlew test
