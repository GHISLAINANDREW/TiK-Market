@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo [BUILD] Compilation de la version Web (Wasm)...
echo ===================================================

call gradlew :composeApp:wasmJsBrowserDistribution

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] La compilation a echoue.
    pause
    exit /b %ERRORLEVEL%
)

set DIST_DIR=composeApp\build\dist\wasmJs\productionExecutable

echo.
echo [PREPARE] Configuration des fichiers pour Vercel...
if not exist "%DIST_DIR%" (
    echo [ERREUR] Repertoire de distribution non trouve : %DIST_DIR%
    pause
    exit /b 1
)

:: Copie du fichier de configuration Vercel
if exist "vercel.json" (
    copy "vercel.json" "%DIST_DIR%\" /Y >nul
)

:: Copie des fichiers personnalisés du dossier 'web' (index.html, manifest, icons, service-worker)
:: On utilise xcopy pour inclure les sous-dossiers comme composeResources si présents
if exist "web" (
    echo [INFO] Application des fichiers personnalises du dossier 'web'...
    xcopy "web\*" "%DIST_DIR%\" /S /E /Y /I >nul
)

echo.
echo ===================================================
echo [DEPLOY] Envoi vers Vercel...
echo ===================================================

cd %DIST_DIR%

:: Verification de la présence de npx/vercel
where npx >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] Node.js/npx n'est pas installe ou n'est pas dans le PATH.
    pause
    exit /b 1
)

:: Déploiement vers Vercel
call npx vercel --prod --yes

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] Le deploiement a echoue. Vérifiez votre connexion ou vos identifiants Vercel.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [SUCCES] Votre application DschangMarket Web est mise a jour !
echo URL: https://dschang-market-web.vercel.app
echo.
pause
