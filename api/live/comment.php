<?php
/**
 * Post a live comment API endpoint.
 *
 * POST /live/comment.php  body: {"stream_id": 1, "text": "..."}
 * → {"success": true, "comment_id": 1}
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // ── Auto-migration: create live_comments table ──
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS live_comments (
            id INT AUTO_INCREMENT PRIMARY KEY,
            stream_id INT NOT NULL,
            user_id INT NOT NULL,
            text TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (stream_id) REFERENCES live_streams(id) ON DELETE CASCADE,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            INDEX idx_live_comments_stream (stream_id, created_at)
        )");
    } catch (Exception $e) { error_log("Migration live_comments table: " . $e->getMessage()); }

    if ($method === 'POST') {
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $streamId = (int)($input['stream_id'] ?? 0);
        $text = trim($input['text'] ?? '');

        if ($streamId <= 0) json(400, ['error' => 'stream_id requis']);
        if ($text === '') json(400, ['error' => 'Texte requis']);

        // Verify stream exists and is live
        $stmt = $db->prepare("SELECT id FROM live_streams WHERE id = ? AND is_live = 1");
        $stmt->execute([$streamId]);
        if (!$stmt->fetch()) json(404, ['error' => 'Direct introuvable ou terminé']);

        $stmt = $db->prepare("INSERT INTO live_comments (stream_id, user_id, text) VALUES (?, ?, ?)");
        $stmt->execute([$streamId, $userId, $text]);
        $commentId = (int)$db->lastInsertId();

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
