<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // Auto-migration for reviews table (development)
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS reviews (
            id INT AUTO_INCREMENT PRIMARY KEY,
            product_id INT NOT NULL,
            user_id INT NOT NULL,
            rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
            comment TEXT,
            image_url TEXT,
            useful_votes INT DEFAULT 0,
            vendor_reply TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY unique_review (product_id, user_id)
        )");
    } catch (Exception $e) {}
    // Auto-migration: add new columns if missing
    try { $db->exec("ALTER TABLE reviews ADD COLUMN image_url TEXT AFTER comment"); } catch (Exception $e) {}
    try { $db->exec("ALTER TABLE reviews ADD COLUMN useful_votes INT DEFAULT 0 AFTER image_url"); } catch (Exception $e) {}
    try { $db->exec("ALTER TABLE reviews ADD COLUMN vendor_reply TEXT AFTER useful_votes"); } catch (Exception $e) {}

    $userId = getAuthUserId();

    // GET: fetch reviews for a product (PUBLIC)
    if ($method === 'GET' && isset($_GET['product_id'])) {
        $productId = (int)$_GET['product_id'];
        $stmt = $db->prepare("
            SELECT r.*, u.name AS user_name, u.avatar AS user_avatar
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            WHERE r.product_id = ?
            ORDER BY r.created_at DESC
        ");
        $stmt->execute([$productId]);
        $reviews = $stmt->fetchAll();
        foreach ($reviews as &$r) {
            $r['id'] = (int)$r['id'];
            $r['user_id'] = (int)$r['user_id'];
            $r['rating'] = (int)$r['rating'];
            $r['useful_votes'] = (int)($r['useful_votes'] ?? 0);
        }
        unset($r);
        json(200, ['reviews' => $reviews]);
    }

    // GET: mark review as useful
    if ($method === 'POST' && isset($_GET['useful'])) {
        $reviewId = (int)$_GET['useful'];
        $stmt = $db->prepare('UPDATE reviews SET useful_votes = useful_votes + 1 WHERE id = ?');
        $stmt->execute([$reviewId]);
        json(200, ['message' => 'Merci pour votre vote !']);
    }

    // PUT: vendor reply to review
    if ($method === 'PUT' && isset($_GET['reply'])) {
        $reviewId = (int)$_GET['reply'];
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps invalide']);
        $reply = trim($input['reply'] ?? '');
        // Verify vendor owns the product
        $stmt = $db->prepare("
            SELECT r.id FROM reviews r
            JOIN products p ON r.product_id = p.id
            JOIN shops s ON p.shop_id = s.id
            WHERE r.id = ? AND s.vendor_id = ?
        ");
        $stmt->execute([$reviewId, $userId]);
        if (!$stmt->fetch()) json(403, ['error' => 'Non autorisé']);
        $stmt = $db->prepare('UPDATE reviews SET vendor_reply = ? WHERE id = ?');
        $stmt->execute([$reply, $reviewId]);
        json(200, ['message' => 'Réponse enregistrée']);
    }

    if ($method === 'POST') {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $product_id = (int)($input['product_id'] ?? 0);
        $rating = (int)($input['rating'] ?? 0);
        $comment = trim($input['comment'] ?? '');
        $image_url = trim($input['image_url'] ?? '');

        if ($product_id <= 0 || $rating < 1 || $rating > 5) {
            json(400, ['error' => 'product_id requis et rating entre 1 et 5']);
        }

        // Vérifier que le produit existe
        $stmt = $db->prepare('SELECT id FROM products WHERE id = ?');
        $stmt->execute([$product_id]);
        if (!$stmt->fetch()) json(404, ['error' => 'Produit non trouvé']);

        // Vérifier que l'utilisateur n'a pas déjà noté ce produit (upsert)
        $stmt = $db->prepare('SELECT id FROM reviews WHERE product_id = ? AND user_id = ?');
        $stmt->execute([$product_id, $userId]);
        $existing = $stmt->fetch();

        if ($existing) {
            // Mise à jour
            $stmt = $db->prepare('UPDATE reviews SET rating = ?, comment = ?, image_url = ? WHERE id = ?');
            $stmt->execute([$rating, $comment, $image_url ?: null, $existing['id']]);
        } else {
            // Création
            $stmt = $db->prepare('INSERT INTO reviews (product_id, user_id, rating, comment, image_url) VALUES (?, ?, ?, ?, ?)');
            $stmt->execute([$product_id, $userId, $rating, $comment, $image_url ?: null]);
        }

        json(200, ['message' => 'Avis enregistré']);

        // Notifier le vendeur du produit
        try {
            $stmtV = $db->prepare('SELECT s.vendor_id, p.title FROM products p JOIN shops s ON p.shop_id = s.id WHERE p.id = ?');
            $stmtV->execute([$product_id]);
            $prod = $stmtV->fetch();
            if ($prod && (int)$prod['vendor_id'] !== $userId) {
                $stars = str_repeat('⭐', $rating);
                sendNotification((int)$prod['vendor_id'], "Nouvel avis sur {$prod['title']}", "Votre produit a reçu $rating/5 : " . (mb_strlen($comment) > 100 ? mb_substr($comment, 0, 100) . '...' : $comment), 'product', $product_id);
            }
        } catch (Exception $e) {}
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
} catch (\Throwable $e) {
    json(500, ['error' => 'Erreur serveur']);
}
