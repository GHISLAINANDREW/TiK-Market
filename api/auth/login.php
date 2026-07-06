<?php
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$input = json_decode(file_get_contents('php://input'), true);
if (!$input) json(400, ['error' => 'Corps de requête invalide']);

$email = trim($input['email'] ?? '');
$password = $input['password'] ?? '';

if ($email === '' || $password === '') {
    json(400, ['error' => 'Email et mot de passe requis']);
}

try {
    $db = getDB();

    $stmt = $db->prepare('SELECT id, name, email, phone, password, role, avatar, location, status, last_seen FROM users WHERE email = ?');
    $stmt->execute([$email]);
    $user = $stmt->fetch();

    if (!$user || !password_verify($password, $user['password'])) {
        json(401, ['error' => 'Email ou mot de passe incorrect']);
    }

    // Vérifier si le compte est banni
    if (isset($user['status']) && $user['status'] === 'banned') {
        json(403, ['error' => 'Votre compte a été suspendu. Contactez l\'administrateur.']);
    }

    $token = generateToken((int)$user['id'], $user['email']);

    json(200, [
        'token' => $token,
        'user' => [
            'id' => (int)$user['id'],
            'name' => $user['name'],
            'email' => $user['email'],
            'phone' => $user['phone'],
            'role' => $user['role'],
            'avatar' => $user['avatar'] ?? '',
            'location' => $user['location'] ?? '',
            'last_seen' => $user['last_seen'] ?? '',
        ]
    ]);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
