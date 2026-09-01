<?php
/**
 * Like/unlike a reel API endpoint.
 *
 * POST /reels/like.php  body: {"reel_id": 1}
 * → {"success": true, "liked": true, "like_count": 5}
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

    if ($method === 'POST') {
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $reelId = (int)($input['reel_id'] ?? 0);
        if ($reelId <= 0) json(400, ['error' => 'reel_id requis']);

        // Verify reel exists
        $stmt = $db->prepare("SELECT id, like_count FROM reels WHERE id = ?");
        $stmt->execute([$reelId]);
        $reel = $stmt->fetch();
        if (!$reel) json(404, ['error' => 'Reel introuvable']);

        // Check if already liked
        $stmt = $db->prepare("SELECT id FROM reel_likes WHERE reel_id = ? AND user_id = ?");
        $stmt->execute([$reelId, $userId]);
        $existing = $stmt->fetch();

        if ($existing) {
            // Unlike
            $db->prepare("DELETE FROM reel_likes WHERE id = ?")->execute([(int)$existing['id']]);
            $newCount = max(0, (int)$reel['like_count'] - 1);
            $db->prepare("UPDATE reels SET like_count = ? WHERE id = ?")->execute([$newCount, $reelId]);
            json(200, ['success' => true, 'liked' => false, 'like_count' => $newCount]);
        } else {
            // Like
            $db->prepare("INSERT INTO reel_likes (reel_id, user_id) VALUES (?, ?)")->execute([$reelId, $userId]);
            $newCount = (int)$reel['like_count'] + 1;
            $db->prepare("UPDATE reels SET like_count = ? WHERE id = ?")->execute([$newCount, $reelId]);

            // Notify the shop owner
            $stmt = $db->prepare("SELECT r.shop_id, s.vendor_id FROM reels r JOIN shops s ON r.shop_id = s.id WHERE r.id = ?");
            $stmt->execute([$reelId]);
            $owner = $stmt->fetch();
            if ($owner && (int)$owner['vendor_id'] !== $userId) {
                $stmtU = $db->prepare("SELECT name FROM users WHERE id = ?");
                $stmtU->execute([$userId]);
                $uname = $stmtU->fetchColumn();
                sendNotification((int)$owner['vendor_id'], "Nouveau like sur votre reel ❤️", ($uname ?: 'Quelqu\'un') . ' a aimé votre reel.', 'reel', $reelId);
            }

            json(200, ['success' => true, 'liked' => true, 'like_count' => $newCount]);
        }
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
