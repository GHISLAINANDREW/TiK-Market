# Dschang Market - Progression KMP

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
- **Cloudinary configuré** – `cloud_name=***REMOVED***`, `api_key=***REMOVED***`, `api_secret=***REMOVED***`
- **APK Android buildé** – `composeApp/build/outputs/apk/debug/composeApp-debug.apk`
- **Web WasmJS buildé et déployé** – `https://dschang-marke.vercel.app`
- **GitHub Actions workflow créé** – `.github/workflows/deploy-web.yml`

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
- **Secret `VERCEL_TOKEN` non ajouté** – Token `***REMOVED******` doit être ajouté comme secret GitHub

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
1. Ajouter le secret `VERCEL_TOKEN` GitHub
2. Pousser tout sur GitHub, tester le flux complet
3. Tester la validation vendeur (orders.php PUT)
4. (Optionnel) Domaine personnalisé ~2-3€/an
5. (Optionnel) Play Store
