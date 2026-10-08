#!/usr/bin/env bash
# =============================================================================
# TiK-Market — DuckDNS dynamic DNS updater
# =============================================================================
# Keeps your DuckDNS subdomain(s) pointed at this VM's public IP.
# DuckDNS subdomains are free and don't require a static IP.
#
# Setup:
#   1. Create your subdomain(s) at https://www.duckdns.org (free account).
#      You get a "token" for each subdomain.
#   2. Edit the DUCK_TOKEN and SUBDOMAINS below.
#   3. Run this script once to test, then install it as a cron job:
#        crontab -e
#        */5 * * * * /var/www/tik-market/deploy/duckdns-update.sh
# =============================================================================

# ── CONFIGURATION ──
DUCK_TOKEN="REPLACE_WITH_YOUR_DUCK_DNS_TOKEN"
# Space-separated list of subdomains (without .duckdns.org)
SUBDOMAINS="tik-market live"

# ── Do not edit below this line ──
for SUB in $SUBDOMAINS; do
    echo "Updating $SUB.duckdns.org ..."
    curl -s "https://www.duckdns.org/update?domains=${SUB}&token=${DUCK_TOKEN}&ip="
    echo ""
done

echo "DuckDNS update complete at $(date)"