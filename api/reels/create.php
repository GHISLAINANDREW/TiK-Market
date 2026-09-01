<?php
/**
 * Create a reel API endpoint.
 *
 * POST /reels/create.php  body: {"shop_id":1,"video_url":"...","description":"...","product_id":123}
 * → {"success": true, "message": "...", "reel_id": 1}
 *
 * Only vendors (or admins) can create a reel for their shop.
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // ── Auto-migration: create reels table ──
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS reels (
            id INT AUTO_INCREMENT PRIMARY KEY,
            shop_id INT NOT NULL,
            video_url VARCHAR(500) NOT NULL,
            description TEXT,
            product_id INT DEFAULT NULL,
            like_count INT DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
            INDEX idx_reels_created (created_at)
        )");
    } catch (Exception $e) { error_log("Migration reels table: " . $e->getMessage()); }

    if ($method === 'POST') {
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $shopId = (int)($input['shop_id'] ?? 0);
        $videoUrl = trim($input['video_url'] ?? '');
        $description = trim($input['description'] ?? '');
        $productId = isset($input['product_id']) ? (int)$input['product_id'] : null;

        if ($shopId <= 0 || $videoUrl === '') {
            json(400, ['error' => 'shop_id et video_url requis']);
        }

        // Verify ownership
        $stmt = $db->prepare("SELECT role FROM users WHERE id = ?");
        $stmt->execute([$userId]);
        $user = $stmt->fetch();
        $role = $user['role'] ?? 'buyer';

        if (!in_array($role, ['admin', 'super_admin'])) {
            $stmt = $db->prepare("SELECT id FROM shops WHERE id = ? AND vendor_id = ?");
            $stmt->execute([$shopId, $userId]);
            if (!$stmt->fetch()) json(403, ['error' => 'Vous n\'êtes pas le propriétaire de cette boutique']);
        }

        $stmt = $db->prepare("INSERT INTO reels (shop_id, video_url, description, product_id) VALUES (?, ?, ?, ?)");
        $stmt->execute([$shopId, $videoUrl, $description ?: null, $productId]);
        $reelId = (int)$db->lastInsertId();

        json(201, ['success' => true, 'message' => 'Reel créé', 'reel_id' => $reelId]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
