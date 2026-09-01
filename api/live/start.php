<?php
/**
 * Start a live stream API endpoint.
 *
 * POST /live/start.php  body: {"title": "...", "pinned_product_id": 123}
 * → {"success": true, "message": "...", "stream_id": 1, "stream_url": "..."}
 *
 * Only vendors (or admins) can start a live stream for their shop.
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // ── Auto-migration: create live_streams table ──
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS live_streams (
            id INT AUTO_INCREMENT PRIMARY KEY,
            shop_id INT NOT NULL,
            title VARCHAR(200) NOT NULL,
            stream_url VARCHAR(500) DEFAULT '',
            viewer_count INT DEFAULT 0,
            is_live TINYINT(1) DEFAULT 1,
            pinned_product_id INT DEFAULT NULL,
            started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            ended_at TIMESTAMP NULL DEFAULT NULL,
            FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
            INDEX idx_live_active (is_live, started_at)
        )");
    } catch (Exception $e) { error_log("Migration live_streams table: " . $e->getMessage()); }

    if ($method === 'POST') {
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $title = trim($input['title'] ?? '');
        $pinnedProductId = isset($input['pinned_product_id']) ? (int)$input['pinned_product_id'] : null;

        if ($title === '') json(400, ['error' => 'Titre requis']);

        // Determine the shop: vendor's own shop, or admin's first shop
        $stmt = $db->prepare("SELECT role FROM users WHERE id = ?");
        $stmt->execute([$userId]);
        $user = $stmt->fetch();
        $role = $user['role'] ?? 'buyer';

        $shopId = 0;
        if (in_array($role, ['admin', 'super_admin'])) {
            // Admin: pick the first active shop (or allow shop_id in body)
            $shopId = isset($input['shop_id']) ? (int)$input['shop_id'] : 0;
            if ($shopId <= 0) {
                $stmt = $db->query("SELECT id FROM shops WHERE status = 'active' ORDER BY id LIMIT 1");
                $shopId = (int)$stmt->fetchColumn();
            }
        } else {
            // Vendor: must own the shop
            $shopId = isset($input['shop_id']) ? (int)$input['shop_id'] : 0;
            if ($shopId <= 0) {
                $stmt = $db->prepare("SELECT id FROM shops WHERE vendor_id = ? AND status = 'active' ORDER BY id LIMIT 1");
                $stmt->execute([$userId]);
                $shopId = (int)$stmt->fetchColumn();
            } else {
                $stmt = $db->prepare("SELECT id FROM shops WHERE id = ? AND vendor_id = ?");
                $stmt->execute([$shopId, $userId]);
                if (!$stmt->fetch()) json(403, ['error' => 'Vous n\'êtes pas le propriétaire de cette boutique']);
            }
        }

        if ($shopId <= 0) json(400, ['error' => 'Aucune boutique disponible pour lancer un direct']);

        // Stop any existing live stream for this shop
        $db->prepare("UPDATE live_streams SET is_live = 0, ended_at = NOW() WHERE shop_id = ? AND is_live = 1")
            ->execute([$shopId]);

        // Build a stream URL (placeholder; real streaming would use a CDN/RTMP)
        $streamUrl = getenv('APP_URL') ?: 'https://tik-market.onrender.com';
        $streamUrl .= '/live/stream_' . $shopId . '_' . time() . '.mp4';

        $stmt = $db->prepare("INSERT INTO live_streams (shop_id, title, stream_url, pinned_product_id) VALUES (?, ?, ?, ?)");
        $stmt->execute([$shopId, $title, $streamUrl, $pinnedProductId]);
        $streamId = (int)$db->lastInsertId();

        json(201, [
            'success' => true,
            'message' => 'Direct lancé',
            'stream_id' => $streamId,
            'stream_url' => $streamUrl
        ]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
