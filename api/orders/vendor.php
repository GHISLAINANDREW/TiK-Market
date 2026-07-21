<?php
/**
 * Vendor order management endpoint.
 * GET  /orders/vendor.php?shop_id=X → list orders for the vendor's shop
 * PUT  /orders/vendor.php?id=X&status=confirmed → update order status (vendor: any status, client: only 'delivered')
 * PUT  /orders/vendor.php?id=X&action=confirm_received → client confirms delivery (alternative)
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
            SELECT DISTINCT o.id, o.order_number, o.total_amount, o.status, o.payment_method,
                   o.payment_status, o.phone, o.shipping_address, o.notes, o.created_at,
                   o.payment_type, o.user_id, u.name AS customer_name, u.phone AS customer_phone
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN products p ON oi.product_id = p.id
            JOIN users u ON o.user_id = u.id
            WHERE p.shop_id = ?
            ORDER BY o.created_at DESC
        ');
        $stmt->execute([$shopId]);
        $orders = $stmt->fetchAll();

        foreach ($orders as &$order) {
            $order['id'] = (int)$order['id'];
            $order['total_amount'] = (float)$order['total_amount'];
            $order['user_id'] = (int)$order['user_id'];

            // Get items for this order that belong to this shop
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
            $order['shop_total'] = (float)$shopTotal; // Vendor specific total
        }
        unset($order);

        json(200, ['orders' => $orders, 'shop' => $shop]);
    }

    if ($method === 'PUT') {
        // Accept params from query string or JSON body
        $orderId = isset($_GET['id']) ? (int)$_GET['id'] : 0;
        $newStatus = trim($_GET['status'] ?? '');
        if (!$orderId || $newStatus === '') {
            // Fallback: read from request body
            $body = json_decode(file_get_contents('php://input'), true);
            if ($body) {
                $orderId = (int)($body['id'] ?? $body['order_id'] ?? 0);
                $newStatus = trim($body['status'] ?? '');
            }
        }
        // Also try QUERY_STRING directly as fallback
        if (!$orderId || $newStatus === '') {
            parse_str($_SERVER['QUERY_STRING'] ?? '', $qParams);
            $orderId = (int)($qParams['id'] ?? $orderId);
            $newStatus = trim($qParams['status'] ?? $newStatus);
        }

        // Dedicated action: client confirms reception (more explicit than status=delivered)
        $action = trim($_GET['action'] ?? '');
        if ($action === 'confirm_received') {
            $orderId = isset($_GET['id']) ? (int)$_GET['id'] : 0;
            if (!$orderId) json(400, ['error' => 'ID commande requis']);
            $stmt = $db->prepare('SELECT user_id, status FROM orders WHERE id = ?');
            $stmt->execute([$orderId]);
            $order = $stmt->fetch();
            if (!$order) json(404, ['error' => 'Commande introuvable']);
            if ((int)$order['user_id'] !== $userId) json(403, ['error' => 'Non autorisé - cette commande ne vous appartient pas']);
            if ($order['status'] !== 'delivering') json(400, ['error' => "La commande doit être en cours de livraison (statut actuel: {$order['status']})"]);
            $stmt = $db->prepare("UPDATE orders SET status = 'delivered' WHERE id = ?");
            $stmt->execute([$orderId]);
            sendNotification((int)$order['user_id'], "Livraison confirmée", "Vous avez confirmé la réception de la commande.", 'order', $orderId);
            json(200, ['success' => true, 'message' => 'Livraison confirmée. Merci !']);
        }

        $allowedStatuses = ['pending', 'confirmed', 'preparing', 'delivering', 'delivered', 'cancelled'];
        if (!$orderId || !in_array($newStatus, $allowedStatuses)) {
            json(400, ['error' => 'ID commande et statut requis (confirmed|preparing|delivering|delivered|cancelled)']);
        }

        // Get current order info
        $stmtOwner = $db->prepare('SELECT user_id, status FROM orders WHERE id = ?');
        $stmtOwner->execute([$orderId]);
        $order = $stmtOwner->fetch();
        if (!$order) json(404, ['error' => 'Commande introuvable']);
        
        $isOwner = (int)$order['user_id'] === $userId;
        $currentStatus = $order['status'];

        if ($isOwner) {
            // Client: can ONLY set to 'delivered' and ONLY if currently 'delivering'
            if ($newStatus !== 'delivered') {
                json(403, ['error' => 'En tant que client, vous ne pouvez que confirmer la réception de la commande. Utilisez le statut "delivered".']);
            }
            if ($currentStatus !== 'delivering') {
                json(400, ['error' => "La commande doit être en cours de livraison (statut actuel: $currentStatus)"]);
            }
        } else {
            // Verify vendor owns products in this order
            $stmt = $db->prepare('
                SELECT oi.id FROM order_items oi
                JOIN products p ON oi.product_id = p.id
                JOIN shops s ON p.shop_id = s.id
                WHERE oi.order_id = ? AND s.vendor_id = ?
                LIMIT 1
            ');
            $stmt->execute([$orderId, $userId]);
            if (!$stmt->fetch()) json(403, ['error' => 'Non autorisé - vous n\'êtes ni le propriétaire de cette commande ni le vendeur associé.']);
        }

        $stmt = $db->prepare('UPDATE orders SET status = ? WHERE id = ?');
        $stmt->execute([$newStatus, $orderId]);

        // Notifier l'acheteur
        $stmtU = $db->prepare('SELECT user_id FROM orders WHERE id = ?');
        $stmtU->execute([$orderId]);
        $orderOwner = $stmtU->fetch();
        if ($orderOwner) {
            $statusLabels = [
                'confirmed' => 'confirmée',
                'preparing' => 'en cours de préparation',
                'delivering' => 'en livraison',
                'delivered' => 'livrée',
                'cancelled' => 'annulée'
            ];
            $label = $statusLabels[$newStatus] ?? $newStatus;
            sendNotification((int)$orderOwner['user_id'], "Commande #$orderId $label", "Votre commande #$orderId est maintenant $label.", 'order', $orderId);
        }

        json(200, ['success' => true, 'message' => "Statut mis à jour : $newStatus"]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
