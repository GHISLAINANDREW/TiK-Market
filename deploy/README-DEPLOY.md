# TiK-Market — Migration vers Oracle Cloud (Guide de déploiement)

Ce guide explique comment migrer le backend TiK-Market de Render vers Oracle Cloud,
avec LiveKit (appels + lives en temps réel) et le stockage vidéo FFmpeg auto-hébergé.

**Architecture finale :**
- **Frontend** : Vercel (inchangé)
- **Backend** : Oracle Cloud VM (PHP + Nginx + MariaDB)
- **Temps réel** : LiveKit auto-hébergé sur la même VM
- **Vidéos** : FFmpeg (2 versions HD/SD) stockées localement sur la VM

---

## Étape 1 — Créer la VM Oracle Cloud

1. Connectez-vous à [Oracle Cloud Console](https://cloud.oracle.com).
2. **Région** : choisissez **Afrique du Sud (Johannesburg)** — latence optimale pour le Cameroun (~50-80ms).
3. **Compute → Instances → Create instance** :
   - Image : **Ubuntu 22.04** (ou 24.04)
   - Shape : **ARM** (Ampere A1) — 4 OCPU / 24 GB RAM (gratuit dans le Always Free tier)
   - SSH : ajoutez votre clé publique SSH
4. **Réseau** : créez/choisissez un VCN. Notez l'**IP publique** de la VM.
5. **Ouvrir les ports** dans la Security List (Networking → VCN → Security Lists → Ingress Rules) :
   - TCP : 22, 80, 443, 7880, 7881
   - UDP : 50000-60000

---

## Étape 2 — Configurer DuckDNS (gratuit)

1. Créez un compte sur [duckdns.org](https://www.duckdns.org).
2. Créez **deux sous-domaines** :
   - `tik-market.duckdns.org` → backend PHP
   - `live.tik-market.duckdns.org` → LiveKit WSS
3. Notez le **token** DuckDNS pour chaque sous-domaine (ou un token par domaine).
4. Pointez les deux sous-domaines vers l'**IP publique** de la VM.

---

## Étape 3 — Provisionner la VM

SSH dans la VM, puis exécutez le script de provisioning :

```bash
ssh ubuntu@<PUBLIC_IP>
```

**Avant d'exécuter**, éditez `deploy/provision_oracle.sh` et remplissez la section CONFIGURATION :
- `GITHUB_REPO`, `GITHUB_BRANCH`
- `DUCK_SUBDOMAIN`, `DUCK_LIVE_SUBDOMAIN`, `DUCK_TOKEN`
- `DB_PASS`, `JWT_SECRET`
- `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`

Puis copiez le script sur la VM et exécutez-le :

```bash
# Depuis votre machine locale
scp deploy/provision_oracle.sh ubuntu@<PUBLIC_IP>:~/

# Sur la VM
sudo bash ~/provision_oracle.sh
```

Le script installe tout (Nginx, PHP 8.2, MariaDB, FFmpeg, LiveKit, Certbot),
clone le repo, importe la base de données, configure Nginx et les variables d'environnement.

---

## Étape 4 — Certificats HTTPS (Let's Encrypt)

```bash
# Sur la VM
sudo certbot --nginx -d tik-market.duckdns.org
sudo certbot --nginx -d live.tik-market.duckdns.org
```

Certbot configure automatiquement le HTTPS et le renouvellement.

---

## Étape 5 — Vérifier le backend

```bash
curl https://tik-market.duckdns.org/ping.php
# → {"status":"ok",...}

curl https://live.tik-market.duckdns.org/
# → JSON d'infos LiveKit (confirme que le reverse proxy fonctionne)
```

---

## Étape 6 — Configurer les secrets GitHub Actions

Pour l'auto-déploiement, ajoutez ces secrets dans **GitHub → Settings → Secrets and variables → Actions** :

| Secret | Valeur |
|--------|--------|
| `ORACLE_HOST` | IP publique ou `tik-market.duckdns.org` |
| `ORACLE_USER` | `ubuntu` (ou `opc`) |
| `ORACLE_SSH_KEY` | Clé privée SSH (PEM) |
| `ORACLE_PORT` | `22` |
| `ORACLE_APP_DIR` | `/var/www/tik-market` |

Le workflow `.github/workflows/deploy-oracle.yml` se déclenche automatiquement
à chaque push sur `main` touchant `api/**`.

---

## Étape 7 — Mettre à jour le frontend (Vercel)

Le frontend Vercel doit pointer vers le nouveau backend. Mettez à jour la variable
d'environnement `API_BASE_URL` (ou équivalent) dans Vercel :

```
https://tik-market.duckdns.org
```

---

## Étape 8 — Mettre à jour l'app Android

L'app Android utilise `ApiClient` avec une URL de base. Mettez à jour cette URL
vers `https://tik-market.duckdns.org` et reconstruisez l'APK.

---

## Dépannage

### Le backend renvoie une erreur 500
- Vérifiez les logs : `sudo tail -f /var/log/nginx/error.log`
- Vérifiez PHP-FPM : `sudo systemctl status php8.2-fpm`
- Vérifiez les variables d'env : `sudo -u www-data php -r 'var_dump(getenv("DB_HOST"));'`

### LiveKit ne se connecte pas
- Vérifiez que les ports 7880/7881/50000-60000 sont ouverts dans la Security List OCI **et** dans ufw.
- Vérifiez que `LIVEKIT_API_KEY`/`LIVEKIT_API_SECRET` dans `livekit.yaml` correspondent à ceux du backend.
- Vérifiez le reverse proxy : `curl https://live.tik-market.duckdns.org/`

### Les vidéos ne sont pas optimisées
- Vérifiez FFmpeg : `ffmpeg -version`
- Vérifiez que le dossier `/var/www/tik-market/api/uploads/videos` est accessible en écriture par `www-data`.

### Le déploiement GitHub Actions échoue
- Vérifiez que la clé SSH (`ORACLE_SSH_KEY`) est correcte et que le user a les droits.
- Vérifiez que le repo est bien cloné sur la VM dans `ORACLE_APP_DIR`.