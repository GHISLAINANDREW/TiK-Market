<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$userId = getAuthUserId();

try {
    $db = getDB();

    // Auto-create preferences row if not exists
    $stmt = $db->prepare('SELECT * FROM notification_preferences WHERE user_id = ?');
    $stmt->execute([$userId]);
    $prefs = $stmt->fetch();

    if (!$prefs) {
        $stmt = $db->prepare('INSERT INTO notification_preferences (user_id) VALUES (?)');
        $stmt->execute([$userId]);
        $prefs = [
            'allow_product' => 1,
            'allow_order'   => 1,
            'allow_promo'   => 1,
            'allow_message' => 1,
            'allow_system'  => 1,
            'push_enabled'  => 1
        ];
    }

    if ($method === 'GET') {
        json(200, [
            'success' => true,
            'preferences' => [
                'allow_product' => (bool)$prefs['allow_product'],
                'allow_order'   => (bool)$prefs['allow_order'],
                'allow_promo'   => (bool)$prefs['allow_promo'],
                'allow_message' => (bool)$prefs['allow_message'],
                'allow_system'  => (bool)$prefs['allow_system'],
                'push_enabled'  => (bool)$prefs['push_enabled']
            ]
        ]);
    }

    if ($method === 'PUT') {
        $data = json_decode(file_get_contents('php://input'), true);
        if (!$data) json(400, ['success' => false, 'error' => 'Corps de requête invalide']);

        $allowed = ['allow_product', 'allow_order', 'allow_promo', 'allow_message', 'allow_system', 'push_enabled'];
        $updates = [];
        $params  = [];

        foreach ($allowed as $field) {
            if (isset($data[$field])) {
                $updates[] = "$field = ?";
                $params[]  = $data[$field] ? 1 : 0;
            }
        }

        if (empty($updates)) {
            json(400, ['success' => false, 'error' => 'Aucun champ à mettre à jour']);
        }

        $params[] = $userId;
        $sql = 'UPDATE notification_preferences SET ' . implode(', ', $updates) . ' WHERE user_id = ?';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);

        // Return the updated preferences
        $stmt = $db->prepare('SELECT * FROM notification_preferences WHERE user_id = ?');
        $stmt->execute([$userId]);
        $updated = $stmt->fetch();

        json(200, [
            'success' => true,
            'preferences' => [
                'allow_product' => (bool)$updated['allow_product'],
                'allow_order'   => (bool)$updated['allow_order'],
                'allow_promo'   => (bool)$updated['allow_promo'],
                'allow_message' => (bool)$updated['allow_message'],
                'allow_system'  => (bool)$updated['allow_system'],
                'push_enabled'  => (bool)$updated['push_enabled']
            ]
        ]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['success' => false, 'error' => 'Erreur serveur: ' . $e->getMessage()]);
}
