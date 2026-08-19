@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo [BUILD] Compilation de la version Web (JS Universal)...
echo ===================================================

call gradlew :composeApp:jsBrowserDistribution

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] La compilation a echoue.
    pause
    exit /b %ERRORLEVEL%
)

set DIST_DIR=composeApp\build\dist\js\productionExecutable
set WEB_DIST=web_dist

echo.
echo [PREPARE] Configuration du dossier de deploiement local (web_dist)...
if not exist "%DIST_DIR%" (
    echo [ERREUR] Repertoire de distribution non trouve : %DIST_DIR%
    pause
    exit /b 1
)

:: Nettoyage et creation du dossier web_dist
if exist "%WEB_DIST%" rd /s /q "%WEB_DIST%"
mkdir "%WEB_DIST%"

:: Copie du build Wasm
xcopy "%DIST_DIR%\*" "%WEB_DIST%\" /S /E /Y /I >nul

:: Copie du fichier de configuration Vercel
if exist "vercel.json" (
    copy "vercel.json" "%WEB_DIST%\" /Y >nul
)

:: Copie des fichiers personnalisés du dossier 'web'
if exist "web" (
    echo [INFO] Application des fichiers personnalises du dossier 'web'...
    xcopy "web\*" "%WEB_DIST%\" /S /E /Y /I >nul
)

echo.
echo ===================================================
echo [GIT] Votre build est pret dans le dossier web_dist.
echo Poussez vos modifications sur Git pour deployer sur Vercel.
echo ===================================================
echo.
pause
exit /b 0

:: Ancienne methode directe (desactivee car maintenant via Git)
:: cd %WEB_DIST%
:: call npx vercel --prod --yes
