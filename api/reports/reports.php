<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // Auto-migration (development)
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS reports (
            id INT AUTO_INCREMENT PRIMARY KEY,
            reporter_id INT NOT NULL,
            type VARCHAR(20) NOT NULL COMMENT 'product / message / user',
            target_id INT NOT NULL,
            reason VARCHAR(100) NOT NULL,
            comment TEXT DEFAULT NULL,
            status VARCHAR(20) DEFAULT 'pending',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )");
    } catch (Exception $e) {}

    $userId = getAuthUserId();

    if ($method === 'POST') {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $type = trim($input['type'] ?? '');
        $target_id = (int)($input['target_id'] ?? 0);
        $reason = trim($input['reason'] ?? '');
        $comment = trim($input['comment'] ?? '');

        if (!in_array($type, ['product', 'message', 'user']) || $target_id <= 0 || $reason === '') {
            json(400, ['error' => 'type (product/message/user), target_id et reason requis']);
        }

        $stmt = $db->prepare('INSERT INTO reports (reporter_id, type, target_id, reason, comment) VALUES (?, ?, ?, ?, ?)');
        $stmt->execute([$userId, $type, $target_id, $reason, $comment]);

        json(201, ['message' => 'Signalement envoyé. Merci de contribuer à la qualité de TiK-Market.']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
} catch (\Throwable $e) {
    json(500, ['error' => 'Erreur serveur']);
}
