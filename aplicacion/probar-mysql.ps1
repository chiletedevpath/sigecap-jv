param([switch]$Inicio, [switch]$Ventanas, [string]$JavaHome = $env:JAVA_HOME, [string]$ConnectorJar = $env:SIGECAP_JDBC_JAR)
$ErrorActionPreference = 'Stop'
if ($Inicio -and $Ventanas) { throw 'Use -Ventanas sin -Inicio: la navegación necesita los datos de la suite general.' }
$clase = if ($Inicio) { 'pe.utp.sigecapjv.PruebaInicioMySql' } else { 'pe.utp.sigecapjv.PruebaMySql' }
$nombre = 'sigecap-prueba-' + [guid]::NewGuid().ToString('N').Substring(0,12)
$variables = @('MYSQL_ROOT_PASSWORD','MYSQL_PASSWORD','SIGECAP_DB_URL','SIGECAP_DB_USER','SIGECAP_DB_PASSWORD','SIGECAP_DB_TEST')
$anteriores = @{}
foreach ($variable in $variables) { $anteriores[$variable] = [Environment]::GetEnvironmentVariable($variable, 'Process') }
try {
    $env:MYSQL_ROOT_PASSWORD = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
    $env:MYSQL_PASSWORD = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
    $esquema = (Resolve-Path (Join-Path $PSScriptRoot '../database/001-esquema.sql')).Path
    & docker run --detach --name $nombre --label sigecap.test=true --publish '127.0.0.1::3306' --env MYSQL_ROOT_PASSWORD --env MYSQL_PASSWORD --env MYSQL_USER=sigecap_test --env MYSQL_DATABASE=sigecap_jv --mount "type=bind,source=$esquema,target=/docker-entrypoint-initdb.d/001-esquema.sql,readonly" mysql:8.4 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar MySQL de prueba.' }
    $listo = $false
    for ($intento=0; $intento -lt 45; $intento++) {
        $preferenciaError = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            & docker exec $nombre sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -h 127.0.0.1 -u sigecap_test -D sigecap_jv -Nse "SELECT COUNT(*) FROM vw_control_vigencias"' 2>$null | Out-Null
            $codigoSalida = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $preferenciaError
        }
        if ($codigoSalida -eq 0) { $listo = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $listo) { throw 'MySQL no terminó de inicializar el esquema.' }
    # Simula la estructura anterior únicamente en este contenedor recién creado.
    & docker exec $nombre sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root -D sigecap_jv -e "ALTER TABLE certificado DROP COLUMN entidadEmisora"'
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar la prueba de migración.' }
    $migracion = (Resolve-Path (Join-Path $PSScriptRoot '../database/004-entidad-certificado.sql')).Path
    & docker cp $migracion "${nombre}:/tmp/migracion.sql"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo copiar la migración.' }
    foreach ($vez in 1..2) {
        & docker exec $nombre sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root sigecap_jv < /tmp/migracion.sql'
        if ($LASTEXITCODE -ne 0) { throw 'Falló la migración de institución emisora.' }
    }
    Write-Host 'OK: migración desde la estructura anterior y segunda ejecución sin errores.'
    # Esta instancia acaba de crearse: simula una base anterior a la recuperación.
    & docker exec $nombre sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root sigecap_jv -e "DROP TABLE recuperacion_contrasena"'
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar la migración de recuperación.' }
    $recuperacion = (Resolve-Path (Join-Path $PSScriptRoot '../database/006-recuperacion-contrasena.sql')).Path
    & docker cp $recuperacion "${nombre}:/tmp/recuperacion.sql"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo copiar la migración de recuperación.' }
    foreach ($vez in 1..2) {
        & docker exec $nombre sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root sigecap_jv < /tmp/recuperacion.sql'
        if ($LASTEXITCODE -ne 0) { throw 'Falló la migración de recuperación.' }
    }
    $puerto = (& docker port $nombre '3306/tcp').Trim().Split(':')[-1]
    $env:SIGECAP_DB_URL = "jdbc:mysql://127.0.0.1:$puerto/sigecap_jv?sslMode=DISABLED&allowPublicKeyRetrieval=true"
    $env:SIGECAP_DB_USER = 'sigecap_test'
    $env:SIGECAP_DB_PASSWORD = $env:MYSQL_PASSWORD
    $env:SIGECAP_DB_TEST = 'true'
    $permisos = (Resolve-Path (Join-Path $PSScriptRoot '../database/docker/002-permisos-app.sh')).Path
    & docker cp $permisos "${nombre}:/tmp/permisos-app.sh"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo copiar el script de permisos.' }
    & docker exec --env SIGECAP_DB_PASSWORD $nombre bash /tmp/permisos-app.sh
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar la cuenta con permisos mínimos.' }
    & (Join-Path $PSScriptRoot 'ejecutar.ps1') -JavaHome $JavaHome -ConnectorJar $ConnectorJar -ProbarMySql -ClasePrueba $clase
    # Nueva JVM y nuevas conexiones, sin reiniciar la base.
    & (Join-Path $PSScriptRoot 'ejecutar.ps1') -JavaHome $JavaHome -ConnectorJar $ConnectorJar -ProbarMySql -ClasePrueba $clase
    if ($Ventanas) {
        & (Join-Path $PSScriptRoot 'ejecutar.ps1') -JavaHome $JavaHome -ConnectorJar $ConnectorJar -ProbarMySql -Ventanas -ClasePrueba 'pe.utp.sigecapjv.PruebaNavegacionMySql'
    }
    & (Join-Path $PSScriptRoot '../database/verificar-respaldo.ps1') -ContenedorOrigen $nombre
} finally {
    & docker rm --force --volumes $nombre 2>$null | Out-Null
    foreach ($variable in $variables) { [Environment]::SetEnvironmentVariable($variable, $anteriores[$variable], 'Process') }
}
