# TiK-Market - Progression KMP

## Goal
Déployer l'infrastructure complète et développer les fonctionnalités métier : API PHP sur Render, base Aiven, web WasmJS sur Vercel, médias Cloudinary, APK Android, avec flux commandes à deux modes de paiement (livraison ou direct vendeur), chat WhatsApp-like, et messages vocaux persistants.

## Constraints & Preferences
- Kotlin Multiplatform + Compose Multiplatform, cibles wasmJs et Android APK
- JAVA_HOME = `C:\Program Files\Java\jdk-23`
- Compilation via `java -jar gradle/wrapper/gradle-wrapper.jar`
- PowerShell cmdlets cassés → `cmd.exe /c` systématiquement préféré
- PHP 8.2.32 sur Render (prod), PHP 8.2.12 ZTS via XAMPP (local)
- Git 2.45.2 (safe.directory nécessaire)
- Vercel CLI 54.20.1, Node.js v20.16.0
- Aiven MySQL 8.0 en ANSI_QUOTES mode
- BaseUrl API : `https://tik-market.onrender.com`
- URL Web : `https://tik-market-app.vercel.app`

## Done
### Infrastructure
- **API Render opérationnelle** – Login, produits, boutiques, commandes fonctionnent
- **Aiven connecté avec SSL** – Host `mysql-32d32cc7-tik-market.k.aivencloud.com:10180`
- **SQL ANSI_QUOTES corrigé** – Tous les identifiants SQL utilisent `'`
- **Cloudinary configuré** – `cloud_name=***REDACTED***`, `api_key=***REDACTED***`, `api_secret=***REDACTED***`
- **APK Android buildé** – `TiK-Market_v1.0.6.apk` (URL API : `https://tik-market.onrender.com`)
- **Web Universel (JS IR) buildé et déployé** – `https://tik-market-app.vercel.app` (Compatibilité maximale).
- **GitHub Actions workflow optimisé** – Déploiement automatique fonctionnel en mode compatibilité.
- **Design Parity Web/APK** – Splash screen (blanc), logo TiK-Market, favicon et icônes Apple synchronisés.
- **Support Navigateurs Anciens** – Passage de Wasm à JS IR pour inclure les vieux téléphones et navigateurs.
- **Feature Addition (In Progress)** – Ajout d'un bouton de support WhatsApp flottant.

### Chat / Messages
- **Messages vocaux** – Upload Cloudinary permanent + ajout optimiste (émetteur voit son message immédiatement)
- **Polling 5s → 1s** – Réception quasi-instantanée
- **Chat WhatsApp-like** – Barre #075E54, bubbles vert/blanc, avatars, waveform vocale, produit partagé, emoji picker, +menu, double check

### Nouveau flux commandes (deux modes)
- **orders.php** – POST avec `payment_type`, PUT validate_payment/confirm_delivery, DELETE annuler
- **ApiCreateOrderBody** – Ajout `paymentType`
- **ApiOrder, ApiVendorInfo** – Modèles avec `paymentType`, `vendorInfo`
- **CheckoutScreen** – Deux options paiement (radios), infos vendeur + bouton copier
- **App.kt** – `onPlaceOrder` avec `paymentType`, Snackbar selon mode
- **OrdersScreen** – Badge Virement/Livraison, infos vendeur pour direct, bouton annuler, timeline suivi, date livraison estimée

### Corrections
- `System.currentTimeMillis` → Random (WasmJs compat)
- `@Serializable` dupliqué sur `ApiVendorInfo` supprimé
- `deleteOrder(orderId)` ajouté à ApiClient

### Documents
- `guide-utilisateur.html` – Guide 16 sections
- `cahier-de-charge.docx` – Cahier de charge (88,6 Ko)

## Blocked
- **Secret `VERCEL_TOKEN` non ajouté** – Token doit être ajouté comme secret GitHub

## Key Decisions
- Render remplace InfinityFree
- Aiven sans SSL (fallback dans `getDB()`)
- ANSI_QUOTES : `'` pour chaînes, `"` pour identifiants
- Messages vocaux sur Cloudinary (fallback local éphémère)
- Ajout optimiste des messages vocaux
- Chat style WhatsApp (#075E54)
- Deux modes paiement : `delivery` (livraison) et `direct` (virement vendeur)
- `payment_type` ajouté via ALTER TABLE automatique

## Next Steps
1. Vérifier le rendu final sur https://tik-market-app.vercel.app après la fin du build.
2. Tester le flux complet de virement vendeur (Validation par le vendeur).
3. (Optionnel) Play Store
