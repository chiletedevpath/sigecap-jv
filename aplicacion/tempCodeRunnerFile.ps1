param([string]$JavaHome = $env:JAVA_HOME, [switch]$Probar)
$ErrorActionPreference = 'Stop'
if ($JavaHome) {
    $compilador = Join-Path $JavaHome 'bin\javac.exe'
    $java = Join-Path $JavaHome 'bin\java.exe'
} else {
    $compilador = (Get-Command javac -ErrorAction Stop).Source
    $java = (Get-Command java -ErrorAction Stop).Source
}
$salida = Join-Path $PSScriptRoot 'out'
New-Item -ItemType Directory -Force -Path $salida | Out-Null
$fuentes = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& $compilador --release 17 -encoding UTF-8 -d $salida @fuentes
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación.' }
Write-Host 'Compilación Java 17 correcta.'
if ($Probar) {
    $pruebas = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'test') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    & $compilador --release 17 -encoding UTF-8 -cp $salida -d $salida @pruebas
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de pruebas.' }
    & $java '-Djava.awt.headless=true' -cp $salida pe.utp.sigecapjv.PruebaFlujo
} else {
    & $java -cp $salida pe.utp.sigecapjv.vista.FrmPrincipal
}
if ($LASTEXITCODE -ne 0) { throw 'Falló la ejecución.' }
