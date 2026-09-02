<?php
/**
 * Post a reel comment API endpoint.
 *
 * POST /reels/comment.php  body: {"reel_id": 1, "text": "..."}
 * → {"success": true, "comment_id": 1}
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // ── Auto-migration: create reel_comments table ──
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS reel_comments (
            id INT AUTO_INCREMENT PRIMARY KEY,
            reel_id INT NOT NULL,
            user_id INT NOT NULL,
            text TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            INDEX idx_reel_comments_reel (reel_id, created_at)
        )");
    } catch (Exception $e) { error_log("Migration reel_comments table: " . $e->getMessage()); }

    if ($method === 'POST') {
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $reelId = (int)($input['reel_id'] ?? 0);
        $text = trim($input['text'] ?? '');

        if ($reelId <= 0) json(400, ['error' => 'reel_id requis']);
        if ($text === '') json(400, ['error' => 'Texte requis']);

        // Verify reel exists
        $stmt = $db->prepare("SELECT id FROM reels WHERE id = ?");
        $stmt->execute([$reelId]);
        if (!$stmt->fetch()) json(404, ['error' => 'Reel introuvable']);

        $stmt = $db->prepare("INSERT INTO reel_comments (reel_id, user_id, text) VALUES (?, ?, ?)");
        $stmt->execute([$reelId, $userId, $text]);
        $commentId = (int)$db->lastInsertId();

        // Notify the shop owner
        try {
            $stmt = $db->prepare("SELECT r.shop_id, s.vendor_id FROM reels r JOIN shops s ON r.shop_id = s.id WHERE r.id = ?");
            $stmt->execute([$reelId]);
            $owner = $stmt->fetch();
            if ($owner && (int)$owner['vendor_id'] !== $userId) {
                $stmtU = $db->prepare("SELECT name FROM users WHERE id = ?");
                $stmtU->execute([$userId]);
                $uname = $stmtU->fetchColumn();
                sendNotification((int)$owner['vendor_id'], "Nouveau commentaire sur votre reel 💬", ($uname ?: 'Quelqu\'un') . ' a commenté votre reel.', 'reel', $reelId);
            }
        } catch (Exception $e) {}

        json(201, ['success' => true, 'comment_id' => $commentId]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}