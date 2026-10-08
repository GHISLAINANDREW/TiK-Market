<?php
/**
 * Like a live comment API endpoint.
 *
 * POST /live/comment-like.php  body: {"stream_id": 1, "comment_id": 1}
 * → {"success": true, "likes_count": 5}
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // ── Auto-migration: create live_comment_likes table ──
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS live_comment_likes (
            id INT AUTO_INCREMENT PRIMARY KEY,
            stream_id INT NOT NULL,
            comment_id INT NOT NULL,
            user_id INT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY unique_like (stream_id, comment_id, user_id),
            FOREIGN KEY (stream_id) REFERENCES live_streams(id) ON DELETE CASCADE,
            FOREIGN KEY (comment_id) REFERENCES live_comments(id) ON DELETE CASCADE,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            INDEX idx_comment_likes (comment_id)
        )");
    } catch (Exception $e) { error_log("Migration live_comment_likes table: " . $e->getMessage()); }

    if ($method === 'POST') {
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $streamId = (int)($input['stream_id'] ?? 0);
        $commentId = (int)($input['comment_id'] ?? 0);

        if ($streamId <= 0) json(400, ['error' => 'stream_id requis']);
        if ($commentId <= 0) json(400, ['error' => 'comment_id requis']);

        // Verify comment exists and belongs to this stream
        $stmt = $db->prepare("SELECT id FROM live_comments WHERE id = ? AND stream_id = ?");
        $stmt->execute([$commentId, $streamId]);
        if (!$stmt->fetch()) json(404, ['error' => 'Commentaire introuvable']);

        // Try to insert the like (UNIQUE key prevents duplicates)
        $stmt = $db->prepare("
            INSERT IGNORE INTO live_comment_likes (stream_id, comment_id, user_id)
            VALUES (?, ?, ?)
        ");
        $stmt->execute([$streamId, $commentId, $userId]);

        // Get the number of likes for this comment
        $stmt = $db->prepare("
            SELECT COUNT(*) FROM live_comment_likes
            WHERE stream_id = ? AND comment_id = ?
        ");
        $stmt->execute([$streamId, $commentId]);
        $likesCount = (int)$stmt->fetchColumn();

        // Check if the current user liked it
        $stmt = $db->prepare("
            SELECT COUNT(*) FROM live_comment_likes
            WHERE stream_id = ? AND comment_id = ? AND user_id = ?
        ");
        $stmt->execute([$streamId, $commentId, $userId]);
        $likedByMe = (int)$stmt->fetchColumn() > 0;

        json(200, [
            'success' => true,
            'likes_count' => $likesCount,
            'liked_by_me' => $likedByMe
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
