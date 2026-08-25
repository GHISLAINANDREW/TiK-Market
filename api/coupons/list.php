<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
if ($method !== 'GET') json(405, ['error' => 'Méthode non autorisée']);

$userId = getAuthUserId();

try {
    $db = getDB();

    $stmt = $db->prepare('
        SELECT id, code, discount_pct, discount_fcfa, min_amount, points_cost,
               expires_at, is_used, created_at
        FROM coupons
        WHERE user_id = ? AND is_used = 0 AND (expires_at IS NULL OR expires_at > NOW())
        ORDER BY created_at DESC
    ');
    $stmt->execute([$userId]);
    $coupons = $stmt->fetchAll();

    foreach ($coupons as &$c) {
        $c['id'] = (int)$c['id'];
        $c['discount_pct'] = $c['discount_pct'] !== null ? (float)$c['discount_pct'] : null;
        $c['discount_fcfa'] = $c['discount_fcfa'] !== null ? (int)$c['discount_fcfa'] : null;
        $c['min_amount'] = (int)$c['min_amount'];
        $c['points_cost'] = (int)$c['points_cost'];
        $c['is_used'] = (bool)$c['is_used'];
    }
    unset($c);

    json(200, ['success' => true, 'coupons' => $coupons]);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['success' => false, 'error' => 'Une erreur interne est survenue']);
}
