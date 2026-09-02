<?php
/**
 * Reel comments list API endpoint.
 *
 * GET /reels/comments.php?reel_id=X → array of comments
 * [{"id":1,"user_id":2,"user_name":"...","text":"...","created_at":"..."}]
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

    if ($method === 'GET') {
        $reelId = isset($_GET['reel_id']) ? (int)$_GET['reel_id'] : 0;
        if ($reelId <= 0) json(400, ['error' => 'reel_id requis']);

        $stmt = $db->prepare("
            SELECT rc.id, rc.user_id, u.name AS user_name, rc.text, rc.created_at
            FROM reel_comments rc
            JOIN users u ON rc.user_id = u.id
            WHERE rc.reel_id = ?
            ORDER BY rc.created_at ASC
        ");
        $stmt->execute([$reelId]);
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