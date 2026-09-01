<?php
/**
 * Reels list API endpoint.
 *
 * GET /reels/list.php → {"reels": [...]}
 * Each reel: {id, shop_id, shop_name, shop_logo, video_url, description,
 *             like_count, is_liked, created_at}
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // ── Auto-migration: create reels + reel_likes tables ──
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

    try {
        $db->exec("CREATE TABLE IF NOT EXISTS reel_likes (
            id INT AUTO_INCREMENT PRIMARY KEY,
            reel_id INT NOT NULL,
            user_id INT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY uq_reel_user (reel_id, user_id),
            FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )");
    } catch (Exception $e) { error_log("Migration reel_likes table: " . $e->getMessage()); }

    if ($method === 'GET') {
        $shopId = isset($_GET['shop_id']) ? (int)$_GET['shop_id'] : 0;
        $userId = 0;
        // Try to get the current user for is_liked (optional auth)
        try { $userId = getAuthUserId(); } catch (Exception $e) { $userId = 0; }

        if ($shopId > 0) {
            $stmt = $db->prepare("
                SELECT r.*, s.name AS shop_name, s.logo AS shop_logo
                FROM reels r
                JOIN shops s ON r.shop_id = s.id
                WHERE r.shop_id = ?
                ORDER BY r.created_at DESC
            ");
            $stmt->execute([$shopId]);
            $reels = $stmt->fetchAll();
        } else {
            $stmt = $db->query("
                SELECT r.*, s.name AS shop_name, s.logo AS shop_logo
                FROM reels r
                JOIN shops s ON r.shop_id = s.id
                ORDER BY r.created_at DESC
            ");
            $reels = $stmt->fetchAll();
        }

        // Fetch liked reel ids for the current user
        $likedIds = [];
        if ($userId > 0 && !empty($reels)) {
            $ids = array_column($reels, 'id');
            $placeholders = implode(',', array_fill(0, count($ids), '?'));
            $stmtL = $db->prepare("SELECT reel_id FROM reel_likes WHERE user_id = ? AND reel_id IN ($placeholders)");
            $params = array_merge([$userId], $ids);
            $stmtL->execute($params);
            $likedIds = array_map('intval', $stmtL->fetchAll(PDO::FETCH_COLUMN));
        }

        foreach ($reels as &$r) {
            $r['id'] = (int)$r['id'];
            $r['shop_id'] = (int)$r['shop_id'];
            $r['like_count'] = (int)$r['like_count'];
            $r['is_liked'] = in_array((int)$r['id'], $likedIds);
        }
        unset($r);

        json(200, ['reels' => $reels]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
