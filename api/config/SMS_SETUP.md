# Configuration SMS — Africa's Talking

## Pour activer l'envoi réel de SMS OTP

### 1. Créez un compte Africa's Talking

1. Allez sur https://africastalking.com
2. Cliquez sur **"Get Started"** ou **"Sign Up"**
3. Créez un compte gratuit (email + mot de passe)
4. Confirmez votre email

### 2. Récupérez vos identifiants

1. Connectez-vous au [dashboard](https://account.africastalking.com)
2. Allez dans **Sandbox** > **SMS**
3. Notez votre **API Key** (clé secrète)
4. Votre **Username** est généralement `sandbox` en mode test

### 3. Configurez le backend PHP

Modifiez le fichier `api/config/sms.php` ou définissez des variables d'environnement :

```bash
# Option A : Variables d'environnement (recommandé)
AT_API_KEY=votre_cle_api_ici
AT_USERNAME=sandbox
AT_MODE=sandbox

# Option B : Modifier directement sms.php
# define('AT_API_KEY', 'votre_cle_api_ici');
```

### 4. Ajoutez vos numéros de test (sandbox)

En mode sandbox, seuls les numéros enregistrés dans le dashboard peuvent recevoir des SMS :
1. Dashboard > Sandbox > SMS
2. Ajoutez votre numéro Cameroun (+2376XXXXXXXX)
3. Vous pouvez en tester jusqu'à 10.

### 5. Testez

1. Lancez l'application
2. Choisissez **"Téléphone (Cameroun)"**
3. Saisissez un numéro enregistré dans votre sandbox (+2376XXXXXXXX)
4. Cliquez sur "Envoyer le code"
5. Vous recevrez le vrai SMS avec le code OTP

### 6. Passage en production

1. Dashboard > Settings > votre compte
2. Demandez l'activation du compte production (pièces d'identité nécessaires)
3. Achetez du crédit SMS (recharge)
4. Modifiez `AT_MODE` à `production`
5. Changez `AT_USERNAME` pour votre username de production

## En mode démo (sans configuration)

Si vous ne configurez pas Africa's Talking, l'application fonctionne quand même en mode démo :
- Le code OTP est écrit dans les **logs PHP** (`php_server.log`)
- Ouvrez le fichier `api/php_server.log` pour voir le code quand vous cliquez "Envoyer"
- Tapez ce code dans l'application pour valider

## Fichiers modifiés dans cette feature

| Fichier | Rôle |
|---------|------|
| `api/config/sms.php` | Configuration Africa's Talking + fonction d'envoi |
| `api/auth/send-otp.php` | Endpoint : envoie le code OTP par SMS |
| `api/auth/verify-otp.php` | Endpoint : vérifie le code et connecte l'utilisateur |
| `api/database.sql` | Nouvelle table `otp_codes` |
| `composeApp/.../Platform.kt` | `expect fun currentTimeMillis()` |
| `composeApp/.../Platform.android.kt` | `actual fun currentTimeMillis()` (Android) |
| `composeApp/.../Platform.wasm.kt` | `actual fun currentTimeMillis()` (WasmJs) |
| `composeApp/.../ApiModels.kt` | Nouveaux DTOs OTP |
| `composeApp/.../ApiClient.kt` | Nouvelles méthodes `sendOtp()` et `verifyOtp()` |
| `composeApp/.../AuthScreen.kt` | Flux OTP réel avec compte à rebours + renvoi |
