<?php
/**
 * verify-otp.php — Vérifie le code OTP et connecte/inscrit l'utilisateur
 * 
 * Endpoint : POST /auth/verify-otp.php
 * Body     : { "phone": "6XXXXXXXX", "code": "123456" }
 * 
 * Réponse  : { "success": true, "token": "...", "user": {...}, "is_new": bool }
 */

require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$input = json_decode(file_get_contents('php://input'), true);
if (!$input) json(400, ['error' => 'Corps de requête invalide']);

$phone = trim($input['phone'] ?? '');
$code  = trim($input['code'] ?? '');

// Nettoyer le numéro
$phone = preg_replace('/[^0-9]/', '', $phone);
if (str_starts_with($phone, '237')) {
    $phone = substr($phone, 3);
}

if (strlen($phone) < 8 || strlen($phone) > 9) {
    json(400, ['error' => 'Numéro de téléphone invalide']);
}
if (!preg_match('/^\d{4,6}$/', $code)) {
    json(400, ['error' => 'Code invalide']);
}

try {
    $db = getDB();

    // Chercher un code OTP valide
    $stmt = $db->prepare(
        'SELECT id, code FROM otp_codes 
         WHERE phone = ? AND code = ? AND expires_at > NOW() AND used = 0
         ORDER BY created_at DESC LIMIT 1'
    );
    $stmt->execute([$phone, $code]);
    $otpRow = $stmt->fetch();

    if (!$otpRow) {
        json(401, ['error' => 'Code incorrect ou expiré. Demandez un nouveau code.']);
    }

    // Marquer le code comme utilisé
    $stmt = $db->prepare('UPDATE otp_codes SET used = 1 WHERE id = ?');
    $stmt->execute([$otpRow['id']]);

    // Chercher si l'utilisateur existe déjà avec ce téléphone
    $stmt = $db->prepare('SELECT id, name, email, phone, role, avatar, created_at FROM users WHERE phone = ?');
    $stmt->execute([$phone]);
    $user = $stmt->fetch();
    $isNew = false;

    if (!$user) {
        // Créer un nouvel utilisateur avec ce téléphone
        $displayName = 'Client +237 ' . $phone;
        $stmt = $db->prepare(
            'INSERT INTO users (name, email, phone, password, role) VALUES (?, ?, ?, ?, ?)'
        );
        // Email unique bidon : phone@dschangmarket.local
        $generatedEmail = $phone . '@dschangmarket.local';
        $stmt->execute([$displayName, $generatedEmail, $phone, password_hash($phone, PASSWORD_BCRYPT), 'buyer']);
        $userId = (int)$db->lastInsertId();
        $isNew = true;

        $user = [
            'id'         => $userId,
            'name'       => $displayName,
            'email'      => $generatedEmail,
            'phone'      => $phone,
            'role'       => 'buyer',
            'avatar'     => '',
            'created_at' => date('Y-m-d H:i:s'),
        ];
    }

    // Générer le token
    $token = generateToken((int)$user['id'], $user['email']);

    json(200, [
        'success' => true,
        'token'   => $token,
        'user'    => [
            'id'         => (int)$user['id'],
            'name'       => $user['name'],
            'email'      => $user['email'],
            'phone'      => $user['phone'],
            'role'       => $user['role'],
            'avatar'     => $user['avatar'] ?? '',
            'last_seen'  => $user['last_seen'] ?? '',
            'created_at' => $user['created_at'],
        ],
        'is_new'  => $isNew,
    ]);

} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
