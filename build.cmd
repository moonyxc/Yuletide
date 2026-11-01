@echo off
setlocal
cd /d "%~dp0"

if /i "%~1"=="clean" (
	call "%~dp0gradlew.bat" clean || exit /b 1
)

call "%~dp0gradlew.bat" build || exit /b 1

if not exist dist mkdir dist
set "JAR="
for /f "delims=" %%F in ('dir /b /o-d build\libs\yuletide-*.jar ^| findstr /v /i /r "sources\.jar dev\.jar"') do (
	if not defined JAR set "JAR=%%F"
)
if not defined JAR (
	echo Keine JAR in build\libs gefunden.
	exit /b 1
)

copy /y "build\libs\%JAR%" dist\ >nul
echo Fertig: dist\%JAR%
endlocal
