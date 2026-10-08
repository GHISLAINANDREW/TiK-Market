#!/usr/bin/env bash
# =============================================================================
# TiK-Market — Oracle Cloud VM provisioning script (v2)
# =============================================================================
# Run this ONCE on a fresh Oracle Cloud Ubuntu VM (ARM, 4 OCPU / 24GB RAM,
# region: South Africa / Johannesburg) to install and configure everything:
#   - Nginx (web server)
#   - PHP 8.2 + FPM (backend)
#   - MariaDB (database)
#   - FFmpeg (video optimization)
#   - LiveKit (real-time calls + live streaming)
#   - Certbot / Let's Encrypt (HTTPS)
#   - Clones the TiK-Market repo
#   - Imports the database
#   - Configures Nginx sites
#
# Usage:
#   sudo bash provision_oracle.sh
#
# BEFORE running, edit the CONFIG section below:
#   - GITHUB_REPO  : your GitHub repo URL
#   - DUCK_SUBDOMAIN / DUCK_LIVE_SUBDOMAIN : your DuckDNS names
#   - DB_PASS      : a strong DB password
#   - JWT_SECRET   : a long random string
#   - LIVEKIT_API_KEY / LIVEKIT_API_SECRET
#   - DUCK_TOKEN   : your DuckDNS token (for the update script)
# =============================================================================
set -e

# ═══════════════════════════════════════════════════════════════════════════
# CONFIGURATION — EDIT THESE
# ═══════════════════════════════════════════════════════════════════════════
GITHUB_REPO="https://github.com/GHISLAINANDREW/TiK-Market.git"
GITHUB_BRANCH="main"
APP_DIR="/var/www/tik-market"

DUCK_SUBDOMAIN="tik-market"          # backend:  tik-market.duckdns.org
DUCK_LIVE_SUBDOMAIN="live"           # livekit:  live.tik-market.duckdns.org
DUCK_TOKEN="REPLACE_WITH_YOUR_DUCK_DNS_TOKEN"

DB_NAME="tik_market"
DB_USER="tik_market"
DB_PASS="CHANGE_ME_STRONG_PASSWORD"

JWT_SECRET="CHANGE_ME_RANDOM_HEX_64_CHARS"

LIVEKIT_API_KEY="devkey"
LIVEKIT_API_SECRET="devsecret"

# ═══════════════════════════════════════════════════════════════════════════
echo "=== TiK-Market Oracle provisioning (v2) ==="

# ── 1. System update ──
echo "=== [1/10] System update ==="
sudo apt-get update -y
sudo apt-get upgrade -y

# ── 2. Install Nginx ──
echo "=== [2/10] Installing Nginx ==="
sudo apt-get install -y nginx

# ── 3. Install PHP 8.2 + FPM + extensions ──
echo "=== [3/10] Installing PHP 8.2 + FPM ==="
sudo apt-get install -y software-properties-common
sudo add-apt-repository -y ppa:ondrej/php
sudo apt-get update -y
sudo apt-get install -y php8.2-fpm php8.2-mysql php8.2-curl php8.2-gd \
    php8.2-mbstring php8.2-xml php8.2-zip php8.2-intl php8.2-bcmath

# ── 4. Install MariaDB ──
echo "=== [4/10] Installing MariaDB ==="
sudo apt-get install -y mariadb-server

# ── 5. Install FFmpeg ──
echo "=== [5/10] Installing FFmpeg ==="
sudo apt-get install -y ffmpeg

# ── 6. Install Certbot ──
echo "=== [6/10] Installing Certbot ==="
sudo apt-get install -y certbot python3-certbot-nginx

# ── 7. Install LiveKit ──
echo "=== [7/10] Installing LiveKit ==="
LIVEKIT_VERSION="2.8.0"
ARCH="$(uname -m)"
case "$ARCH" in
  aarch64|arm64) LIVEKIT_ARCH="arm64" ;;
  x86_64|amd64)  LIVEKIT_ARCH="amd64" ;;
  *) echo "Unsupported arch: $ARCH"; LIVEKIT_ARCH="amd64" ;;
esac

sudo mkdir -p /opt/livekit
cd /tmp
curl -sLO "https://github.com/livekit/livekit/releases/download/v${LIVEKIT_VERSION}/livekit_${LIVEKIT_VERSION}_linux_${LIVEKIT_ARCH}.tar.gz"
tar -xzf "livekit_${LIVEKIT_VERSION}_linux_${LIVEKIT_ARCH}.tar.gz"
sudo mv livekit /opt/livekit/livekit
sudo chmod +x /opt/livekit/livekit

# LiveKit config file.
sudo tee /opt/livekit/livekit.yaml > /dev/null <<EOF
port: 7880
rtc:
  tcp_port: 7881
  port_range_start: 50000
  port_range_end: 60000
  use_external_ip: true
keys:
  ${LIVEKIT_API_KEY}: ${LIVEKIT_API_SECRET}
logging:
  level: info
EOF

# LiveKit systemd service.
sudo tee /etc/systemd/system/livekit.service > /dev/null <<'EOF'
[Unit]
Description=LiveKit Server
After=network.target

[Service]
ExecStart=/opt/livekit/livekit --config /opt/livekit/livekit.yaml
Restart=always
RestartSec=5
User=root

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable livekit
sudo systemctl start livekit

