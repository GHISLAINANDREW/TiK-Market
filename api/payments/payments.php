<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // Auto-migration (development)
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS payments (
            id INT AUTO_INCREMENT PRIMARY KEY,
            order_id INT NOT NULL,
            amount DECIMAL(12,2) NOT NULL,
            provider VARCHAR(20) NOT NULL,
            phone VARCHAR(20) NOT NULL,
            transaction_id VARCHAR(100) DEFAULT NULL,
            status VARCHAR(20) DEFAULT 'pending',
            message TEXT DEFAULT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
        )");
    } catch (Exception $e) {}

    $userId = getAuthUserId();

    if ($method === 'POST') {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $order_id = (int)($input['order_id'] ?? 0);
        $provider = trim($input['provider'] ?? '');
        $phone = trim($input['phone'] ?? '');

        if ($order_id <= 0 || !in_array($provider, ['MTN', 'Orange', 'Moov', 'Camtel']) || $phone === '') {
            json(400, ['error' => 'order_id, provider (MTN/Orange/Moov/Camtel) et phone requis']);
        }

        // Vérifier que la commande appartient à l'utilisateur
        $stmt = $db->prepare('SELECT id, total FROM orders WHERE id = ? AND user_id = ?');
        $stmt->execute([$order_id, $userId]);
        $order = $stmt->fetch();
        if (!$order) json(404, ['error' => 'Commande non trouvée']);

        // Vérifier qu'aucun paiement n'existe déjà pour cette commande
        $stmt = $db->prepare("SELECT id FROM payments WHERE order_id = ? AND status = 'completed'");
        $stmt->execute([$order_id]);
        if ($stmt->fetch()) json(400, ['error' => 'Cette commande est déjà payée']);

        // Simulation : générer un ID de transaction
        $txId = strtoupper(bin2hex(random_bytes(8)));

        // Insérer le paiement
        $stmt = $db->prepare("INSERT INTO payments (order_id, amount, provider, phone, transaction_id, status, message) VALUES (?, ?, ?, ?, ?, 'completed', 'Paiement simulé avec succès')");
        $stmt->execute([$order_id, $order['total'], $provider, $phone, $txId]);
        $paymentId = (int)$db->lastInsertId();

        // Mettre à jour le statut de la commande
        $stmt = $db->prepare("UPDATE orders SET payment_status = 'paid', status = 'confirmed', payment_method = ? WHERE id = ?");
        $stmt->execute([$provider, $order_id]);

        // Notification à l'acheteur
        sendNotification($userId, "Paiement confirmé", "Votre paiement de " . number_format($order['total'], 0, ',', ' ') . " FCFA pour la commande #$order_id a été reçu.", 'order', $order_id);

        // Notification au vendeur (trouver le vendeur via les produits de la commande)
        $stmtV = $db->prepare('SELECT DISTINCT s.vendor_id FROM order_items oi JOIN products p ON oi.product_id = p.id JOIN shops s ON p.shop_id = s.id WHERE oi.order_id = ?');
        $stmtV->execute([$order_id]);
        $vendors = $stmtV->fetchAll();
        foreach ($vendors as $v) {
            sendNotification((int)$v['vendor_id'], "Nouvelle commande payée", "Un client a payé sa commande #$order_id. Préparez les articles.", 'order', $order_id);
        }

        json(200, [
            'payment' => [
                'id' => $paymentId,
                'order_id' => $order_id,
                'amount' => (float)$order['total'],
                'provider' => $provider,
                'phone' => $phone,
                'transaction_id' => $txId,
                'status' => 'completed'
            ]
        ]);
    }

    if ($method === 'GET') {
        $order_id = isset($_GET['order_id']) ? (int)$_GET['order_id'] : 0;
        if ($order_id > 0) {
            $stmt = $db->prepare('SELECT * FROM payments WHERE order_id = ? ORDER BY created_at DESC LIMIT 1');
            $stmt->execute([$order_id]);
            $payment = $stmt->fetch();
            if ($payment) {
                $payment['id'] = (int)$payment['id'];
                $payment['order_id'] = (int)$payment['order_id'];
                $payment['amount'] = (float)$payment['amount'];
                json(200, $payment);
            } else {
                json(404, ['error' => 'Aucun paiement trouvé pour cette commande']);
            }
        }
        json(400, ['error' => 'order_id requis']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
} catch (\Throwable $e) {
    json(500, ['error' => 'Erreur serveur']);
}
