<?php
/**
 * Stop a live stream API endpoint.
 *
 * POST /live/stop.php  body: {"stream_id": 1}
 * → {"success": true}
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    if ($method === 'POST') {
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $streamId = (int)($input['stream_id'] ?? 0);
        if ($streamId <= 0) json(400, ['error' => 'stream_id requis']);

        // Verify ownership: vendor owns the shop, or admin
        $stmt = $db->prepare("
            SELECT ls.id, ls.shop_id, s.vendor_id, u.role AS caller_role
            FROM live_streams ls
            JOIN shops s ON ls.shop_id = s.id
            JOIN users u ON u.id = ?
            WHERE ls.id = ?
        ");
        $stmt->execute([$userId, $streamId]);
        $stream = $stmt->fetch();
        if (!$stream) json(404, ['error' => 'Direct introuvable']);

        $isAdmin = in_array($stream['caller_role'], ['admin', 'super_admin']);
        if ((int)$stream['vendor_id'] !== $userId && !$isAdmin) {
            json(403, ['error' => 'Vous ne pouvez arrêter que vos propres directs']);
        }

        $db->prepare("UPDATE live_streams SET is_live = 0, ended_at = NOW() WHERE id = ?")
            ->execute([$streamId]);

        json(200, ['success' => true]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
