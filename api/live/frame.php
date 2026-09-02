<?php
/**
 * Live stream frame upload / retrieval API endpoint.
 *
 * POST /live/frame.php?stream_id=1  body: {"frame": "<base64 jpeg>"}
 *   → {"success": true, "message": "Frame reçue"}
 *
 * GET /live/frame.php?stream_id=1
 *   → {"success": true, "frame": "<base64 jpeg>", "frame_at": "..."}  (200)
 *   → {"success": false, "message": "Aucune frame"}                    (404)
 *
 * This implements a simple frame-based live stream: the streamer uploads
 * JPEG frames periodically (~1 fps) and spectators poll for the latest one.
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // ── Auto-migration: create live_frames table ──
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS live_frames (
            stream_id INT NOT NULL,
            frame_data LONGBLOB NOT NULL,
            frame_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (stream_id),
            FOREIGN KEY (stream_id) REFERENCES live_streams(id) ON DELETE CASCADE
        )");
    } catch (Exception $e) { error_log("Migration live_frames table: " . $e->getMessage()); }

    $streamId = isset($_GET['stream_id']) ? (int)$_GET['stream_id'] : 0;
    if ($streamId <= 0) json(400, ['error' => 'stream_id requis']);

    if ($method === 'POST') {
        // Only the stream owner (vendor/admin) can upload frames.
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $frameB64 = $input['frame'] ?? '';
        if ($frameB64 === '') json(400, ['error' => 'Frame manquante']);

        // Verify the stream exists and belongs to the user's shop.
        $stmt = $db->prepare("
            SELECT ls.id, s.vendor_id, u.role
            FROM live_streams ls
            JOIN shops s ON ls.shop_id = s.id
            JOIN users u ON u.id = ?
            WHERE ls.id = ? AND ls.is_live = 1
        ");
        $stmt->execute([$userId, $streamId]);
        $row = $stmt->fetch();
        if (!$row) json(403, ['error' => 'Direct introuvable ou non autorisé']);

        $isOwner = (int)$row['vendor_id'] === $userId || in_array($row['role'], ['admin', 'super_admin']);
        if (!$isOwner) json(403, ['error' => 'Non autorisé à diffuser ce direct']);

        $frameData = base64_decode($frameB64, true);
        if ($frameData === false || $frameData === '') json(400, ['error' => 'Frame invalide']);

        // Upsert the latest frame.
        $stmt = $db->prepare("
            INSERT INTO live_frames (stream_id, frame_data, frame_at)
            VALUES (?, ?, NOW())
            ON DUPLICATE KEY UPDATE frame_data = VALUES(frame_data), frame_at = NOW()
        ");
        $stmt->execute([$streamId, $frameData]);

        json(200, ['success' => true, 'message' => 'Frame reçue']);
    }

    if ($method === 'GET') {
        $stmt = $db->prepare("SELECT frame_data, frame_at FROM live_frames WHERE stream_id = ?");
        $stmt->execute([$streamId]);
        $row = $stmt->fetch();
        if (!$row) json(404, ['success' => false, 'message' => 'Aucune frame']);

        // ── Viewer tracking ──
        // Record this spectator as an active viewer (heartbeat). The streamer's
        // viewer count is derived from the number of distinct users who polled
        // within the last 15 seconds, so it reflects real spectators.
        try {
            $db->exec("CREATE TABLE IF NOT EXISTS live_viewers (
                stream_id INT NOT NULL,
                user_id INT NOT NULL,
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (stream_id, user_id),
                FOREIGN KEY (stream_id) REFERENCES live_streams(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )");
        } catch (Exception $e) { error_log("Migration live_viewers table: " . $e->getMessage()); }

        $viewerUserId = 0;
        try { $viewerUserId = getAuthUserId(); } catch (Exception $e) { $viewerUserId = 0; }
        if ($viewerUserId > 0) {
            try {
                $db->prepare("INSERT INTO live_viewers (stream_id, user_id, last_seen) VALUES (?, ?, NOW())
                    ON DUPLICATE KEY UPDATE last_seen = NOW()")
                    ->execute([$streamId, $viewerUserId]);
            } catch (Exception $e) { error_log("Viewer tracking: " . $e->getMessage()); }
        }

        json(200, [
            'success' => true,
            'frame' => base64_encode($row['frame_data']),
            'frame_at' => $row['frame_at']
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
