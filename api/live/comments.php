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

        // Pagination
        $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;
        $offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;
        if ($limit < 1) $limit = 50;
        if ($limit > 200) $limit = 200;
        if ($offset < 0) $offset = 0;

        // Verify stream exists and is live
        $stmtCheck = $db->prepare("SELECT id FROM live_streams WHERE id = ? AND is_live = 1");
        $stmtCheck->execute([$streamId]);
        if (!$stmtCheck->fetch()) json(404, ['error' => 'Direct introuvable ou termine']);

        // Get comments with likes aggregation
        $stmt = $db->prepare("
            SELECT lc.id, lc.user_id, u.name AS user_name, lc.text, lc.created_at,
                   COALESCE(lcl.likes_count, 0) AS likes_count,
                   CASE WHEN lcl.user_liked = 1 THEN 1 ELSE 0 END AS liked_by_me
            FROM live_comments lc
            JOIN users u ON lc.user_id = u.id
            LEFT JOIN (
                SELECT comment_id, COUNT(*) AS likes_count, MAX(CASE WHEN user_id = ? THEN 1 ELSE 0 END) AS user_liked
                FROM live_comment_likes
                WHERE stream_id = ?
                GROUP BY comment_id
            ) lcl ON lcl.comment_id = lc.id
            WHERE lc.stream_id = ?
            ORDER BY lc.created_at DESC
            LIMIT ? OFFSET ?
        ");
        $stmt->execute([$userId, $streamId, $streamId, $limit, $offset]);
        $comments = $stmt->fetchAll();

        foreach ($comments as &$c) {
            $c['id'] = (int)$c['id'];
            $c['user_id'] = (int)$c['user_id'];
            $c['likes_count'] = (int)$c['likes_count'];
            $c['liked_by_me'] = (bool)$c['liked_by_me'];
        }
        unset($c);

        json(200, ['comments' => $comments, 'count' => count($comments), 'limit' => $limit, 'offset' => $offset]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
