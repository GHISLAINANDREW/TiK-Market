<?php
require_once __DIR__ . '/../../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') json(405, ['error' => 'Méthode non autorisée']);

try {
    $db = getDB();
    $userId = getAuthUserId();

    $stmt = $db->prepare('
        SELECT w.*, lt.name as tier_name, lt.cashback_pct, lt.bonus_pct, lt.color as tier_color
        FROM wallets w
        JOIN loyalty_tiers lt ON w.tier = lt.name
        WHERE w.user_id = ?
    ');
    $stmt->execute([$userId]);
    $wallet = $stmt->fetch();

    if (!$wallet) {
        $stmt = $db->prepare('INSERT INTO wallets (user_id) VALUES (?)');
        $stmt->execute([$userId]);
        $stmt = $db->prepare('
            SELECT w.*, lt.name as tier_name, lt.cashback_pct, lt.bonus_pct, lt.color as tier_color
            FROM wallets w
            JOIN loyalty_tiers lt ON w.tier = lt.name
            WHERE w.user_id = ?
        ');
        $stmt->execute([$userId]);
        $wallet = $stmt->fetch();
    }

    $currentPoints = (int)$wallet['total_points'];
    $tiers = $db->query('SELECT name, min_points FROM loyalty_tiers ORDER BY min_points ASC')->fetchAll();
    $nextTier = null;
    for ($i = 0; $i < count($tiers); $i++) {
        if ($tiers[$i]['name'] === $wallet['tier'] && isset($tiers[$i + 1])) {
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
            'balance' => (int)$wallet['balance'],
            'total_points' => (int)$wallet['total_points'],
            'current_points' => (int)$wallet['current_points'],
            'tier' => $wallet['tier'],
            'tier_name' => $wallet['tier_name'],
            'tier_color' => $wallet['tier_color'],
            'cashback_pct' => (float)$wallet['cashback_pct'],
            'bonus_pct' => (float)$wallet['bonus_pct'],
            'lifetime_spent' => (int)$wallet['lifetime_spent'],
            'created_at' => $wallet['created_at'],
            'updated_at' => $wallet['updated_at'],
        ],
        'next_tier' => $nextTier
    ]);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