# ── 8. Clone the repo ──
echo "=== [8/10] Cloning TiK-Market repo ==="
sudo mkdir -p "$APP_DIR"
sudo chown -R "$USER":"$USER" "$APP_DIR"
cd "$APP_DIR"
if [ ! -d .git ]; then
    git clone -b "$GITHUB_BRANCH" "$GITHUB_REPO" .
else
    git fetch --all
    git reset --hard origin/"$GITHUB_BRANCH"
fi

# Ensure uploads dirs exist and are writable.
sudo mkdir -p "$APP_DIR/api/uploads/videos" "$APP_DIR/api/uploads/stories" "$APP_DIR/api/uploads/voices"
sudo chmod -R 775 "$APP_DIR/api/uploads"
sudo chown -R www-data:www-data "$APP_DIR/api/uploads"

# ── 9. Configure database ──
echo "=== [9/10] Configuring MariaDB ==="
sudo systemctl enable mariadb
sudo systemctl start mariadb

# Create DB + user.
sudo mysql <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
SQL

# Import schema + data.
if [ -f "$APP_DIR/api/database-tik-market.sql" ]; then
    echo "Importing database-tik-market.sql ..."
    sudo mysql "$DB_NAME" < "$APP_DIR/api/database-tik-market.sql"
elif [ -f "$APP_DIR/api/database.sql" ]; then
    echo "Importing database.sql ..."
    sudo mysql "$DB_NAME" < "$APP_DIR/api/database.sql"
else
    echo "WARNING: No database SQL file found. Import manually."
fi

# ── 10. Configure Nginx + env vars + DuckDNS ──
echo "=== [10/10] Configuring Nginx, env vars, DuckDNS ==="

# Copy the Nginx site config.
sudo cp "$APP_DIR/deploy/nginx-tik-market.conf" /etc/nginx/sites-available/tik-market
sudo ln -sf /etc/nginx/sites-available/tik-market /etc/nginx/sites-enabled/tik-market
sudo rm -f /etc/nginx/sites-enabled/default

# Inject env vars into PHP-FPM pool config.
PHP_POOL="/etc/php/8.2/fpm/pool.d/www.conf"
sudo tee -a "$PHP_POOL" > /dev/null <<EOF

; ── TiK-Market env vars ──
env[DB_HOST] = 127.0.0.1
env[DB_PORT] = 3306
env[DB_NAME] = ${DB_NAME}
env[DB_USER] = ${DB_USER}
env[DB_PASS] = ${DB_PASS}
env[JWT_SECRET] = ${JWT_SECRET}
env[APP_URL] = https://${DUCK_SUBDOMAIN}.duckdns.org
env[LIVEKIT_API_KEY] = ${LIVEKIT_API_KEY}
env[LIVEKIT_API_SECRET] = ${LIVEKIT_API_SECRET}
env[LIVEKIT_URL] = wss://${DUCK_LIVE_SUBDOMAIN}.duckdns.org
EOF

# Copy the DuckDNS update script and set the token.
sudo cp "$APP_DIR/deploy/duckdns-update.sh" /usr/local/bin/duckdns-update.sh
sudo sed -i "s/REPLACE_WITH_YOUR_DUCK_DNS_TOKEN/${DUCK_TOKEN}/" /usr/local/bin/duckdns-update.sh
sudo chmod +x /usr/local/bin/duckdns-update.sh

# Test Nginx config.
sudo nginx -t

# Reload services.
sudo systemctl reload nginx
sudo systemctl restart php8.2-fpm

# ── Open firewall ports ──
echo "=== Opening firewall ports ==="
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 7880/tcp
sudo ufw allow 7881/tcp
sudo ufw allow 50000:60000/udp
sudo ufw --force enable

echo ""
echo "============================================================"
echo "=== PROVISIONING COMPLETE ==="
echo "============================================================"
echo ""
echo "NEXT STEPS (manual):"
echo "  1. Point your DuckDNS subdomains at this VM's public IP:"
echo "       ${DUCK_SUBDOMAIN}.duckdns.org  -> <PUBLIC_IP>"
echo "       ${DUCK_LIVE_SUBDOMAIN}.duckdns.org -> <PUBLIC_IP>"
echo "     Or run: sudo /usr/local/bin/duckdns-update.sh"
echo ""
echo "  2. Issue Let's Encrypt certs:"
echo "       sudo certbot --nginx -d ${DUCK_SUBDOMAIN}.duckdns.org"
echo "       sudo certbot --nginx -d ${DUCK_LIVE_SUBDOMAIN}.duckdns.org"
echo ""
echo "  3. Open these ports in the OCI Security List"
echo "     (Networking > VCN > Security Lists > Ingress Rules):"
echo "       TCP 22, 80, 443, 7880, 7881"
echo "       UDP 50000-60000"
echo ""
echo "  4. Set GitHub Actions secrets for auto-deploy:"
echo "       ORACLE_HOST     = <PUBLIC_IP or ${DUCK_SUBDOMAIN}.duckdns.org>"
echo "       ORACLE_USER     = ubuntu (or opc)"
echo "       ORACLE_SSH_KEY  = <private SSH key PEM>"
echo "       ORACLE_PORT     = 22"
echo "       ORACLE_APP_DIR  = ${APP_DIR}"
echo ""
echo "  5. Verify the backend:"
echo "       curl https://${DUCK_SUBDOMAIN}.duckdns.org/ping.php"
echo ""
echo "  6. Verify LiveKit:"
echo "       curl https://${DUCK_LIVE_SUBDOMAIN}.duckdns.org/"
echo "     (should return LiveKit server info JSON)"
echo "============================================================"