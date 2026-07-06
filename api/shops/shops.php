<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$id = isset($_GET['id']) ? (int)$_GET['id'] : null;
$vendor_id = isset($_GET['vendor_id']) ? (int)$_GET['vendor_id'] : 0;

try {
    $db = getDB();

    if ($method === 'GET') {
        if ($id) {
            $stmt = $db->prepare('
                SELECT s.*,
                    COUNT(p.id) AS product_count,
                    COALESCE(SUM(p.total_sales), 0) AS total_sales
                FROM shops s
                LEFT JOIN products p ON p.shop_id = s.id AND p.is_active = 1
                WHERE s.id = ?
                GROUP BY s.id
            ');
            $stmt->execute([$id]);
            $shop = $stmt->fetch();

            if (!$shop) json(404, ['error' => 'Boutique non trouvée']);

            $stmt = $db->prepare('
                SELECT id, title, price, compare_price, category, image_url, stock, unit, rating, total_reviews, total_sales, created_at
                FROM products
                WHERE shop_id = ? AND is_active = 1
                ORDER BY created_at DESC
                LIMIT 10
            ');
            $stmt->execute([$id]);
            $products = $stmt->fetchAll();

            foreach ($products as &$p) {
                $p['id'] = (int)$p['id'];
                $p['price'] = (float)$p['price'];
                if ($p['compare_price']) $p['compare_price'] = (float)$p['compare_price'];
                $p['stock'] = (int)$p['stock'];
                $p['rating'] = (float)$p['rating'];
                $p['total_sales'] = (int)$p['total_sales'];
                $p['total_reviews'] = (int)$p['total_reviews'];
            }
            unset($p);

            $shop['id'] = (int)$shop['id'];
            $shop['vendor_id'] = (int)$shop['vendor_id'];
            $shop['product_count'] = (int)$shop['product_count'];
            $shop['total_sales'] = (int)$shop['total_sales'];
            $shop['is_verified'] = (bool)$shop['is_verified'];
            $shop['products'] = $products;

            json(200, ['shop' => $shop]);
        }

        if ($vendor_id > 0) {
            $stmt = $db->prepare('
                SELECT s.*,
                    COUNT(p.id) AS product_count,
                    COALESCE(SUM(p.total_sales), 0) AS total_sales
                FROM shops s
                LEFT JOIN products p ON p.shop_id = s.id AND p.is_active = 1
                WHERE s.vendor_id = ?
                GROUP BY s.id
            ');
            $stmt->execute([$vendor_id]);
            $shop = $stmt->fetch();

            if (!$shop) json(404, ['error' => 'Boutique non trouvée']);

            $shop['id'] = (int)$shop['id'];
            $shop['vendor_id'] = (int)$shop['vendor_id'];
            $shop['product_count'] = (int)$shop['product_count'];
            $shop['total_sales'] = (int)$shop['total_sales'];
            $shop['is_verified'] = (bool)$shop['is_verified'];

            json(200, ['shop' => $shop]);
        }

        // Pas d'ID ni vendor_id → retourner les boutiques actives (non bannies)
        $stmt = $db->query('
            SELECT s.*,
                COUNT(p.id) AS product_count,
                COALESCE(SUM(p.total_sales), 0) AS total_sales
            FROM shops s
            JOIN users u ON s.vendor_id = u.id
            LEFT JOIN products p ON p.shop_id = s.id AND p.is_active = 1
            WHERE s.status = "active" AND u.status = "active"
            GROUP BY s.id
            ORDER BY s.created_at DESC
        ');
        $shops = $stmt->fetchAll();

        foreach ($shops as &$s) {
            $s['id'] = (int)$s['id'];
            $s['vendor_id'] = (int)$s['vendor_id'];
            $s['product_count'] = (int)$s['product_count'];
            $s['total_sales'] = (int)$s['total_sales'];
            $s['is_verified'] = (bool)$s['is_verified'];
        }
        unset($s);

        json(200, $shops);
    }

    if ($method === 'POST') {
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $name = trim($input['name'] ?? '');
        $description = trim($input['description'] ?? '');
        $phone = trim($input['phone'] ?? '');
        $location = trim($input['location'] ?? '');
        $category = trim($input['category'] ?? '');

        if ($name === '' || $phone === '' || $location === '') {
            json(400, ['error' => 'Champs obligatoires manquants (name, phone, location)']);
        }

        $stmt = $db->prepare('INSERT INTO shops (vendor_id, name, description, phone, location, category) VALUES (?, ?, ?, ?, ?, ?)');
        $stmt->execute([$userId, $name, $description, $phone, $location, $category]);
        $shopId = (int)$db->lastInsertId();

        // Return the full shop object
        $stmt = $db->prepare('SELECT s.*, COUNT(p.id) AS product_count, COALESCE(SUM(p.total_sales), 0) AS total_sales
            FROM shops s LEFT JOIN products p ON p.shop_id = s.id AND p.is_active = 1
            WHERE s.id = ? GROUP BY s.id');
        $stmt->execute([$shopId]);
        $shop = $stmt->fetch();
        $shop['id'] = (int)$shop['id'];
        $shop['vendor_id'] = (int)$shop['vendor_id'];
        $shop['product_count'] = (int)($shop['product_count'] ?? 0);
        $shop['total_sales'] = (int)($shop['total_sales'] ?? 0);
        $shop['is_verified'] = (bool)$shop['is_verified'];

        json(201, ['shop' => $shop]);
    }

    if ($method === 'PUT') {
        if (!$id) json(400, ['error' => 'ID requis']);
        $userId = getAuthUserId();

        $stmt = $db->prepare('SELECT id FROM shops WHERE id = ? AND vendor_id = ?');
        $stmt->execute([$id, $userId]);
        if (!$stmt->fetch()) json(403, ['error' => 'Non autorisé']);

        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $fields = [];
        $params = [];
        foreach (['name', 'description', 'phone', 'location', 'category'] as $f) {
            if (array_key_exists($f, $input)) {
                $fields[] = "$f = ?";
                $params[] = $input[$f];
            }
        }
        if (empty($fields)) json(400, ['error' => 'Aucun champ à mettre à jour']);

        $params[] = $id;
        $stmt = $db->prepare('UPDATE shops SET ' . implode(', ', $fields) . ' WHERE id = ?');
        $stmt->execute($params);

        json(200, ['message' => 'Boutique mise à jour']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
