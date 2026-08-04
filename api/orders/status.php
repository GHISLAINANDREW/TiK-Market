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

    if ($newStatus === 'delivered') {
        // Protection : Utiliser vendor.php pour la double confirmation
        json(400, ['error' => "Pour marquer comme livré, utilisez l'endpoint vendor.php qui gère la double confirmation client/vendeur."]);
    }

    $stmt = $db->prepare('UPDATE orders SET status = ? WHERE id = ?');
    $stmt->execute([$newStatus, $order_id]);

    // ── Award loyalty points and update sales on successful delivery ──
    if ($newStatus === 'delivered') {
        handleOrderDelivery($db, $order_id);
    }

    json(200, ['message' => 'Statut mis à jour', 'status' => $newStatus]);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
