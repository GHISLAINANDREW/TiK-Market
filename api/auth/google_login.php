<?php
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$input = json_decode(file_get_contents('php://input'), true);
$idToken = $input['id_token'] ?? '';

if (!$idToken) json(400, ['error' => 'id_token requis']);

// Config Google
$google_client_id = "GOOGLE_CLIENT_ID_PLACEHOLDER";
$google_client_secret = "GOOGLE_CLIENT_SECRET_PLACEHOLDER";

// 1. Vérifier le token avec Google
$verifyUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" . $idToken;
$response = @file_get_contents($verifyUrl);
if (!$response) json(401, ['error' => 'Token Google invalide ou expiré']);

$payload = json_decode($response, true);
if (!isset($payload['email'])) json(401, ['error' => 'Données Google incomplètes']);

// Vérification de l'audience (sécurité supplémentaire)
if ($payload['aud'] !== $google_client_id) {
    json(401, ['error' => 'Token destiné à une autre application']);
}

$email = $payload['email'];
$name = $payload['name'] ?? explode('@', $email)[0];
$avatar = $payload['picture'] ?? '';

try {
    $db = getDB();

    // 2. Chercher l'utilisateur par email
    $stmt = $db->prepare('SELECT id, name, email, phone, role, avatar, status FROM users WHERE email = ?');
    $stmt->execute([$email]);
    $user = $stmt->fetch();

    if (!$user) {
        // 3. Créer l'utilisateur s'il n'existe pas
        $referralCode = strtoupper(substr(md5($email . time()), 0, 8));
        $stmt = $db->prepare('INSERT INTO users (name, email, role, avatar, referral_code, password) VALUES (?, ?, ?, ?, ?, ?)');
        $stmt->execute([$name, $email, 'buyer', $avatar, $referralCode, password_hash(bin2hex(random_bytes(16)), PASSWORD_DEFAULT)]);
        $userId = (int)$db->lastInsertId();

        // Re-fetch
        $stmt = $db->prepare('SELECT id, name, email, phone, role, avatar, status FROM users WHERE id = ?');
        $stmt->execute([$userId]);
        $user = $stmt->fetch();
    }

    if ($user['status'] === 'banned') {
        json(403, ['error' => 'Votre compte a été suspendu']);
    }

    // 4. Générer le token de session
    $token = generateToken((int)$user['id'], $user['email']);

    json(200, [
        'token' => $token,
        'user' => [
            'id' => (int)$user['id'],
            'name' => $user['name'],
            'email' => $user['email'],
            'phone' => $user['phone'] ?? '',
            'role' => $user['role'],
            'avatar' => $user['avatar'] ?? '',
        ]
    ]);

} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur: ' . $e->getMessage()]);
}
