<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$id = isset($_GET['id']) ? (int)$_GET['id'] : null;
$action = trim($_GET['action'] ?? '');

try {
    $db = getDB();
    $userId = getAuthUserId();

    // ── Ensure payment_type column exists ──
    try {
        $db->exec("ALTER TABLE orders ADD COLUMN payment_type VARCHAR(20) DEFAULT 'delivery' AFTER payment_status");
    } catch (PDOException $e) {
        // Column already exists — ignore
    }

    // ── GET ──
    if ($method === 'GET') {
        if ($id) {
            $stmt = $db->prepare('SELECT * FROM orders WHERE id = ? AND user_id = ?');
            $stmt->execute([$id, $userId]);
            $order = $stmt->fetch();
            if (!$order) json(404, ['error' => 'Commande non trouvée']);

            $stmt = $db->prepare('
                SELECT oi.*, p.title, p.image_url, p.price AS product_price
                FROM order_items oi
                JOIN products p ON oi.product_id = p.id
                WHERE oi.order_id = ?
            ');
            $stmt->execute([$id]);
            $items = $stmt->fetchAll();

            foreach ($items as &$item) {
                $item['id'] = (int)$item['id'];
                $item['product_id'] = (int)$item['product_id'];
                $item['quantity'] = (int)$item['quantity'];
                $item['price'] = (float)$item['price'];
                $item['product_price'] = (float)$item['product_price'];
            }
            unset($item);

            $order['id'] = (int)$order['id'];
            $order['total_amount'] = (float)$order['total_amount'];
            $order['items'] = $items;

            json(200, ['order' => $order]);
        }

        $stmt = $db->prepare('
            SELECT o.*,
                (SELECT COUNT(*) FROM order_items oi WHERE oi.order_id = o.id) AS item_count
            FROM orders o
            WHERE o.user_id = ?
            ORDER BY o.created_at DESC
        ');
        $stmt->execute([$userId]);
        $orders = $stmt->fetchAll();

        foreach ($orders as &$o) {
            $o['id'] = (int)$o['id'];
            $o['total_amount'] = (float)$o['total_amount'];
            $o['item_count'] = (int)$o['item_count'];
        }
        unset($o);

        json(200, ['orders' => $orders]);
    }

    // ── POST : Create order ──
    if ($method === 'POST') {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $shipping_address = trim($input['shipping_address'] ?? '');
        $phone = trim($input['phone'] ?? '');
        $notes = trim($input['notes'] ?? '');
        $payment_method = trim($input['payment_method'] ?? '');
        $payment_type = trim($input['payment_type'] ?? 'delivery'); // 'direct' ou 'delivery'

        if ($shipping_address === '' || $phone === '') {
            json(400, ['error' => 'shipping_address et phone requis']);
        }

        // Accept items from request body
        $requestItems = $input['items'] ?? null;
        
        if ($requestItems !== null && is_array($requestItems) && count($requestItems) > 0) {
            $cartItems = [];
            foreach ($requestItems as $ri) {
                $productId = (int)($ri['product_id'] ?? 0);
                $quantity = (int)($ri['quantity'] ?? 0);
                if ($productId <= 0 || $quantity <= 0) continue;
                
                $stmtP = $db->prepare('SELECT id, price, stock, title FROM products WHERE id = ? AND is_active = 1');
                $stmtP->execute([$productId]);
                $prod = $stmtP->fetch();
                if (!$prod) json(400, ['error' => "Produit #$productId introuvable"]);
                
                if ($quantity > $prod['stock']) {
                    json(400, ['error' => "Stock insuffisant pour {$prod['title']}"]);
                }
                
                $cartItems[] = [
                    'product_id' => $productId,
                    'quantity' => $quantity,
                    'price' => (float)$prod['price'],
                    'stock' => (int)$prod['stock'],
                    'title' => $prod['title']
                ];
            }
            
            if (empty($cartItems)) {
                json(400, ['error' => 'Panier vide']);
            }
        } else {
            $db->beginTransaction();
            $stmt = $db->prepare('
                SELECT ci.product_id, ci.quantity, p.price, p.stock, p.title
                FROM cart_items ci
                JOIN products p ON ci.product_id = p.id
                WHERE ci.user_id = ?
            ');
            $stmt->execute([$userId]);
            $cartItems = $stmt->fetchAll();

            if (empty($cartItems)) {
                $db->rollBack();
                json(400, ['error' => 'Panier vide']);
            }
        }

        if (!$db->inTransaction()) {
            $db->beginTransaction();
        }

        $total = 0;
        foreach ($cartItems as $item) {
            $total += (float)$item['price'] * (int)$item['quantity'];
        }

        $orderNumber = 'CMD-' . time();

        if ($payment_type === 'direct') {
            // Paiement direct au vendeur : commande en attente de validation vendeur
            $status = 'pending';
            $paymentStatus = 'unpaid';
        } else {
            // Paiement à la livraison : commande en attente
            $status = 'pending';
            $paymentStatus = 'unpaid';
        }

        $stmt = $db->prepare('
            INSERT INTO orders (user_id, order_number, total_amount, status, payment_method, payment_status, payment_type, phone, shipping_address, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ');
        $stmt->execute([$userId, $orderNumber, $total, $status, $payment_method, $paymentStatus, $payment_type, $phone, $shipping_address, $notes ?: null]);
        $orderId = (int)$db->lastInsertId();

        // Insert order items
        $stmtItem = $db->prepare('INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)');
        foreach ($cartItems as $item) {
            $stmtItem->execute([$orderId, $item['product_id'], $item['quantity'], $item['price']]);
        }

        // Clear cart
        if ($requestItems === null || !is_array($requestItems) || count($requestItems) === 0) {
            $stmt = $db->prepare('DELETE FROM cart_items WHERE user_id = ?');
            $stmt->execute([$userId]);
        }

        $db->commit();

        // Get vendor info for direct payment
        $vendorInfo = [];
        if ($payment_type === 'direct') {
            $stmt = $db->prepare('
                SELECT DISTINCT s.name AS shop_name, s.phone AS vendor_phone, u.phone AS vendor_phone_user
                FROM order_items oi
                JOIN products p ON oi.product_id = p.id
                JOIN shops s ON p.shop_id = s.id
                JOIN users u ON s.vendor_id = u.id
                WHERE oi.order_id = ?
            ');
            $stmt->execute([$orderId]);
            $vendorInfo = $stmt->fetchAll();
        }

        // Notifications
        sendNotification($userId, "Commande enregistrée", "Votre commande $orderNumber a été enregistrée.", 'order', $orderId);

        $stmt = $db->prepare('
            SELECT DISTINCT s.vendor_id
            FROM order_items oi
            JOIN products p ON oi.product_id = p.id
            JOIN shops s ON p.shop_id = s.id
            WHERE oi.order_id = ?
        ');
        $stmt->execute([$orderId]);
        $vendors = $stmt->fetchAll(PDO::FETCH_COLUMN);
        foreach ($vendors as $vendorId) {
            sendNotification((int)$vendorId, "Nouvelle commande", "Vous avez reçu une nouvelle commande ($orderNumber).", 'order', $orderId);
        }

        $stmt = $db->prepare('SELECT * FROM orders WHERE id = ?');
        $stmt->execute([$orderId]);
        $order = $stmt->fetch();

        $stmt = $db->prepare('
            SELECT oi.*, p.title, p.image_url
            FROM order_items oi
            JOIN products p ON oi.product_id = p.id
            WHERE oi.order_id = ?
        ');
        $stmt->execute([$orderId]);
        $items = $stmt->fetchAll();

        foreach ($items as &$item) {
            $item['id'] = (int)$item['id'];
            $item['product_id'] = (int)$item['product_id'];
            $item['quantity'] = (int)$item['quantity'];
            $item['price'] = (float)$item['price'];
        }
        unset($item);

        $order['id'] = (int)$order['id'];
        $order['total_amount'] = (float)$order['total_amount'];
        $order['items'] = $items;
        $order['vendor_info'] = $vendorInfo;

        json(201, ['order' => $order]);
    }

    // ── PUT : Validate payment (vendor) ──
    if ($method === 'PUT') {
        if (!$id) json(400, ['error' => 'ID commande requis']);

        if ($action === 'validate_payment') {
            // Vendor validates that they received payment for a 'direct' order
            $stmt = $db->prepare('
                SELECT o.id, o.payment_type, o.status FROM orders o
                JOIN order_items oi ON oi.order_id = o.id
                JOIN products p ON oi.product_id = p.id
                JOIN shops s ON p.shop_id = s.id
                WHERE o.id = ? AND s.vendor_id = ?
                LIMIT 1
            ');
            $stmt->execute([$id, $userId]);
            $orderCheck = $stmt->fetch();
            if (!$orderCheck) json(403, ['error' => 'Non autorisé ou commande introuvable']);
            if ($orderCheck['payment_type'] !== 'direct') json(400, ['error' => 'Paiement déjà traité ou type incorrect']);

            $stmt = $db->prepare("UPDATE orders SET status = 'confirmed', payment_status = 'paid' WHERE id = ?");
            $stmt->execute([$id]);

            // Notify customer
            $stmtU = $db->prepare('SELECT user_id FROM orders WHERE id = ?');
            $stmtU->execute([$id]);
            $owner = $stmtU->fetch();
            if ($owner) {
                sendNotification((int)$owner['user_id'], "Paiement confirmé", "Votre paiement a été confirmé par le vendeur. Commande en préparation.", 'order', $id);
            }

            json(200, ['success' => true, 'message' => 'Paiement confirmé']);
        }

        if ($action === 'confirm_delivery') {
            // Vendor confirms a 'delivery' order (ready to prepare)
            $stmt = $db->prepare('
                SELECT o.id, o.payment_type, o.status FROM orders o
                JOIN order_items oi ON oi.order_id = o.id
                JOIN products p ON oi.product_id = p.id
                JOIN shops s ON p.shop_id = s.id
                WHERE o.id = ? AND s.vendor_id = ? AND o.status = ?
                LIMIT 1
            ');
            $stmt->execute([$id, $userId, 'pending']);
            $orderCheck = $stmt->fetch();
            if (!$orderCheck) json(403, ['error' => 'Non autorisé ou commande déjà traitée']);

            $stmt = $db->prepare("UPDATE orders SET status = 'confirmed' WHERE id = ?");
            $stmt->execute([$id]);

            json(200, ['success' => true, 'message' => 'Commande confirmée']);
        }

        json(400, ['error' => 'Action non reconnue. Utilisez validate_payment ou confirm_delivery']);
    }

    // ── DELETE : Customer cancels a pending order ──
    if ($method === 'DELETE') {
        if (!$id) json(400, ['error' => 'ID commande requis']);

        $stmt = $db->prepare("SELECT id, status FROM orders WHERE id = ? AND user_id = ?");
        $stmt->execute([$id, $userId]);
        $orderCheck = $stmt->fetch();
        if (!$orderCheck) json(404, ['error' => 'Commande non trouvée']);
        if ($orderCheck['status'] !== 'pending') json(400, ['error' => 'Seules les commandes en attente peuvent être annulées']);

        $stmt = $db->prepare("UPDATE orders SET status = 'cancelled' WHERE id = ?");
        $stmt->execute([$id]);

        json(200, ['success' => true, 'message' => 'Commande annulée']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    if (isset($db) && $db->inTransaction()) $db->rollBack();
    json(500, ['error' => 'Erreur serveur']);
}
