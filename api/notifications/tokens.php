<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$userId = getAuthUserId();

try {
    $db = getDB();

    if ($method === 'POST') {
        $data = json_decode(file_get_contents('php://input'), true);
        if (!$data) json(400, ['success' => false, 'error' => 'Corps de requête invalide']);

        $token = trim($data['token'] ?? '');
        $platform = trim($data['platform'] ?? 'web');

        if ($token === '') {
            json(400, ['success' => false, 'error' => 'Token requis']);
        }

        if (!in_array($platform, ['android', 'web', 'ios'])) {
            json(400, ['success' => false, 'error' => 'Plateforme invalide']);
        }

        $stmt = $db->prepare('
            INSERT INTO device_tokens (user_id, token, platform)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE platform = VALUES(platform), is_active = 1, updated_at = CURRENT_TIMESTAMP
        ');
        $stmt->execute([$userId, $token, $platform]);

        json(200, ['success' => true]);
    }

    if ($method === 'DELETE') {
        $data = json_decode(file_get_contents('php://input'), true);
        if (!$data) json(400, ['success' => false, 'error' => 'Corps de requête invalide']);

        $token = trim($data['token'] ?? '');
        if ($token === '') {
            json(400, ['success' => false, 'error' => 'Token requis']);
        }

        $stmt = $db->prepare('UPDATE device_tokens SET is_active = 0 WHERE user_id = ? AND token = ?');
        $stmt->execute([$userId, $token]);

        json(200, ['success' => true]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['success' => false, 'error' => 'Erreur serveur: ' . $e->getMessage()]);
}
