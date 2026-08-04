<?php
/**
 * Vendor order management endpoint.
 * GET  /orders/vendor.php?shop_id=X → list orders for the vendor's shop
 * PUT  /orders/vendor.php?id=X&status=confirmed → update order status (vendor only)
 * PUT  /orders/vendor.php?id=X&action=confirm_received → client confirms delivery
 * La commande passe à 'delivered' uniquement après les DEUX confirmations.
 */
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$userId = getAuthUserId();

try {
    $db = getDB();

    if ($method === 'GET') {
        // Verify vendor and get their shop
        $stmt = $db->prepare('SELECT id, name FROM shops WHERE vendor_id = ?');
        $stmt->execute([$userId]);
        $shop = $stmt->fetch();
        if (!$shop) json(404, ['error' => 'Aucune boutique trouvée pour ce vendeur']);

        $shopId = (int)$shop['id'];

        // Get orders containing products from this shop
        $stmt = $db->prepare('
            SELECT o.*,
                   u.name AS customer_name, u.phone AS customer_phone
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN products p ON oi.product_id = p.id
            JOIN users u ON o.user_id = u.id
            WHERE p.shop_id = ?
            GROUP BY o.id
            ORDER BY o.created_at DESC
        ');
        $stmt->execute([$shopId]);
        $orders = $stmt->fetchAll();

        foreach ($orders as &$order) {
            $order['id'] = (int)$order['id'];
            $order['total_amount'] = (float)$order['total_amount'];
            $order['user_id'] = (int)$order['user_id'];
            $order['vendor_confirmed'] = (int)($order['vendor_confirmed'] ?? 0);
            $order['client_confirmed'] = (int)($order['client_confirmed'] ?? 0);

            $stmt2 = $db->prepare('
                SELECT oi.*, p.title, p.image_url, p.price AS product_price
                FROM order_items oi
                JOIN products p ON oi.product_id = p.id
                WHERE oi.order_id = ? AND p.shop_id = ?
            ');
            $stmt2->execute([$order['id'], $shopId]);
            $items = $stmt2->fetchAll();

            $shopTotal = 0;
            foreach ($items as &$item) {
                $item['id'] = (int)$item['id'];
                $item['product_id'] = (int)$item['product_id'];
                $item['quantity'] = (int)$item['quantity'];
                $item['price'] = (float)$item['price'];
                $item['product_price'] = (float)$item['product_price'];
                $shopTotal += ($item['price'] * $item['quantity']);
            }
            unset($item);
            $order['items'] = $items;
            $order['shop_total'] = (float)$shopTotal;
        }
        unset($order);
        json(200, ['orders' => $orders, 'shop' => $shop]);
    }

    if ($method === 'PUT') {
        $orderId = isset($_GET['id']) ? (int)$_GET['id'] : 0;
        $action = trim($_GET['action'] ?? '');
        $newStatus = trim($_GET['status'] ?? '');

        if (!$orderId) {
            $body = json_decode(file_get_contents('php://input'), true);
            if ($body) {
                $orderId = (int)($body['id'] ?? $body['order_id'] ?? 0);
                $action = $action ?: trim($body['action'] ?? '');
                $newStatus = $newStatus ?: trim($body['status'] ?? '');
            }
        }

        if (!$orderId) json(400, ['error' => 'ID commande requis']);

        // Get current order info
        $stmt = $db->prepare('SELECT user_id, status, vendor_confirmed, client_confirmed FROM orders WHERE id = ?');
        $stmt->execute([$orderId]);
        $order = $stmt->fetch();
        if (!$order) json(404, ['error' => 'Commande introuvable']);

        $isOwner = (int)$order['user_id'] === $userId;

        // --- CLIENT ACTION: Confirm Reception ---
        if ($action === 'confirm_received') {
            if (!$isOwner) json(403, ['error' => 'Seul le client peut confirmer la réception.']);
            if ($order['status'] !== 'delivering') json(400, ['error' => 'La commande doit être en cours de livraison.']);

            $stmt = $db->prepare("UPDATE orders SET client_confirmed = 1 WHERE id = ?");
            $stmt->execute([$orderId]);

            // Notify vendor
            $stmtV = $db->prepare('SELECT s.vendor_id FROM order_items oi JOIN products p ON oi.product_id = p.id JOIN shops s ON p.shop_id = s.id WHERE oi.order_id = ? LIMIT 1');
            $stmtV->execute([$orderId]);
            $v = $stmtV->fetch();
            if ($v) sendNotification((int)$v['vendor_id'], "Réception client confirmée", "Le client a reçu sa commande #$orderId. Veuillez la clôturer.", 'order', $orderId);

            json(200, ['success' => true, 'message' => 'Réception confirmée par le client.']);
        }

        // --- VENDOR ACTIONS: Update Status ---
        if ($newStatus !== '') {
            if ($isOwner) json(403, ['error' => 'Le client ne peut pas changer le statut directement.']);

            // Verify vendor
            $stmtV = $db->prepare('SELECT oi.id FROM order_items oi JOIN products p ON oi.product_id = p.id JOIN shops s ON p.shop_id = s.id WHERE oi.order_id = ? AND s.vendor_id = ? LIMIT 1');
            $stmtV->execute([$orderId, $userId]);
            if (!$stmtV->fetch()) json(403, ['error' => 'Non autorisé - vous n\'êtes pas le vendeur de cette commande.']);

            if ($newStatus === 'delivered') {
                // Mark vendor confirmed
                $stmt = $db->prepare("UPDATE orders SET vendor_confirmed = 1 WHERE id = ?");
                $stmt->execute([$orderId]);

                // Check if client also confirmed
                if ((int)$order['client_confirmed'] === 1) {
                    $stmt = $db->prepare("UPDATE orders SET status = 'delivered' WHERE id = ?");
                    $stmt->execute([$orderId]);
                    handleOrderDelivery($db, $orderId);
                    sendNotification((int)$order['user_id'], "Commande livrée", "La commande #$orderId est maintenant clôturée.", 'order', $orderId);
                    json(200, ['success' => true, 'message' => 'Commande clôturée avec succès.']);
                } else {
                    json(200, ['success' => true, 'message' => 'Vente confirmée. En attente de la réception par le client.']);
                }
                exit; // IMPORTANT: Stop execution here for 'delivered' status
            }

            // Other statuses
            $allowed = ['confirmed', 'preparing', 'delivering', 'cancelled'];
            if (!in_array($newStatus, $allowed)) json(400, ['error' => 'Statut invalide']);

            $stmt = $db->prepare("UPDATE orders SET status = ? WHERE id = ?");
            $stmt->execute([$newStatus, $orderId]);

            sendNotification((int)$order['user_id'], "Commande mise à jour", "Votre commande #$orderId est passée à : $newStatus", 'order', $orderId);
            json(200, ['success' => true, 'message' => 'Statut mis à jour.', 'debug_status' => $newStatus]);
        }

        json(400, ['error' => 'Action ou statut requis']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur : ' . $e->getMessage()]);
}
