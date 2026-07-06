<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$userId = getAuthUserId();

try {
    $db = getDB();

    if ($method === 'GET') {
        $stmt = $db->prepare('SELECT id, name, email, phone, role, location, avatar, last_seen FROM users WHERE id = ?');
        $stmt->execute([$userId]);
        $user = $stmt->fetch();

        if (!$user) {
            json(404, ['error' => 'Utilisateur non trouvé']);
        }

        $user['id'] = (int)$user['id'];

        json(200, ['user' => $user]);
    }

    if ($method === 'PUT') {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $fields = [];
        $params = [];

        foreach (['name', 'phone', 'location', 'avatar'] as $f) {
            if (isset($input[$f])) {
                $fields[] = "$f = ?";
                $params[] = $input[$f];
            }
        }

        if (empty($fields)) json(400, ['error' => 'Aucun champ à mettre à jour']);

        $params[] = $userId;
        $stmt = $db->prepare('UPDATE users SET ' . implode(', ', $fields) . ' WHERE id = ?');
        $stmt->execute($params);

        if (isset($input['avatar'])) {
            // Also update vendor shop logo if the user is a vendor
            $stmt = $db->prepare('UPDATE shops SET logo = ? WHERE vendor_id = ? AND (logo IS NULL OR logo = ? OR logo = ?)');
            $stmt->execute([$input['avatar'], $userId, '', $input['avatar']]);
        }

        // Return updated user
        $stmt = $db->prepare('SELECT id, name, email, phone, role, location, avatar, last_seen FROM users WHERE id = ?');
        $stmt->execute([$userId]);
        $user = $stmt->fetch();
        $user['id'] = (int)$user['id'];
        json(200, ['message' => 'Profil mis à jour', 'user' => $user]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
} catch (\Throwable $e) {
    json(500, ['error' => 'Erreur serveur']);
}
