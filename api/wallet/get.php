<?php
require_once __DIR__ . '/../../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') json(405, ['error' => 'Méthode non autorisée']);

try {
    $db = getDB();
    $userId = getAuthUserId();

    // ── Auto-migration: ensure wallets & loyalty_tiers tables exist ──
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS loyalty_tiers (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(20) NOT NULL UNIQUE,
            min_points INT NOT NULL DEFAULT 0,
            cashback_pct DECIMAL(5,2) NOT NULL DEFAULT 0,
            bonus_pct DECIMAL(5,2) NOT NULL DEFAULT 0,
            color VARCHAR(10) DEFAULT '#8D6E63',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )");
        // Seed default tiers if empty
        $check = $db->query("SELECT COUNT(*) FROM loyalty_tiers")->fetchColumn();
        if ((int)$check === 0) {
            $db->exec("INSERT INTO loyalty_tiers (name, min_points, cashback_pct, bonus_pct, color) VALUES
                ('bronze', 0, 1.0, 0, '#8D6E63'),
                ('argent', 100, 2.0, 5, '#9E9E9E'),
                ('or', 500, 3.0, 10, '#FFD700')");
        }
    } catch (Exception $e) { error_log("Migration loyalty_tiers: " . $e->getMessage()); }

    try {
        $db->exec("CREATE TABLE IF NOT EXISTS wallets (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL UNIQUE,
            balance INT NOT NULL DEFAULT 0,
            total_points INT NOT NULL DEFAULT 0,
            current_points INT NOT NULL DEFAULT 0,
            tier VARCHAR(20) NOT NULL DEFAULT 'bronze',
            lifetime_spent INT NOT NULL DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )");
    } catch (Exception $e) { error_log("Migration wallets: " . $e->getMessage()); }

    try {
        $db->exec("CREATE TABLE IF NOT EXISTS wallet_transactions (
            id INT AUTO_INCREMENT PRIMARY KEY,
            wallet_id INT NOT NULL,
            type VARCHAR(20) NOT NULL DEFAULT 'earn',
            amount_fcfa INT NOT NULL DEFAULT 0,
            points INT NOT NULL DEFAULT 0,
            description TEXT,
            reference_type VARCHAR(20) DEFAULT NULL,
            reference_id INT DEFAULT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
        )");
    } catch (Exception $e) { error_log("Migration wallet_transactions: " . $e->getMessage()); }

    $stmt = $db->prepare('
        SELECT w.*, lt.name as tier_name, lt.cashback_pct, lt.bonus_pct, lt.color as tier_color
        FROM wallets w
        LEFT JOIN loyalty_tiers lt ON w.tier = lt.name
        WHERE w.user_id = ?
    ');
    $stmt->execute([$userId]);
    $wallet = $stmt->fetch();

    if (!$wallet) {
        $stmt = $db->prepare('INSERT IGNORE INTO wallets (user_id) VALUES (?)');
        $stmt->execute([$userId]);

        $stmt = $db->prepare('
            SELECT w.*, lt.name as tier_name, lt.cashback_pct, lt.bonus_pct, lt.color as tier_color
            FROM wallets w
            LEFT JOIN loyalty_tiers lt ON w.tier = lt.name
            WHERE w.user_id = ?
        ');
        $stmt->execute([$userId]);
        $wallet = $stmt->fetch();
    }

    if (!$wallet) json(404, ['error' => 'Portefeuille introuvable']);

    $currentPoints = (int)($wallet['total_points'] ?? 0);
    $tiers = $db->query('SELECT name, min_points FROM loyalty_tiers ORDER BY min_points ASC')->fetchAll();
    $nextTier = null;

    $userTier = $wallet['tier'] ?? 'bronze';
    for ($i = 0; $i < count($tiers); $i++) {
        if ($tiers[$i]['name'] === $userTier && isset($tiers[$i + 1])) {
            $nextTier = [
                'name' => $tiers[$i + 1]['name'],
                'points_needed' => max(0, (int)$tiers[$i + 1]['min_points'] - $currentPoints)
            ];
            break;
        }
    }

    json(200, [
        'success' => true,
        'wallet' => [
            'id' => (int)$wallet['id'],
            'user_id' => (int)$wallet['user_id'],
            'balance' => (float)$wallet['balance'],
            'total_points' => (int)$wallet['total_points'],
            'current_points' => (int)$wallet['current_points'],
            'tier' => $userTier,
            'tier_name' => $wallet['tier_name'] ?: ucfirst($userTier),
            'tier_color' => $wallet['tier_color'] ?: '#8D6E63',
            'cashback_pct' => (float)($wallet['cashback_pct'] ?: 1.0),
            'bonus_pct' => (float)($wallet['bonus_pct'] ?: 0.0),
            'lifetime_spent' => (float)$wallet['lifetime_spent'],
            'next_tier' => $nextTier,
            'created_at' => (string)($wallet['created_at'] ?: ''),
            'updated_at' => (string)($wallet['updated_at'] ?: ''),
        ]
    ]);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
