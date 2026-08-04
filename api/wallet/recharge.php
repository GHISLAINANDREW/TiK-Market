<?php
require_once __DIR__ . '/../../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$data = json_decode(file_get_contents('php://input'), true);
if (!$data) json(400, ['error' => 'Corps de requête invalide']);

$amount = (int)($data['amount'] ?? 0);
$method = trim($data['method'] ?? '');

if ($amount <= 0) json(400, ['error' => 'Montant invalide']);
if (!in_array($method, ['orange', 'mtn', 'other'])) json(400, ['error' => 'Méthode de paiement invalide']);

try {
    $db = getDB();
    $userId = getAuthUserId();

    $stmt = $db->prepare('SELECT id, balance FROM wallets WHERE user_id = ?');
    $stmt->execute([$userId]);
    $wallet = $stmt->fetch();

    // 1 point pour chaque 500 FCFA rechargés
    $points = (int)floor($amount / 500);

    if (!$wallet) {
        $stmt = $db->prepare('INSERT INTO wallets (user_id, balance, total_points, current_points) VALUES (?, ?, ?, ?)');
        $stmt->execute([$userId, $amount, $points, $points]);
        $newBalance = $amount;
        $walletId = (int)$db->lastInsertId();
    } else {
        $stmt = $db->prepare('UPDATE wallets SET balance = balance + ?, total_points = total_points + ?, current_points = current_points + ? WHERE id = ?');
        $stmt->execute([$amount, $points, $points, $wallet['id']]);
        $newBalance = (int)$wallet['balance'] + $amount;
        $walletId = (int)$wallet['id'];
    }

    $methodLabels = ['orange' => 'Orange Money', 'mtn' => 'MTN Mobile Money', 'other' => 'Autre'];
    $stmt = $db->prepare('
        INSERT INTO wallet_transactions (wallet_id, type, amount_fcfa, points, description)
        VALUES (?, ?, ?, ?, ?)
    ');
    $stmt->execute([
        $walletId, 'recharge', $amount, $points,
        "Recharge via {$methodLabels[$method]}"
    ]);

    sendNotification(
        $userId,
        'Recharge effectuée',
        "Votre portefeuille a été crédité de {$amount} FCFA via {$methodLabels[$method]}.",
        'system'
    );

    json(200, [
        'success' => true,
        'new_balance' => $newBalance
    ]);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
