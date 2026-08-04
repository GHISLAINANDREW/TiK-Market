<?php
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') json(405, ['error' => 'Méthode non autorisée']);

try {
    $db = getDB();
    $userId = getAuthUserId();

    $stmt = $db->prepare('SELECT id FROM wallets WHERE user_id = ?');
    $stmt->execute([$userId]);
    $wallet = $stmt->fetch();

    if (!$wallet) {
        json(200, ['success' => true, 'transactions' => []]);
    }

    $stmt = $db->prepare('
        SELECT id, type, amount_fcfa, points, description, reference_type, reference_id, created_at
        FROM wallet_transactions
        WHERE wallet_id = ?
        ORDER BY created_at DESC
        LIMIT 50
    ');
    $stmt->execute([(int)$wallet['id']]);
    $transactions = $stmt->fetchAll();

    foreach ($transactions as &$t) {
        $t['id'] = (int)$t['id'];
        $t['amount_fcfa'] = (int)$t['amount_fcfa'];
        $t['points'] = (int)$t['points'];
        $t['reference_id'] = $t['reference_id'] !== null ? (int)$t['reference_id'] : null;
    }

    json(200, ['success' => true, 'transactions' => $transactions]);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
