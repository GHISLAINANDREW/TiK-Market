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
                    COALESCE(SUM(p.total_sales), 0) AS total_sales,
                    COALESCE((
                        SELECT AVG(r.rating)
                        FROM reviews r
                        JOIN products p2 ON r.product_id = p2.id
                        WHERE p2.shop_id = s.id
                    ), 0) AS rating
                FROM shops s
                LEFT JOIN products p ON p.shop_id = s.id AND p.is_active = 1
                WHERE s.id = ?
                GROUP BY s.id
            ');
            $stmt->execute([$id]);
            $shop = $stmt->fetch();

            if (!$shop) json(404, ['error' => 'Boutique non trouvée']);

            $stmt = $db->prepare('
                SELECT id, title, price, compare_price, category, image_url, stock, unit,
                    COALESCE((SELECT AVG(rating) FROM reviews WHERE product_id = products.id), 0) as rating,
                    (SELECT COUNT(*) FROM reviews WHERE product_id = products.id) as total_reviews,
                    total_sales, created_at
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
            $shop['rating'] = (float)$shop['rating'];
            $shop['is_verified'] = (bool)$shop['is_verified'];
            $shop['is_featured'] = (bool)($shop['is_featured'] ?? false);
            $shop['products'] = $products;

            json(200, ['shop' => $shop]);
        }

        if ($vendor_id > 0) {
            $stmt = $db->prepare('
                SELECT s.*,
                    COUNT(p.id) AS product_count,
                    COALESCE(SUM(p.total_sales), 0) AS total_sales,
                    COALESCE((
                        SELECT AVG(r.rating)
                        FROM reviews r
                        JOIN products p2 ON r.product_id = p2.id
                        WHERE p2.shop_id = s.id
                    ), 0) AS rating
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
            $shop['rating'] = (float)$shop['rating'];
            $shop['is_verified'] = (bool)$shop['is_verified'];
            $shop['is_featured'] = (bool)($shop['is_featured'] ?? false);

            json(200, ['shop' => $shop]);
        }

        $location = $_GET['location'] ?? '';
        $where = ["s.status = 'active'", "u.status = 'active'"];
        $params = [];
        if ($location !== '') {
            $where[] = 's.location LIKE ?';
            $params[] = "%$location%";
        }
        $whereClause = implode(' AND ', $where);

        $stmt = $db->prepare("
            SELECT s.*,
                COUNT(p.id) AS product_count,
                COALESCE(SUM(p.total_sales), 0) AS total_sales,
                COALESCE((
                    SELECT AVG(r.rating)
                    FROM reviews r
                    JOIN products p2 ON r.product_id = p2.id
                    WHERE p2.shop_id = s.id
                ), 0) AS rating
            FROM shops s
            JOIN users u ON s.vendor_id = u.id
            LEFT JOIN products p ON p.shop_id = s.id AND p.is_active = 1
            WHERE $whereClause
            GROUP BY s.id
            ORDER BY s.is_featured DESC, s.created_at DESC
        ");
        $stmt->execute($params);
        $shops = $stmt->fetchAll();

        foreach ($shops as &$s) {
            $s['id'] = (int)$s['id'];
            $s['vendor_id'] = (int)$s['vendor_id'];
            $s['product_count'] = (int)$s['product_count'];
            $s['total_sales'] = (int)$s['total_sales'];
            $s['rating'] = (float)$s['rating'];
            $s['is_verified'] = (bool)$s['is_verified'];
            $s['is_featured'] = (bool)($s['is_featured'] ?? false);
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
        $logo = trim($input['logo'] ?? '');
        $phone = trim($input['phone'] ?? '');
        $location = trim($input['location'] ?? '');
        $category = trim($input['category'] ?? '');

        if ($name === '' || $phone === '' || $location === '') {
            json(400, ['error' => 'Champs obligatoires manquants (name, phone, location)']);
        }

        $stmt = $db->prepare('INSERT INTO shops (vendor_id, name, description, logo, phone, location, category) VALUES (?, ?, ?, ?, ?, ?, ?)');
        $stmt->execute([$userId, $name, $description, $logo, $phone, $location, $category]);
        $shopId = (int)$db->lastInsertId();

        // Return the full shop object
        $stmt = $db->prepare('SELECT s.*,
            COUNT(p.id) AS product_count,
            COALESCE(SUM(p.total_sales), 0) AS total_sales,
            0.0 AS rating
            FROM shops s LEFT JOIN products p ON p.shop_id = s.id AND p.is_active = 1
            WHERE s.id = ? GROUP BY s.id');
        $stmt->execute([$shopId]);
        $shop = $stmt->fetch();
        $shop['id'] = (int)$shop['id'];
        $shop['vendor_id'] = (int)$shop['vendor_id'];
        $shop['product_count'] = (int)($shop['product_count'] ?? 0);
        $shop['total_sales'] = (int)($shop['total_sales'] ?? 0);
        $shop['rating'] = 0.0;
        $shop['is_verified'] = (bool)$shop['is_verified'];
        $shop['is_featured'] = (bool)($shop['is_featured'] ?? false);

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
        foreach (['name', 'description', 'logo', 'phone', 'location', 'category'] as $f) {
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

    if ($method === 'DELETE') {
        if (!$id) json(400, ['error' => 'ID requis']);
        $userId = getAuthUserId();

        $stmt = $db->prepare('SELECT vendor_id FROM shops WHERE id = ?');
        $stmt->execute([$id]);
        $shop = $stmt->fetch();
        if (!$shop) json(404, ['error' => 'Boutique non trouvée']);

        if ($shop['vendor_id'] != $userId) {
            // Check if user is admin
            $stmt = $db->prepare('SELECT role FROM users WHERE id = ?');
            $stmt->execute([$userId]);
            $userRole = $stmt->fetchColumn();
            if (!in_array($userRole, ['admin', 'super_admin'])) {
                json(403, ['error' => 'Non autorisé']);
            }
        }

        $stmt = $db->prepare('DELETE FROM shops WHERE id = ?');
        $stmt->execute([$id]);

        json(200, ['message' => 'Boutique supprimée avec succès']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
