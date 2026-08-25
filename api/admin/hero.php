<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // Ensure hero_items table exists
    $db->exec("CREATE TABLE IF NOT EXISTS hero_items (
        id INT AUTO_INCREMENT PRIMARY KEY,
        title VARCHAR(200) NOT NULL,
        subtitle VARCHAR(500) NOT NULL,
        image_url VARCHAR(500) NOT NULL,
        shop_id INT DEFAULT NULL,
        priority INT DEFAULT 0,
        is_active TINYINT(1) DEFAULT 1,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    if ($method === 'GET') {
        $stmt = $db->query("
            SELECT h.*, s.name AS shop_name
            FROM hero_items h
            LEFT JOIN shops s ON h.shop_id = s.id
            WHERE h.is_active = 1
            ORDER BY h.priority DESC, h.created_at DESC
        ");
        $items = $stmt->fetchAll();
        foreach ($items as &$item) {
            $item['id'] = (int)$item['id'];
            $item['shop_id'] = $item['shop_id'] ? (int)$item['shop_id'] : null;
            $item['priority'] = (int)$item['priority'];
            $item['is_active'] = (bool)$item['is_active'];
        }
        json(200, $items);
    }

    if ($method === 'POST') {
        $userId = getAuthUserId();
        // Check if admin
        $stmt = $db->prepare("SELECT role FROM users WHERE id = ?");
        $stmt->execute([$userId]);
        $user = $stmt->fetch();
        if (!$user || $user['role'] !== 'admin') json(403, ['error' => 'Accès refusé']);

        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $title = trim($input['title'] ?? '');
        $subtitle = trim($input['subtitle'] ?? '');
        $image_url = trim($input['image_url'] ?? '');
        $shop_id = isset($input['shop_id']) ? (int)$input['shop_id'] : null;
        $priority = isset($input['priority']) ? (int)$input['priority'] : 0;

        if ($title === '' || $image_url === '') {
            json(400, ['error' => 'Titre et URL d\'image requis']);
        }

        $stmt = $db->prepare("INSERT INTO hero_items (title, subtitle, image_url, shop_id, priority) VALUES (?, ?, ?, ?, ?)");
        $stmt->execute([$title, $subtitle, $image_url, $shop_id, $priority]);

        $id = (int)$db->lastInsertId();
        json(201, ['message' => 'Bannière ajoutée', 'id' => $id]);
    }

    if ($method === 'DELETE') {
        $userId = getAuthUserId();
        // Check if admin
        $stmt = $db->prepare("SELECT role FROM users WHERE id = ?");
        $stmt->execute([$userId]);
        $user = $stmt->fetch();
        if (!$user || $user['role'] !== 'admin') json(403, ['error' => 'Accès refusé']);

        $id = isset($_GET['id']) ? (int)$_GET['id'] : null;
        if (!$id) json(400, ['error' => 'ID requis']);

        $stmt = $db->prepare("DELETE FROM hero_items WHERE id = ?");
        $stmt->execute([$id]);

        json(200, ['message' => 'Bannière supprimée']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
