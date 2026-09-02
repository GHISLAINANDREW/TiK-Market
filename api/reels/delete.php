<?php
/**
 * Delete a reel API endpoint.
 *
 * POST /reels/delete.php  body: {"reel_id": 1}
 * → {"success": true, "message": "Reel supprimé"}
 *
 * Only the shop owner (vendor) or an admin can delete a reel.
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    if ($method === 'POST') {
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $reelId = (int)($input['reel_id'] ?? 0);
        if ($reelId <= 0) json(400, ['error' => 'reel_id requis']);

        // Verify reel exists and check ownership
        $stmt = $db->prepare("
            SELECT r.id, r.shop_id, s.vendor_id, u.role
            FROM reels r
            JOIN shops s ON r.shop_id = s.id
            JOIN users u ON u.id = ?
            WHERE r.id = ?
        ");
        $stmt->execute([$userId, $reelId]);
        $row = $stmt->fetch();
        if (!$row) json(404, ['error' => 'Reel introuvable']);

        $isOwner = (int)$row['vendor_id'] === $userId || in_array($row['role'], ['admin', 'super_admin']);
        if (!$isOwner) json(403, ['error' => 'Vous n\'êtes pas autorisé à supprimer ce reel']);

        $db->prepare("DELETE FROM reels WHERE id = ?")->execute([$reelId]);

        json(200, ['success' => true, 'message' => 'Reel supprimé']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}