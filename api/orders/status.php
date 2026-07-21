<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method !== 'PUT') json(405, ['error' => 'Méthode non autorisée']);

try {
    $db = getDB();
    $userId = getAuthUserId();

    $input = json_decode(file_get_contents('php://input'), true);
    if (!$input) json(400, ['error' => 'Corps de requête invalide']);

    $order_id = (int)($input['order_id'] ?? 0);
    $newStatus = trim($input['status'] ?? '');

    if ($order_id <= 0 || $newStatus === '') {
        json(400, ['error' => 'order_id et status requis']);
    }

    $stmt = $db->prepare('
        SELECT DISTINCT o.id, o.status AS current_status
        FROM orders o
        JOIN order_items oi ON oi.order_id = o.id
        JOIN products p ON oi.product_id = p.id
        JOIN shops s ON p.shop_id = s.id
        WHERE o.id = ? AND s.vendor_id = ?
    ');
    $stmt->execute([$order_id, $userId]);
    $order = $stmt->fetch();

    if (!$order) json(404, ['error' => 'Commande non trouvée ou accès refusé']);

    $allowedTransitions = [
        'pending' => 'confirmed',
        'confirmed' => 'preparing',
        'preparing' => 'delivering',
        'delivering' => 'delivered',
    ];

    $current = $order['current_status'];

    if (!isset($allowedTransitions[$current]) || $allowedTransitions[$current] !== $newStatus) {
        json(400, ['error' => "Transition de statut invalide: $current → $newStatus"]);
    }

    $stmt = $db->prepare('UPDATE orders SET status = ? WHERE id = ?');
    $stmt->execute([$newStatus, $order_id]);

    // ── Award loyalty points on successful delivery ──
    if ($newStatus === 'delivered') {
        // Award 1 point to the buyer
        $stmtB = $db->prepare('SELECT user_id FROM orders WHERE id = ?');
        $stmtB->execute([$order_id]);
        $buyerId = (int)$stmtB->fetchColumn();
        if ($buyerId > 0) {
            awardPoints($db, $buyerId, 1, "Achat réussi #{$order_id}", 'order', $order_id);
            sendNotification($buyerId, "Point fidélité gagné 🎉", "Vous avez gagné 1 point de fidélité pour votre achat #{$order_id}.", 'order', $order_id);
        }

        // Award 1 point to the vendor(s) of this order
        $stmtV = $db->prepare('
            SELECT DISTINCT s.vendor_id
            FROM order_items oi
            JOIN products p ON oi.product_id = p.id
            JOIN shops s ON p.shop_id = s.id
            WHERE oi.order_id = ?
        ');
        $stmtV->execute([$order_id]);
        $vendorIds = $stmtV->fetchAll(PDO::FETCH_COLUMN);
        foreach ($vendorIds as $vendorId) {
            $vendorId = (int)$vendorId;
            if ($vendorId > 0) {
                awardPoints($db, $vendorId, 1, "Vente réussie #{$order_id}", 'order', $order_id);
                sendNotification($vendorId, "Point fidélité gagné 🎉", "Vous avez gagné 1 point de fidélité pour la vente #{$order_id}.", 'order', $order_id);
            }
        }
    }

    json(200, ['message' => 'Statut mis à jour', 'status' => $newStatus]);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
