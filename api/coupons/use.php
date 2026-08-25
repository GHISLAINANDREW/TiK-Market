<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
if ($method !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$userId = getAuthUserId();
$data = json_decode(file_get_contents('php://input'), true);
if (!$data) json(400, ['success' => false, 'error' => 'Corps de requête invalide']);

$code = trim($data['code'] ?? '');
if ($code === '') json(400, ['success' => false, 'error' => 'Code requis']);

try {
    $db = getDB();

    $stmt = $db->prepare('
        SELECT id, code, discount_pct, discount_fcfa, min_amount, is_used, expires_at
        FROM coupons WHERE code = ? AND user_id = ?
    ');
    $stmt->execute([$code, $userId]);
    $coupon = $stmt->fetch();

    if (!$coupon) {
        json(400, ['success' => false, 'error' => 'Code promo invalide']);
    }

    if ($coupon['is_used']) {
        json(400, ['success' => false, 'error' => 'Ce code a déjà été utilisé']);
    }

    if ($coupon['expires_at'] !== null && $coupon['expires_at'] <= date('Y-m-d H:i:s')) {
        json(400, ['success' => false, 'error' => 'Ce code a expiré']);
    }

    $stmt = $db->prepare('UPDATE coupons SET is_used = 1 WHERE id = ?');
    $stmt->execute([$coupon['id']]);

    json(200, [
        'success' => true,
        'coupon' => [
            'id'           => (int)$coupon['id'],
            'code'         => $coupon['code'],
            'discount_pct' => $coupon['discount_pct'] !== null ? (float)$coupon['discount_pct'] : null,
            'discount_fcfa'=> $coupon['discount_fcfa'] !== null ? (int)$coupon['discount_fcfa'] : null,
            'min_amount'  => (int)$coupon['min_amount']
        ]
    ]);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['success' => false, 'error' => 'Une erreur interne est survenue']);
}
