@echo off
echo [BUILD] Compilation de la version Web (Wasm)...
call gradlew :composeApp:wasmJsBrowserDistribution

if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] La compilation a echoue.
    pause
    exit /b %ERRORLEVEL%
)

set DIST_DIR=composeApp\build\dist\wasmJs\productionExecutable

echo [PREPARE] Configuration des fichiers pour Vercel...
copy vercel.json %DIST_DIR%\ /Y
copy web\index.html %DIST_DIR%\ /Y

echo [DEPLOY] Envoi vers Vercel...
cd %DIST_DIR%
npx vercel --prod --yes

if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] Le deploiement a echoue.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [SUCCES] Votre application DschangMarket Web est mise a jour !
echo URL: https://dschang-market-web.vercel.app
pause
