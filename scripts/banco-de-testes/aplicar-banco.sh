#!/usr/bin/env bash
# Aplica o banco dos testes manuais no MySQL do docker compose do boragora-api.
#
# Uso:  ./aplicar-banco.sh              confirmações no dia de hoje
#       ./aplicar-banco.sh 2026-11-27   confirmações nesse dia (o mesmo do APP_RELOGIO_FIXO)
#
# Variáveis opcionais, com os mesmos padrões do docker-compose.yml:
#   BORAGORA_CONTAINER (boragora-mysql), DB_NOME (boragora), DB_USUARIO (boragora), DB_SENHA (boragora)
set -euo pipefail

PASTA="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL="$PASTA/banco-de-testes.sql"
CONTAINER="${BORAGORA_CONTAINER:-boragora-mysql}"
NOME="${DB_NOME:-boragora}"
USUARIO="${DB_USUARIO:-boragora}"
SENHA="${DB_SENHA:-boragora}"
DIA="${1:-}"

falhar() { echo "ERRO: $*" >&2; exit 1; }

if [[ -n "$DIA" && ! "$DIA" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  falhar "a data deve estar no formato AAAA-MM-DD, por exemplo 2026-11-27."
fi
command -v docker >/dev/null || falhar "o Docker não foi encontrado."
[[ -f "$SQL" ]] || falhar "não encontrei $SQL."

estado="$(docker inspect -f '{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || true)"
[[ "$estado" == "healthy" ]] || falhar "o container $CONTAINER não está rodando (estado: ${estado:-inexistente}).
Na pasta boragora-api, rode: docker compose up -d   e espere o status (healthy)."

mysql_no_container() {
  docker exec -i -e MYSQL_PWD="$SENHA" "$CONTAINER" \
    mysql --default-character-set=utf8mb4 -u"$USUARIO" "$NOME" "$@"
}

tabelas="$(mysql_no_container -N -e "SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name IN ('usuario','grupo','turma','turma_aluno','viagem','confirmacao');")"
[[ "$tabelas" == "6" ]] || falhar "as tabelas ainda não existem. Suba a API uma vez (./gradlew bootRun),
espere ela terminar de subir, pare com Ctrl+C e rode este script de novo."

echo "Atenção: todos os dados do banco '$NOME' serão apagados e trocados pelo cenário dos testes."
if [[ -t 0 ]]; then
  read -r -p "Continuar? [s/N] " resposta
  [[ "$resposta" =~ ^[sS]$ ]] || { echo "Nada foi alterado."; exit 0; }
fi

{
  if [[ -n "$DIA" ]]; then echo "SET @dia_teste = '$DIA';"; fi
  cat "$SQL"
} | mysql_no_container --table

cat <<'FIM'

Banco aplicado. Agora:
  1. No app, se você já estava logado antes desta primeira aplicação, toque em "Sair" e entre de novo.
  2. Suba a API com o relógio do roteiro, por exemplo:
       APP_RELOGIO_FIXO=$(date +%F)T19:00 ./gradlew bootRun
  3. Logins: admin@boragora.local / admin123; os demais com senha123.
FIM
