<?php
/**
 * Migration: Configure le système de fidélité (tables et paliers par défaut)
 *
 * Usage: GET /api/migrations/setup_loyalty.php
 */
require_once __DIR__ . '/../config/database.php';

$db = getDB();
$migrations = [];

// 1. Créer la table loyalty_tiers
try {
    $db->exec("CREATE TABLE IF NOT EXISTS loyalty_tiers (
        id INT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(20) NOT NULL UNIQUE,
        min_points INT NOT NULL DEFAULT 0,
        cashback_pct DECIMAL(5,2) NOT NULL DEFAULT 0,
        bonus_pct DECIMAL(5,2) NOT NULL DEFAULT 0,
        color VARCHAR(10) DEFAULT '#8D6E63',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    $migrations[] = "✅ Table `loyalty_tiers` vérifiée";
} catch (PDOException $e) { $migrations[] = "❌ Erreur `loyalty_tiers`: " . $e->getMessage(); }

// 2. Insérer les paliers par défaut
try {
    $check = $db->query("SELECT COUNT(*) FROM loyalty_tiers")->fetchColumn();
    if ((int)$check === 0) {
        $db->exec("INSERT INTO loyalty_tiers (name, min_points, cashback_pct, bonus_pct, color) VALUES
            ('bronze', 0, 1.0, 0, '#8D6E63'),
            ('argent', 100, 2.0, 5, '#9E9E9E'),
            ('or', 500, 3.0, 10, '#FFD700')");
        $migrations[] = "✅ Paliers de fidélité insérés";
    } else {
        $migrations[] = "ℹ️ Paliers de fidélité déjà présents";
    }
} catch (PDOException $e) { $migrations[] = "❌ Erreur insertion paliers: " . $e->getMessage(); }

// 3. Créer la table wallets
try {
    $db->exec("CREATE TABLE IF NOT EXISTS wallets (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL UNIQUE,
        balance DECIMAL(12,0) NOT NULL DEFAULT 0,
        total_points INT NOT NULL DEFAULT 0,
        current_points INT NOT NULL DEFAULT 0,
        tier VARCHAR(20) NOT NULL DEFAULT 'bronze',
        lifetime_spent DECIMAL(12,0) NOT NULL DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    $migrations[] = "✅ Table `wallets` vérifiée";
} catch (PDOException $e) { $migrations[] = "❌ Erreur `wallets`: " . $e->getMessage(); }

// 4. Créer les wallets manquants pour les utilisateurs existants
try {
    $db->exec("INSERT IGNORE INTO wallets (user_id) SELECT id FROM users");
    $migrations[] = "✅ Portefeuilles créés pour les utilisateurs existants";
} catch (PDOException $e) { $migrations[] = "❌ Erreur init wallets: " . $e->getMessage(); }

// 5. Créer la table wallet_transactions
try {
    $db->exec("CREATE TABLE IF NOT EXISTS wallet_transactions (
        id INT AUTO_INCREMENT PRIMARY KEY,
        wallet_id INT NOT NULL,
        type VARCHAR(20) NOT NULL DEFAULT 'earn',
        amount_fcfa DECIMAL(12,0) NOT NULL DEFAULT 0,
        points INT NOT NULL DEFAULT 0,
        description TEXT,
        reference_type VARCHAR(20) DEFAULT NULL,
        reference_id INT DEFAULT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    $migrations[] = "✅ Table `wallet_transactions` vérifiée";
} catch (PDOException $e) { $migrations[] = "❌ Erreur `wallet_transactions`: " . $e->getMessage(); }

echo json_encode(['success' => true, 'migrations' => $migrations]);
