<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();
    $userId = getAuthUserId();

    if ($method === 'POST') {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $order_id = (int)($input['order_id'] ?? 0);
        $provider = trim($input['provider'] ?? '');
        $phone = trim($input['phone'] ?? '');

        if ($order_id <= 0 || $provider === '' || $phone === '') {
            json(400, ['error' => 'order_id, provider et phone requis']);
        }

        if (!in_array($provider, ['orange', 'mtn'])) {
            json(400, ['error' => 'Provider invalide. Utilisez orange ou mtn']);
        }

        if (!preg_match('/^6\d{8}$/', $phone)) {
            json(400, ['error' => 'Numéro de téléphone invalide. Format attendu: 6XXXXXXXX']);
        }

        $stmt = $db->prepare('SELECT * FROM orders WHERE id = ? AND user_id = ?');
        $stmt->execute([$order_id, $userId]);
        $order = $stmt->fetch();

        if (!$order) json(404, ['error' => 'Commande non trouvée']);

        if ($order['payment_status'] !== 'unpaid') {
            json(400, ['error' => 'Cette commande a déjà été payée']);
        }

        $stmt = $db->prepare('INSERT INTO payments (order_id, user_id, amount, provider, phone, status) VALUES (?, ?, ?, ?, ?, ?)');
        $stmt->execute([$order_id, $userId, $order['total_amount'], $provider, $phone, 'pending']);
        $paymentId = (int)$db->lastInsertId();

        json(201, [
            'payment_id' => $paymentId,
            'order_id' => $order_id,
            'amount' => (float)$order['total_amount'],
            'provider' => $provider,
            'phone' => $phone,
            'status' => 'pending',
            'message' => 'Demande de paiement initiée. Confirmez sur votre téléphone.'
        ]);
    }

    if ($method === 'GET') {
        $order_id = isset($_GET['order_id']) ? (int)$_GET['order_id'] : 0;

        if ($order_id <= 0) {
            json(400, ['error' => 'order_id requis']);
        }

        $stmt = $db->prepare('SELECT * FROM payments WHERE order_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT 1');
        $stmt->execute([$order_id, $userId]);
        $payment = $stmt->fetch();

        if (!$payment) json(404, ['error' => 'Paiement non trouvé']);

        $payment['id'] = (int)$payment['id'];
        $payment['order_id'] = (int)$payment['order_id'];
        $payment['user_id'] = (int)$payment['user_id'];
        $payment['amount'] = (float)$payment['amount'];

        json(200, ['payment' => $payment]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
