param([Parameter(Mandatory)][string]$ContenedorOrigen)
$ErrorActionPreference = 'Stop'

# Solo se acepta el contenedor desechable que crea probar-mysql.ps1.
$etiqueta = & docker inspect --format '{{ index .Config.Labels "sigecap.test" }}' $ContenedorOrigen
if ($LASTEXITCODE -ne 0 -or $etiqueta -ne 'true') { throw 'El origen debe ser una base de prueba aislada.' }
if (-not $env:MYSQL_ROOT_PASSWORD) { throw 'Ejecute esta verificación desde probar-mysql.ps1.' }
$destino = 'sigecap-restauracion-' + [guid]::NewGuid().ToString('N').Substring(0,12)
$archivo = Join-Path ([IO.Path]::GetTempPath()) ($destino + '.sql')
$restaurado = Join-Path ([IO.Path]::GetTempPath()) ($destino + '-verificado.sql')
$creado = $false
$exportar = 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -u root --single-transaction --no-tablespaces --set-gtid-purged=OFF --skip-comments --skip-dump-date --order-by-primary --hex-blob sigecap_jv > /tmp/respaldo.sql'
try {
    & docker exec $ContenedorOrigen sh -c $exportar
    if ($LASTEXITCODE -ne 0) { throw 'Falló la exportación del respaldo de prueba.' }
    & docker cp "${ContenedorOrigen}:/tmp/respaldo.sql" $archivo
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo leer el respaldo.' }
    & docker run --detach --name $destino --label sigecap.test=true --env MYSQL_ROOT_PASSWORD --env MYSQL_DATABASE=sigecap_jv mysql:8.4 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base de restauración.' }
    $creado = $true
    $listo = $false
    for ($intento = 0; $intento -lt 45; $intento++) {
        $preferencia = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            & docker exec $destino sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -h 127.0.0.1 -u root -D sigecap_jv -e "SELECT 1"' 2>$null | Out-Null
            $codigo = $LASTEXITCODE
        } finally { $ErrorActionPreference = $preferencia }
        if ($codigo -eq 0) { $listo = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $listo) { throw 'La base de restauración no terminó de iniciar.' }
    & docker cp $archivo "${destino}:/tmp/respaldo.sql"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo copiar el respaldo al destino.' }
    & docker exec $destino sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root sigecap_jv < /tmp/respaldo.sql'
    if ($LASTEXITCODE -ne 0) { throw 'Falló la restauración.' }
    & docker exec $destino sh -c $exportar
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo comprobar la base restaurada.' }
    & docker cp "${destino}:/tmp/respaldo.sql" $restaurado
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo leer la comprobación.' }
    if ((Get-FileHash -LiteralPath $archivo).Hash -ne (Get-FileHash -LiteralPath $restaurado).Hash) {
        throw 'La estructura o los datos restaurados difieren del respaldo.'
    }
    & docker exec $destino sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root -D sigecap_jv -Nse "SELECT COUNT(*) FROM vw_control_vigencias"'
    if ($LASTEXITCODE -ne 0) { throw 'La vista restaurada no se puede consultar.' }
    Write-Host 'OK: respaldo restaurado en otro MySQL; estructura y datos coinciden por SHA-256 y la vista funciona.'
} finally {
    if ($creado) { & docker rm --force --volumes $destino | Out-Null }
    foreach ($temporal in @($archivo, $restaurado)) {
        if (Test-Path -LiteralPath $temporal) { Remove-Item -LiteralPath $temporal }
    }
}
