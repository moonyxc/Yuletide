#Requires -Version 5.1
<#
.SYNOPSIS
  Baut die Yuletide-Mod und legt die fertige JAR in dist\ ab.
.PARAMETER Clean
  Führt vorher "gradlew clean" aus.
#>
param(
	[switch]$Clean
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if ($Clean) {
	& .\gradlew.bat clean
	if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

& .\gradlew.bat build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$jar = Get-ChildItem build\libs -Filter 'yuletide-*.jar' |
	Where-Object { $_.Name -notmatch '-(sources|dev)\.jar$' } |
	Sort-Object LastWriteTime -Descending |
	Select-Object -First 1
if (-not $jar) { throw 'Keine JAR in build\libs gefunden.' }

New-Item -ItemType Directory -Force dist | Out-Null
Copy-Item $jar.FullName dist\ -Force
Write-Host "Fertig: dist\$($jar.Name)" -ForegroundColor Green
