<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$id = isset($_GET['id']) ? (int)$_GET['id'] : null;

try {
    $db = getDB();
    $userId = getAuthUserId();

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

    if ($method === 'POST') {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $shipping_address = trim($input['shipping_address'] ?? '');
        $phone = trim($input['phone'] ?? '');
        $notes = trim($input['notes'] ?? '');
        $payment_method = trim($input['payment_method'] ?? 'Mobile Money');

        if ($shipping_address === '' || $phone === '') {
            json(400, ['error' => 'shipping_address et phone requis']);
        }

        // Accept items from request body OR fall back to cart_items table
        $requestItems = $input['items'] ?? null;
        
        if ($requestItems !== null && is_array($requestItems) && count($requestItems) > 0) {
            // Items provided directly in the request
            $cartItems = [];
            foreach ($requestItems as $ri) {
                $productId = (int)($ri['product_id'] ?? 0);
                $quantity = (int)($ri['quantity'] ?? 0);
                if ($productId <= 0 || $quantity <= 0) continue;
                
                // Fetch product details from DB for validation
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
            // Fallback: read from cart_items table (legacy)
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

        // Start transaction for order creation
        if (!$db->inTransaction()) {
            $db->beginTransaction();
        }

        $total = 0;
        foreach ($cartItems as $item) {
            $total += (float)$item['price'] * (int)$item['quantity'];
        }

        $orderNumber = 'CMD-' . time();

        // Paiement désactivé : commande créée directement comme confirmée
        $stmt = $db->prepare('
            INSERT INTO orders (user_id, order_number, total_amount, status, payment_method, payment_status, phone, shipping_address, notes)
            VALUES (?, ?, ?, \'confirmed\', ?, \'paid\', ?, ?, ?)
        ');
        $stmt->execute([$userId, $orderNumber, $total, $payment_method, $phone, $shipping_address, $notes ?: null]);
        $orderId = (int)$db->lastInsertId();

        $stmtItem = $db->prepare('INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)');
        $stmtSales = $db->prepare('UPDATE products SET total_sales = total_sales + ? WHERE id = ?');
        foreach ($cartItems as $item) {
            $stmtItem->execute([$orderId, $item['product_id'], $item['quantity'], $item['price']]);
            $stmtSales->execute([$item['quantity'], $item['product_id']]);
        }

        // Clear cart_items only if we used the legacy path
        if ($requestItems === null || !is_array($requestItems) || count($requestItems) === 0) {
            $stmt = $db->prepare('DELETE FROM cart_items WHERE user_id = ?');
            $stmt->execute([$userId]);
        }

        $db->commit();

        // Notifications
        sendNotification($userId, "Commande confirmée", "Votre commande $orderNumber a été enregistrée avec succès.", 'order', $orderId);

        // Notifier les vendeurs
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

        json(201, ['order' => $order]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    if (isset($db) && $db->inTransaction()) $db->rollBack();
    json(500, ['error' => 'Erreur serveur']);
}
