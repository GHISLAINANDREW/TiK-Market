<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();
    $userId = getAuthUserId();

    if ($method === 'GET') {
        $stmt = $db->prepare('
            SELECT w.id, w.product_id, w.created_at,
                p.title, p.price, p.compare_price, p.image_url, p.stock,
                s.name AS shop_name
            FROM wishlist w
            JOIN products p ON w.product_id = p.id
            JOIN shops s ON p.shop_id = s.id
            WHERE w.user_id = ?
            ORDER BY w.created_at DESC
        ');
        $stmt->execute([$userId]);
        $items = $stmt->fetchAll();

        foreach ($items as &$item) {
            $item['id'] = (int)$item['id'];
            $item['product_id'] = (int)$item['product_id'];
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
        if ($product_id <= 0) json(400, ['error' => 'product_id requis']);

        // Check product exists
        $stmt = $db->prepare('SELECT id FROM products WHERE id = ? AND is_active = 1');
        $stmt->execute([$product_id]);
        if (!$stmt->fetch()) json(404, ['error' => 'Produit non trouvé']);

        // Insert or ignore (no duplicates)
        $stmt = $db->prepare('INSERT IGNORE INTO wishlist (user_id, product_id) VALUES (?, ?)');
        $stmt->execute([$userId, $product_id]);

        json(200, ['message' => 'Ajouté aux favoris']);
    }

    if ($method === 'DELETE') {
        $product_id = isset($_GET['product_id']) ? (int)$_GET['product_id'] : 0;
        if ($product_id <= 0) json(400, ['error' => 'product_id requis']);

        $stmt = $db->prepare('DELETE FROM wishlist WHERE user_id = ? AND product_id = ?');
        $stmt->execute([$userId, $product_id]);

        json(200, ['message' => 'Retiré des favoris']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
