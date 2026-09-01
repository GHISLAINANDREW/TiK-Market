<?php
/**
 * Live comments list API endpoint.
 *
 * GET /live/comments.php?stream_id=X → array of comments
 * [{"id":1,"user_id":2,"user_name":"...","text":"...","created_at":"..."}]
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

    if ($method === 'GET') {
        $streamId = isset($_GET['stream_id']) ? (int)$_GET['stream_id'] : 0;
        if ($streamId <= 0) json(400, ['error' => 'stream_id requis']);

        $stmt = $db->prepare("
            SELECT lc.id, lc.user_id, u.name AS user_name, lc.text, lc.created_at
            FROM live_comments lc
            JOIN users u ON lc.user_id = u.id
            WHERE lc.stream_id = ?
            ORDER BY lc.created_at ASC
        ");
        $stmt->execute([$streamId]);
        $comments = $stmt->fetchAll();

        foreach ($comments as &$c) {
            $c['id'] = (int)$c['id'];
            $c['user_id'] = (int)$c['user_id'];
        }
        unset($c);

        json(200, $comments);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
