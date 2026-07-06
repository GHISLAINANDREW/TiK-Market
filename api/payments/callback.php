<?php
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$input = json_decode(file_get_contents('php://input'), true);
if (!$input) json(400, ['error' => 'Corps de requête invalide']);

$payment_id = (int)($input['payment_id'] ?? 0);
$status = trim($input['status'] ?? '');
$transaction_id = trim($input['transaction_id'] ?? '');

if ($payment_id <= 0 || $status === '' || $transaction_id === '') {
    json(400, ['error' => 'payment_id, status et transaction_id requis']);
}

if (!in_array($status, ['success', 'failed'])) {
    json(400, ['error' => 'Status invalide. Utilisez success ou failed']);
}

try {
    $db = getDB();

    $stmt = $db->prepare('SELECT * FROM payments WHERE id = ?');
    $stmt->execute([$payment_id]);
    $payment = $stmt->fetch();

    if (!$payment) json(404, ['error' => 'Paiement non trouvé']);

    if ($payment['status'] !== 'pending') {
        json(400, ['error' => 'Ce paiement a déjà été traité']);
    }

    $db->beginTransaction();

    $stmt = $db->prepare('UPDATE payments SET status = ?, transaction_id = ? WHERE id = ?');
    $stmt->execute([$status, $transaction_id, $payment_id]);

    if ($status === 'success') {
        $stmt = $db->prepare('UPDATE orders SET payment_status = \'paid\', status = \'confirmed\' WHERE id = ?');
        $stmt->execute([$payment['order_id']]);
    } else {
        $stmt = $db->prepare('UPDATE orders SET payment_status = \'unpaid\' WHERE id = ?');
        $stmt->execute([$payment['order_id']]);
    }

    $db->commit();

    json(200, ['success' => true]);
} catch (PDOException $e) {
    if (isset($db) && $db->inTransaction()) $db->rollBack();
    json(500, ['error' => 'Erreur serveur']);
}
