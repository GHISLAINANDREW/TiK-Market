# Plan de Mise à Jour TiK-Market

Ce plan propose une série d'améliorations basées sur l'analyse du projet, visant à améliorer la qualité du code, l'expérience utilisateur et la robustesse de l'application multiplateforme.

## User Review Required

> [!IMPORTANT]
> Le fichier `HomeScreen.kt` fait actuellement plus de 42 Ko. Une refactorisation majeure est proposée pour faciliter la maintenance.

## Proposed Changes

### 1. Refactorisation de l'Architecture
L'objectif est de diviser les fichiers volumineux et de standardiser la gestion d'état.

#### [MODIFY] [HomeScreen.kt](file:///D:/aaaaaaaaa/workspace/Tik-Market/composeApp/src/commonMain/kotlin/com/tik_market/ui/home/HomeScreen.kt)
- Extraire les sections (Header, Categories, ProductGrid, Stories) dans des fichiers séparés dans `ui/home/components/`.
- Simplifier la logique de chargement des données.

#### [NEW] [HomeViewModel.kt](file:///D:/aaaaaaaaa/workspace/Tik-Market/composeApp/src/commonMain/kotlin/com/tik_market/ui/home/HomeViewModel.kt)
- Introduire un ViewModel pour gérer la logique métier de l'accueil (chargement API, filtres, cache) séparément de la vue.

### 2. Améliorations UI/UX (Quick Wins)
Améliorer le ressenti "Premium" de l'application.

#### [NEW] [Shimmer.kt](file:///D:/aaaaaaaaa/workspace/Tik-Market/composeApp/src/commonMain/kotlin/com/tik_market/ui/components/Shimmer.kt)
- Ajouter un composant de chargement "Shimmer" pour remplacer les indicateurs de progression circulaires.

#### [MODIFY] [Theme.kt](file:///D:/aaaaaaaaa/workspace/Tik-Market/composeApp/src/commonMain/kotlin/com/tik_market/theme/Theme.kt)
- Optimiser les transitions de thèmes et s'assurer du support complet du `isSystemInDarkTheme()`.

### 3. Stabilité Web (WasmJs)
Résoudre les problèmes de build et améliorer le support PWA.

#### [MODIFY] [build.gradle.kts](file:///D:/aaaaaaaaa/workspace/Tik-Market/composeApp/build.gradle.kts)
- Corriger les avertissements de configuration Gradle pour WasmJs.

#### [NEW] [manifest.json](file:///D:/aaaaaaaaa/workspace/Tik-Market/composeApp/src/wasmJsMain/resources/manifest.json)
- Ajouter le support PWA pour permettre l'installation de l'application web.

## Verification Plan

### Automated Tests
- Lancer le build Android : `gradlew assembleDebug`
- Lancer le build WasmJs : `gradlew wasmJsBrowserDevelopmentRun`

### Manual Verification
- Vérifier le rendu des nouveaux composants Shimmer sur l'accueil.
- Tester la navigation après refactorisation de `HomeScreen`.
