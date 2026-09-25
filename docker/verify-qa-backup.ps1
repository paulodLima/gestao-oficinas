[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$compose = Join-Path $PSScriptRoot 'docker-compose.qa.yml'
$stamp = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 6)
$evidence = Join-Path $repo "artifacts/qa-backup-$stamp"
$restore = "oficinas-qa-restore-$stamp"
$restoreDb = "$restore-db"
$restorePhotos = "$restore-photos"
function Docker {
    & docker.exe @args
    if ($LASTEXITCODE -ne 0) { throw "Docker falhou: $($args -join ' ')" }
}
function Sql($container, $query) { Docker exec $container psql -U qa -d oficinas_qa -At -v ON_ERROR_STOP=1 -c $query }

# Resolve only the explicitly isolated QA project. Never target the user's main stack.
$apiId = Docker compose -f $compose ps -q api
$dbId = Docker compose -f $compose ps -q postgres
if (!$apiId -or !$dbId) { throw 'Inicie o Compose QA antes da verificação.' }
$api = (Docker inspect $apiId | ConvertFrom-Json)[0]
if ($api.Config.Labels.'com.docker.compose.project' -ne 'gestao-oficinas-qa') { throw 'Projeto inesperado.' }
$sourcePhotos = ($api.Mounts | Where-Object Destination -eq '/data/photos').Name
if ($sourcePhotos -ne 'gestao-oficinas-qa_qa-photos') { throw 'Volume de origem inesperado.' }
if (Test-Path -LiteralPath $evidence) { throw 'Destino já existe; nada será sobrescrito.' }
New-Item -ItemType Directory -Path $evidence | Out-Null
$apiStopped = $false
try {
    Docker compose -f $compose stop api
    $apiStopped = $true
    Docker exec $dbId pg_dump -U qa -d oficinas_qa -Fc -f "/tmp/$stamp.dump"
    Docker cp "${dbId}:/tmp/$stamp.dump" (Join-Path $evidence 'database.dump')
    Docker run --rm --network none --user 0 --mount "type=volume,src=$sourcePhotos,dst=/photos,readonly" --mount "type=bind,src=$evidence,dst=/backup" --entrypoint sh gestao-oficinas-qa-api:local -c 'tar -cf /backup/photos.tar -C /photos .'

    # Unique new volumes: deliberately kept for inspection; never restore over an existing target.
    $existing = @(Docker volume ls --format '{{.Name}}')
    if ($existing -contains $restoreDb -or $existing -contains $restorePhotos) { throw 'Destino de restauração já existe.' }
    Docker volume create $restoreDb
    Docker volume create $restorePhotos
    Docker run -d --name $restore --network none -e POSTGRES_DB=oficinas_qa -e POSTGRES_USER=qa -e POSTGRES_PASSWORD=qa-local-only --mount "type=volume,src=$restoreDb,dst=/var/lib/postgresql/data" postgres:17-alpine
    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        & docker.exe exec $restore pg_isready -U qa -d oficinas_qa 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (!$ready) { throw 'PostgreSQL de restauração não ficou pronto.' }
    Docker cp (Join-Path $evidence 'database.dump') "${restore}:/tmp/database.dump"
    Docker exec $restore pg_restore -U qa -d oficinas_qa --exit-on-error /tmp/database.dump
    Docker run --rm --network none --user 0 --mount "type=volume,src=$restorePhotos,dst=/photos" --mount "type=bind,src=$evidence,dst=/backup,readonly" --entrypoint sh gestao-oficinas-qa-api:local -c 'tar -xf /backup/photos.tar -C /photos'

    $tables = Sql $dbId "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename"
    foreach ($table in $tables) {
        if ($table -notmatch '^[a-z_]+$') { throw 'Nome de tabela inesperado.' }
        $query = "SELECT count(*) || ':' || md5(coalesce(string_agg(row_to_json(t)::text, '' ORDER BY row_to_json(t)::text),'')) FROM $table t"
        $source = Sql $dbId $query
        $restored = Sql $restore $query
        if ($source -ne $restored) { throw "Conteúdo diferente em $table" }
        Write-Output "TABLE OK $table $source"
    }
    $sourceHash = Docker run --rm --network none --mount "type=volume,src=$sourcePhotos,dst=/photos,readonly" --entrypoint sh gestao-oficinas-qa-api:local -c 'cd /photos && find . -type f -exec sha256sum {} \; | sort'
    $restoredHash = Docker run --rm --network none --mount "type=volume,src=$restorePhotos,dst=/photos,readonly" --entrypoint sh gestao-oficinas-qa-api:local -c 'cd /photos && find . -type f -exec sha256sum {} \; | sort'
    if (!$sourceHash) { throw 'Inclua ao menos uma foto sintética antes de validar o backup.' }
    if (($sourceHash -join "`n") -ne ($restoredHash -join "`n")) { throw 'Fotos restauradas diferem da origem.' }
    Write-Output "PHOTOS SHA256 OK $(@($sourceHash).Count) arquivos"
    Get-FileHash (Join-Path $evidence 'database.dump'), (Join-Path $evidence 'photos.tar') -Algorithm SHA256
    Write-Output "BACKUP RESTORE OK: $evidence"
    Write-Output "Volumes preservados: $restoreDb / $restorePhotos"
} finally {
    if ($apiStopped) { Docker compose -f $compose start api }
    if (@(& docker.exe ps -a --format '{{.Names}}') -contains $restore) { Docker stop $restore }
}
