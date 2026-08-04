<?php
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$data = json_decode(file_get_contents('php://input'), true);
if (!$data) json(400, ['error' => 'Corps de requête invalide']);

$points = (int)($data['points'] ?? 0);

if ($points <= 0) json(400, ['error' => 'Points invalides']);

try {
    $db = getDB();
    $userId = getAuthUserId();

    $stmt = $db->prepare('SELECT id, current_points FROM wallets WHERE user_id = ?');
    $stmt->execute([$userId]);
    $wallet = $stmt->fetch();

    if (!$wallet) {
        json(400, ['error' => 'Portefeuille introuvable']);
    }

    if ((int)$wallet['current_points'] < $points) {
        json(400, ['error' => 'Points insuffisants']);
    }

    $discountFcfa = (int)floor($points / 100) * 500;
    if ($discountFcfa <= 0) {
        json(400, ['error' => 'Minimum 100 points requis']);
    }

    $chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    $random = '';
    for ($i = 0; $i < 8; $i++) {
        $random .= $chars[random_int(0, strlen($chars) - 1)];
    }
    $code = 'FID-' . $random;

    $expiresAt = date('Y-m-d H:i:s', strtotime('+30 days'));

    $db->beginTransaction();

    $stmt = $db->prepare('
        INSERT INTO coupons (user_id, code, discount_fcfa, points_cost, expires_at)
        VALUES (?, ?, ?, ?, ?)
    ');
    $stmt->execute([$userId, $code, $discountFcfa, $points, $expiresAt]);

    $stmt = $db->prepare('UPDATE wallets SET current_points = current_points - ? WHERE id = ?');
    $stmt->execute([$points, $wallet['id']]);

    $stmt = $db->prepare('
        INSERT INTO wallet_transactions (wallet_id, type, amount_fcfa, points, description)
        VALUES (?, ?, ?, ?, ?)
    ');
    $stmt->execute([
        $wallet['id'], 'spend', 0, $points,
        "Échange de {$points} points contre coupon {$code} ({$discountFcfa} FCFA)"
    ]);

    $db->commit();

    json(201, [
        'success' => true,
        'coupon' => [
            'code' => $code,
            'discount_fcfa' => $discountFcfa,
            'expires_at' => $expiresAt
        ]
    ]);
} catch (PDOException $e) {
    if ($db->inTransaction()) $db->rollBack();
    json(500, ['error' => 'Erreur serveur']);
}
