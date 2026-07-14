@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo [BUILD] Compilation de la version Web (WasmJS)...
java -jar gradle/wrapper/gradle-wrapper.jar :composeApp:wasmJsProductionExecutableCompileSync

if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] La compilation a echoue.
    pause
    exit /b %ERRORLEVEL%
)

echo [FIX] Correction de l'import map (chemin js-joda)...
set DIST_DIR=composeApp\build\web\dist-prod

:: Copier js-joda.esm.js hors de node_modules
copy "%DIST_DIR%\node_modules\@js-joda\core\js-joda.esm.js" "%DIST_DIR%\js-joda.esm.js" /Y >nul

:: Corriger l'import map dans index.html
powershell -Command "(Get-Content '%DIST_DIR%\index.html') -replace '\./node_modules/@js-joda/core/dist/js-joda\.esm\.js', './js-joda.esm.js' | Set-Content '%DIST_DIR%\index.html'"

echo [DEPLOY] Envoi vers Vercel...
cd /d "%DIST_DIR%"
vercel --prod --yes

if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] Le deploiement a echoue.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [SUCCES] DschangMarket Web : https://dschang-market.vercel.app
echo [NOTE] N'oubliez pas de commit et push pour l'auto-deploiement !
pause
