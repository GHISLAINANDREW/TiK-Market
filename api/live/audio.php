<?php
/**
 * Live stream audio chunk upload / retrieval API endpoint.
 *
 * POST /live/audio.php?stream_id=1  body: {"seq": 1, "audio": "<base64 aac/mp4>"}
 *   → {"success": true, "message": "Audio reçu"}
 *
 * GET /live/audio.php?stream_id=1&after_seq=0
 *   → {"success": true, "chunks": [{"seq":1,"audio":"<base64>","created_at":"..."}]}
 *
 * The streamer records short audio chunks (AAC/MP4) and uploads them with an
 * incrementing sequence number. Spectators poll for chunks after a given seq
 * and play them in order, giving near-real-time voice.
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // ── Auto-migration: create live_audio table ──
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS live_audio (
            id INT AUTO_INCREMENT PRIMARY KEY,
            stream_id INT NOT NULL,
            seq INT NOT NULL,
            audio_data LONGBLOB NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY uq_stream_seq (stream_id, seq),
            FOREIGN KEY (stream_id) REFERENCES live_streams(id) ON DELETE CASCADE,
            INDEX idx_stream_seq (stream_id, seq)
        )");
    } catch (Exception $e) { error_log("Migration live_audio table: " . $e->getMessage()); }

    $streamId = isset($_GET['stream_id']) ? (int)$_GET['stream_id'] : 0;
    if ($streamId <= 0) json(400, ['error' => 'stream_id requis']);

    if ($method === 'POST') {
        // Only the stream owner (vendor/admin) can upload audio.
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $seq = (int)($input['seq'] ?? 0);
        $audioB64 = $input['audio'] ?? '';
        if ($audioB64 === '') json(400, ['error' => 'Audio manquant']);

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

        $audioData = base64_decode($audioB64, true);
        if ($audioData === false || $audioData === '') json(400, ['error' => 'Audio invalide']);

        // Insert the chunk (ignore duplicates on seq).
        try {
            $stmt = $db->prepare("INSERT INTO live_audio (stream_id, seq, audio_data) VALUES (?, ?, ?)");
            $stmt->execute([$streamId, $seq, $audioData]);
        } catch (PDOException $e) {
            // Duplicate seq → ignore.
            if ($e->getCode() != 23000) throw $e;
        }

        json(200, ['success' => true, 'message' => 'Audio reçu']);
    }

    if ($method === 'GET') {
        $afterSeq = isset($_GET['after_seq']) ? (int)$_GET['after_seq'] : 0;

        $stmt = $db->prepare("
            SELECT seq, audio_data, created_at
            FROM live_audio
            WHERE stream_id = ? AND seq > ?
            ORDER BY seq ASC
            LIMIT 50
        ");
        $stmt->execute([$streamId, $afterSeq]);
        $rows = $stmt->fetchAll();

        $chunks = [];
        foreach ($rows as $r) {
            $chunks[] = [
                'seq' => (int)$r['seq'],
                'audio' => base64_encode($r['audio_data']),
                'created_at' => $r['created_at']
            ];
        }

        json(200, ['success' => true, 'chunks' => $chunks]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
