<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$id = isset($_GET['id']) ? (int)$_GET['id'] : null;

try {
    $db = getDB();

    // Auto-migration: ensure is_story column exists
    try {
        $check = $db->query("SHOW COLUMNS FROM products LIKE 'is_story'")->fetch();
        if (!$check) {
            $db->exec("ALTER TABLE products ADD COLUMN is_story TINYINT(1) DEFAULT 0 AFTER total_sales");
        }
    } catch (Exception $e) {
        error_log("Migration error (is_story): " . $e->getMessage());
    }

    if ($method === 'GET') {
        if ($id) {
            $stmt = $db->prepare("
                SELECT p.*, s.name AS shop_name, s.phone AS shop_phone, s.location AS shop_location, s.vendor_id, s.is_verified
                FROM products p
                JOIN shops s ON p.shop_id = s.id
                JOIN users u ON s.vendor_id = u.id
                WHERE p.id = ? AND p.is_active = 1 AND s.status = 'active' AND u.status = 'active'
            ");
            $stmt->execute([$id]);
            $product = $stmt->fetch();

            if (!$product) json(404, ['error' => 'Produit non trouvé']);

            $stmt = $db->prepare('
                SELECT r.id, r.rating, r.comment, r.created_at, u.name AS user_name
                FROM reviews r
                JOIN users u ON r.user_id = u.id
                WHERE r.product_id = ?
                ORDER BY r.created_at DESC
            ');
            $stmt->execute([$id]);
            $reviews = $stmt->fetchAll();

            $avgRating = 0;
            if (count($reviews) > 0) {
                $avgRating = round(array_sum(array_column($reviews, 'rating')) / count($reviews), 1);
            }

            $product['rating'] = $avgRating;
            $product['total_reviews'] = count($reviews);
            $product['reviews'] = $reviews;
            $product['id'] = (int)$product['id'];
            $product['shop_id'] = (int)$product['shop_id'];
            $product['vendor_id'] = (int)$product['vendor_id'];
            $product['price'] = (float)$product['price'];
            if ($product['compare_price']) $product['compare_price'] = (float)$product['compare_price'];
            $product['stock'] = (int)$product['stock'];
            $product['total_sales'] = (int)$product['total_sales'];
            $product['is_active'] = (bool)$product['is_active'];
            $product['is_verified'] = (bool)$product['is_verified'];
            $product['is_story'] = (bool)($product['is_story'] ?? false);
            json(200, $product);
        }

        $category = $_GET['category'] ?? '';
        $search = $_GET['search'] ?? '';
        $shop_id = isset($_GET['shop_id']) ? (int)$_GET['shop_id'] : 0;
        $min_price = isset($_GET['min_price']) ? (float)$_GET['min_price'] : 0;
        $max_price = isset($_GET['max_price']) ? (float)$_GET['max_price'] : 0;
        $page = max(1, (int)($_GET['page'] ?? 1));
        $limit = max(1, min(100, (int)($_GET['limit'] ?? 20)));
        $offset = ($page - 1) * $limit;
        $sort_by = $_GET['sort_by'] ?? 'newest';

        $where = ['p.is_active = 1', "s.status = 'active'", "u.status = 'active'"];

        // Stories are always visible as products; filtering for stories section is done client-side

        $params = [];

        if ($category !== '') {
            $where[] = 'p.category = ?';
            $params[] = $category;
        }
        if ($search !== '') {
            $where[] = '(p.title LIKE ? OR p.description LIKE ?)';
            $params[] = "%$search%";
            $params[] = "%$search%";
        }
        if ($shop_id > 0) {
            $where[] = 'p.shop_id = ?';
            $params[] = $shop_id;
        }
        if ($min_price > 0) {
            $where[] = 'p.price >= ?';
            $params[] = $min_price;
        }
        if ($max_price > 0) {
            $where[] = 'p.price <= ?';
            $params[] = $max_price;
        }

        $whereClause = implode(' AND ', $where);

        $countStmt = $db->prepare("SELECT COUNT(*) FROM products p JOIN shops s ON p.shop_id = s.id JOIN users u ON s.vendor_id = u.id WHERE $whereClause");
        $countStmt->execute($params);
        $total = (int)$countStmt->fetchColumn();

        $orderMap = [
            'newest'    => 'p.created_at DESC',
            'oldest'    => 'p.created_at ASC',
            'price_asc' => 'p.price ASC',
            'price_desc'=> 'p.price DESC',
            'rating'    => 'rating DESC, p.total_sales DESC',
            'popular'   => 'p.total_sales DESC, rating DESC',
            'name_asc'  => 'p.title ASC',
            'name_desc' => 'p.title DESC',
        ];
        $orderClause = $orderMap[$sort_by] ?? 'p.created_at DESC';

        $stmt = $db->prepare("
            SELECT p.*, s.name AS shop_name, s.vendor_id, s.is_verified,
                COALESCE((SELECT AVG(r.rating) FROM reviews r WHERE r.product_id = p.id), 0) AS rating
            FROM products p
            JOIN shops s ON p.shop_id = s.id
            JOIN users u ON s.vendor_id = u.id
            WHERE $whereClause
            ORDER BY $orderClause
            LIMIT $limit OFFSET $offset
        ");
        $stmt->execute($params);
        $products = $stmt->fetchAll();

        foreach ($products as &$p) {
            $p['id'] = (int)$p['id'];
            $p['shop_id'] = (int)$p['shop_id'];
            $p['vendor_id'] = (int)$p['vendor_id'];
            $p['price'] = (float)$p['price'];
            if ($p['compare_price']) $p['compare_price'] = (float)$p['compare_price'];
            $p['stock'] = (int)$p['stock'];
            $p['rating'] = (float)$p['rating'];
            $p['total_sales'] = (int)$p['total_sales'];
            $p['total_reviews'] = (int)$p['total_reviews'];
            $p['is_active'] = (bool)$p['is_active'];
            $p['is_verified'] = (bool)$p['is_verified'];
            $p['is_story'] = (bool)($p['is_story'] ?? false);
        }
        unset($p);

        json(200, [
            'products' => $products,
            'pagination' => [
                'page' => $page,
                'limit' => $limit,
                'total' => $total,
                'pages' => (int)ceil($total / $limit)
            ]
        ]);
    }

    if ($method === 'POST') {
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $shop_id = (int)($input['shop_id'] ?? 0);
        $title = trim($input['title'] ?? '');
        $description = trim($input['description'] ?? '');
        $price = $input['price'] ?? null;
        $compare_price = $input['compare_price'] ?? null;
        $category = trim($input['category'] ?? '');
        $image_url = trim($input['image_url'] ?? '');
        $stock = (int)($input['stock'] ?? 0);
        $unit = trim($input['unit'] ?? 'pièce');

        // Handle boolean or string or int for is_story
        $is_story_input = $input['is_story'] ?? false;
        $is_story = ($is_story_input === true || $is_story_input === 1 || $is_story_input === 'true') ? 1 : 0;

        if ($shop_id <= 0 || $title === '' || $price === null || $stock < 0) {
            json(400, ['error' => 'Champs obligatoires manquants (shop_id, title, price, stock)']);
        }

        $stmt = $db->prepare('SELECT id FROM shops WHERE id = ? AND vendor_id = ?');
        $stmt->execute([$shop_id, $userId]);
        if (!$stmt->fetch()) json(403, ['error' => 'Vous n\'êtes pas le propriétaire de cette boutique']);

        $stmt = $db->prepare('INSERT INTO products (shop_id, title, description, price, compare_price, category, image_url, stock, unit, is_story) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)');
        $stmt->execute([$shop_id, $title, $description, $price, $compare_price ?: null, $category, $image_url, $stock, $unit, $is_story]);
        $productId = (int)$db->lastInsertId();

        // Notifier les abonnés de la boutique
        $notifTitle = "Nouveau produit !";
        $notifMsg = "'$title' vient d'être ajouté dans la boutique.";
        $stmtSubs = $db->prepare('SELECT DISTINCT user_id FROM shop_favorites WHERE shop_id = ?');
        $stmtSubs->execute([$shop_id]);
        $subscribers = $stmtSubs->fetchAll();
        if (!empty($subscribers)) {
            $stmtNotif = $db->prepare('INSERT INTO notifications (user_id, title, message, type, related_id) VALUES (?, ?, ?, "product", ?)');
            foreach ($subscribers as $sub) {
                $stmtNotif->execute([(int)$sub['user_id'], $notifTitle, $notifMsg, $productId]);
            }
        }

        // Return the full product
        $stmt = $db->prepare('SELECT p.*, s.name AS shop_name FROM products p JOIN shops s ON p.shop_id = s.id WHERE p.id = ?');
        $stmt->execute([$productId]);
        $product = $stmt->fetch();
        $product['id'] = (int)$product['id'];
        $product['shop_id'] = (int)$product['shop_id'];
        $product['price'] = (float)$product['price'];
        if ($product['compare_price']) $product['compare_price'] = (float)$product['compare_price'];
        $product['stock'] = (int)$product['stock'];
        $product['rating'] = 0.0;
        $product['total_reviews'] = 0;
        $product['total_sales'] = 0;
        $product['is_story'] = (bool)$product['is_story'];

        json(201, $product);
    }

    if ($method === 'PUT') {
        if (!$id) json(400, ['error' => 'ID requis']);
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $stmt = $db->prepare('SELECT p.id FROM products p JOIN shops s ON p.shop_id = s.id WHERE p.id = ? AND s.vendor_id = ?');
        $stmt->execute([$id, $userId]);
        if (!$stmt->fetch()) json(403, ['error' => 'Non autorisé']);

        $fields = [];
        $params = [];
        foreach (['title', 'description', 'price', 'compare_price', 'category', 'image_url', 'stock', 'unit', 'is_story'] as $f) {
            if (array_key_exists($f, $input)) {
                $fields[] = "$f = ?";
                if ($f === 'is_story') {
                    $val = $input[$f];
                    $params[] = ($val === true || $val === 1 || $val === 'true') ? 1 : 0;
                } elseif ($f === 'compare_price') {
                    $params[] = $input[$f] ?: null;
                } else {
                    $params[] = $input[$f];
                }
            }
        }
        if (empty($fields)) json(400, ['error' => 'Aucun champ à mettre à jour']);

        $params[] = $id;
        $stmt = $db->prepare('UPDATE products SET ' . implode(', ', $fields) . ' WHERE id = ?');
        $stmt->execute($params);

        json(200, ['message' => 'Produit mis à jour']);
    }

    if ($method === 'DELETE') {
        if (!$id) json(400, ['error' => 'ID requis']);
        $userId = getAuthUserId();

        $stmt = $db->prepare('SELECT p.id FROM products p JOIN shops s ON p.shop_id = s.id WHERE p.id = ? AND s.vendor_id = ?');
        $stmt->execute([$id, $userId]);
        if (!$stmt->fetch()) json(403, ['error' => 'Non autorisé']);

        $stmt = $db->prepare('UPDATE products SET is_active = 0 WHERE id = ?');
        $stmt->execute([$id]);

        json(200, ['message' => 'Produit désactivé']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur: ' . $e->getMessage()]);
} catch (\Throwable $e) {
    json(500, ['error' => 'Erreur serveur: ' . $e->getMessage()]);
}
