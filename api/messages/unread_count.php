<?php
require_once __DIR__ . '/../config/database.php';

try {
    $userId = getAuthUserId();
    $db = getDB();

    $stmt = $db->prepare('SELECT COUNT(*) AS unread_count FROM messages WHERE receiver_id = ? AND is_read = 0');
    $stmt->execute([$userId]);
    $result = $stmt->fetch();

    json(200, ['unread_count' => (int)$result['unread_count']]);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
