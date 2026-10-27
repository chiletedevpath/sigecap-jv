param(
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$Probar,
    [switch]$ProbarMySql,
    [ValidateSet("pe.utp.sigecapjv.PruebaMySql", "pe.utp.sigecapjv.PruebaInicioMySql")]
    [string]$ClasePrueba = "pe.utp.sigecapjv.PruebaMySql",
    [string]$ConnectorJar = $env:SIGECAP_JDBC_JAR
)
$ErrorActionPreference = 'Stop'
if ($JavaHome) {
    $compilador = Join-Path $JavaHome 'bin\javac.exe'
    $java = Join-Path $JavaHome 'bin\java.exe'
} else {
    $compilador = (Get-Command javac -ErrorAction Stop).Source
    $java = (Get-Command java -ErrorAction Stop).Source
}
$salida = Join-Path $PSScriptRoot 'out'
$classpath = $salida
if (-not $Probar -or $ProbarMySql) {
    if (-not $ConnectorJar) {
        $ConnectorJar = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository/com/mysql/mysql-connector-j/26.7.0/mysql-connector-j-26.7.0.jar'
    }
    if (-not (Test-Path -LiteralPath $ConnectorJar -PathType Leaf)) {
        throw 'No se encontró MySQL Connector/J. Indique -ConnectorJar o SIGECAP_JDBC_JAR con la ruta del JAR.'
    }
    $classpath = $salida + [IO.Path]::PathSeparator + $ConnectorJar
}
New-Item -ItemType Directory -Force -Path $salida | Out-Null
$fuentes = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& $compilador --release 17 -encoding UTF-8 -d $salida @fuentes
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación.' }
Write-Host 'Compilación Java 17 correcta.'
if ($Probar -or $ProbarMySql) {
    $pruebas = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'test') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    & $compilador --release 17 -encoding UTF-8 -cp $salida -d $salida @pruebas
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de pruebas.' }
    if ($ProbarMySql) {
        & $java '-Dsigecap.pruebas=true' '-Djava.awt.headless=true' -cp $classpath $ClasePrueba
    } else {
        & $java '-Dsigecap.pruebas=true' '-Djava.awt.headless=true' -cp $classpath pe.utp.sigecapjv.PruebaFlujo
    }
} else {
    & $java -cp $classpath pe.utp.sigecapjv.vista.FrmPrincipal
}
if ($LASTEXITCODE -ne 0) { throw 'Falló la ejecución.' }
