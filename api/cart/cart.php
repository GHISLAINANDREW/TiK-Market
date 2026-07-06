<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();
    $userId = getAuthUserId();

    if ($method === 'GET') {
        $stmt = $db->prepare('
            SELECT ci.id, ci.product_id, ci.quantity, ci.created_at,
                p.title, p.price, p.compare_price, p.image_url, p.stock, s.name AS shop_name
            FROM cart_items ci
            JOIN products p ON ci.product_id = p.id
            JOIN shops s ON p.shop_id = s.id
            WHERE ci.user_id = ?
            ORDER BY ci.created_at DESC
        ');
        $stmt->execute([$userId]);
        $items = $stmt->fetchAll();

        foreach ($items as &$item) {
            $item['id'] = (int)$item['id'];
            $item['product_id'] = (int)$item['product_id'];
            $item['quantity'] = (int)$item['quantity'];
            $item['price'] = (float)$item['price'];
            if ($item['compare_price']) $item['compare_price'] = (float)$item['compare_price'];
            $item['stock'] = (int)$item['stock'];
        }
        unset($item);

        json(200, ['items' => $items]);
    }

    if ($method === 'POST') {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $product_id = (int)($input['product_id'] ?? 0);
        $quantity = max(1, (int)($input['quantity'] ?? 1));

        if ($product_id <= 0) json(400, ['error' => 'product_id requis']);

        $stmt = $db->prepare('SELECT id, stock FROM products WHERE id = ? AND is_active = 1');
        $stmt->execute([$product_id]);
        $product = $stmt->fetch();
        if (!$product) json(404, ['error' => 'Produit non trouvé']);

        $stmt = $db->prepare('
            INSERT INTO cart_items (user_id, product_id, quantity)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)
        ');
        $stmt->execute([$userId, $product_id, $quantity]);

        json(200, ['message' => 'Produit ajouté au panier']);
    }

    if ($method === 'PUT') {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $product_id = (int)($input['product_id'] ?? 0);
        $quantity = (int)($input['quantity'] ?? 0);

        if ($product_id <= 0) json(400, ['error' => 'product_id requis']);

        if ($quantity <= 0) {
            $stmt = $db->prepare('DELETE FROM cart_items WHERE user_id = ? AND product_id = ?');
            $stmt->execute([$userId, $product_id]);
        } else {
            $stmt = $db->prepare('UPDATE cart_items SET quantity = ? WHERE user_id = ? AND product_id = ?');
            $stmt->execute([$quantity, $userId, $product_id]);
        }

        json(200, ['message' => 'Panier mis à jour']);
    }

    if ($method === 'DELETE') {
        $product_id = isset($_GET['product_id']) ? (int)$_GET['product_id'] : 0;
        if ($product_id <= 0) json(400, ['error' => 'product_id requis']);

        $stmt = $db->prepare('DELETE FROM cart_items WHERE user_id = ? AND product_id = ?');
        $stmt->execute([$userId, $product_id]);

        json(200, ['message' => 'Produit retiré du panier']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
