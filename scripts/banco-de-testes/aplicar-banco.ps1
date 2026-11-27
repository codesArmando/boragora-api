# Aplica o banco dos testes manuais no MySQL do docker compose do boragora-api (Windows).
#
# Uso (PowerShell, nesta pasta):
#   .\aplicar-banco.ps1                 confirmações no dia de hoje
#   .\aplicar-banco.ps1 -Dia 2026-11-27 confirmações nesse dia (o mesmo do APP_RELOGIO_FIXO)
#
# Se o Windows bloquear o script, rode antes, só nesta janela:
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
param(
    [string]$Dia = "",
    [string]$Container = $(if ($env:BORAGORA_CONTAINER) { $env:BORAGORA_CONTAINER } else { "boragora-mysql" }),
    [string]$Nome = $(if ($env:DB_NOME) { $env:DB_NOME } else { "boragora" }),
    [string]$Usuario = $(if ($env:DB_USUARIO) { $env:DB_USUARIO } else { "boragora" }),
    [string]$Senha = $(if ($env:DB_SENHA) { $env:DB_SENHA } else { "boragora" })
)
$ErrorActionPreference = "Stop"

function Falhar($mensagem) { Write-Host "ERRO: $mensagem" -ForegroundColor Red; exit 1 }

$sql = Join-Path $PSScriptRoot "banco-de-testes.sql"
if ($Dia -ne "" -and $Dia -notmatch '^\d{4}-\d{2}-\d{2}$') { Falhar "a data deve estar no formato AAAA-MM-DD, por exemplo 2026-11-27." }
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { Falhar "o Docker não foi encontrado." }
if (-not (Test-Path $sql)) { Falhar "não encontrei $sql." }

$estado = (docker inspect -f '{{.State.Health.Status}}' $Container 2>$null)
if ($estado -ne "healthy") {
    Falhar "o container $Container não está rodando. Na pasta boragora-api, rode: docker compose up -d   e espere o status (healthy)."
}

$consulta = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('usuario','grupo','turma','turma_aluno','viagem','confirmacao');"
$tabelas = (docker exec -e MYSQL_PWD=$Senha $Container mysql -N -u $Usuario $Nome -e $consulta)
if ("$tabelas".Trim() -ne "6") {
    Falhar "as tabelas ainda não existem. Suba a API uma vez (.\gradlew.bat bootRun), espere terminar de subir, pare com Ctrl+C e rode este script de novo."
}

Write-Host "Atenção: todos os dados do banco '$Nome' serão apagados e trocados pelo cenário dos testes." -ForegroundColor Yellow
$resposta = Read-Host "Continuar? [s/N]"
if ($resposta -notmatch '^[sS]$') { Write-Host "Nada foi alterado."; exit 0 }

# Lê o arquivo como UTF-8 para os acentos chegarem certos ao MySQL.
$conteudo = [System.IO.File]::ReadAllText($sql, [System.Text.Encoding]::UTF8)
if ($Dia -ne "") { $conteudo = "SET @dia_teste = '$Dia';`n" + $conteudo }
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$conteudo | docker exec -i -e MYSQL_PWD=$Senha $Container mysql --default-character-set=utf8mb4 --table -u $Usuario $Nome
if ($LASTEXITCODE -ne 0) { Falhar "o MySQL recusou o script (veja a mensagem acima)." }

$hoje = Get-Date -Format "yyyy-MM-dd"
Write-Host ""
Write-Host "Banco aplicado. Agora:" -ForegroundColor Green
Write-Host "  1. No app, se você já estava logado antes desta primeira aplicação, toque em 'Sair' e entre de novo."
Write-Host "  2. Suba a API com o relógio do roteiro, por exemplo:"
Write-Host "       `$env:APP_RELOGIO_FIXO=`"$($hoje)T19:00`"; .\gradlew.bat bootRun"
Write-Host "  3. Logins: admin@boragora.local / admin123; os demais com senha123."
