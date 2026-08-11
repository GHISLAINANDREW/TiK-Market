<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$userId = getAuthUserId();

try {
    $db = getDB();

    // Auto-migration: ensure cover_photo column exists on users
    try {
        $check = $db->query("SHOW COLUMNS FROM users LIKE 'cover_photo'")->fetch();
        if (!$check) {
            $db->exec("ALTER TABLE users ADD COLUMN cover_photo VARCHAR(500) DEFAULT '' AFTER avatar");
        }
    } catch (Exception $e) {
        error_log("Migration cover_photo error: " . $e->getMessage());
    }

    if ($method === 'GET') {
        $stmt = $db->prepare('SELECT id, name, email, phone, role, location, avatar, cover_photo, last_seen FROM users WHERE id = ?');
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

        foreach (['name', 'phone', 'location', 'avatar', 'cover_photo'] as $f) {
            if (isset($input[$f])) {
                $fields[] = "$f = ?";
                $params[] = $input[$f];
            }
        }

        if (isset($input['password']) && !empty($input['password'])) {
            $fields[] = "password = ?";
            $params[] = password_hash($input['password'], PASSWORD_DEFAULT);
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
        $stmt = $db->prepare('SELECT id, name, email, phone, role, location, avatar, cover_photo, last_seen FROM users WHERE id = ?');
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
