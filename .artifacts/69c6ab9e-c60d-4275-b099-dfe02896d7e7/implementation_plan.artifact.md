# Amélioration de la compatibilité pour l'installation de l'APK

Ce plan vise à résoudre l'erreur "Application non installée" en augmentant la compatibilité de l'APK avec une plus large gamme de téléphones Android et en s'assurant que la signature et les architectures sont correctes.

## Changements Proposés

### [Component] Configuration Gradle (Android)

L'objectif est de réduire les restrictions techniques qui empêchent l'installation sur les vieux modèles et de renforcer la signature du package.

#### [MODIFY] [build.gradle.kts](file:///D:/aaaaaaaaa/workspace/Tik-Market/composeApp/build.gradle.kts)
- Baisser `minSdk` de `23` à `21` (Android 5.0).
- Activer `multiDexEnabled` pour éviter les erreurs de limite de méthodes.
- Ajouter le support du **desugaring** (pour utiliser les fonctionnalités Java récentes sur les vieux Android).
- Configurer explicitement la signature `debug` pour utiliser V1 et V2 (souvent requis par certains constructeurs comme Xiaomi/Oppo).
- Optimiser la gestion des ressources natives.

## Étapes de Vérification

### Manuel
1. **Nettoyage du projet** : Effectuer un `Clean Project` puis `Rebuild Project`.
2. **Désinstallation préalable** : Demander à l'utilisateur de désinstaller toute version précédente de l'application sur les téléphones de test avant de réessayer.
3. **Installation manuelle** : Vérifier sur un téléphone avec une version d'Android ancienne (ex: Android 7 ou 8) et un récent (Android 13+).

## Recommandations pour l'utilisateur
> [!IMPORTANT]
> L'erreur "Application non installée" est très souvent due à un **conflit de signature**. Si vous avez déjà installé une version de l'app (même une ancienne), vous **devez la désinstaller** avant d'installer la nouvelle APK.
