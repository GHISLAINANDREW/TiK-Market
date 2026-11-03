<?php
/**
 * reset-password.php — Validation du code OTP et changement du mot de passe
 *
 * Endpoint : POST /auth/reset-password.php
 * Body     : { "identifier": "user@example.com ou 6XXXXXXXX", "code": "123456", "new_password": "..." }
 */

require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$input = json_decode(file_get_contents('php://input'), true);
if (!$input) json(400, ['error' => 'Corps de requête invalide']);

$identifier  = trim($input['identifier'] ?? '');
$code        = trim($input['code'] ?? '');
$newPassword = $input['new_password'] ?? '';

if (empty($identifier) || empty($code) || empty($newPassword)) {
    json(400, ['error' => 'Veuillez remplir tous les champs.']);
}

if (strlen($newPassword) < 6) {
    json(400, ['error' => 'Le nouveau mot de passe doit contenir au moins 6 caractères.']);
}

$cleanPhone = preg_replace('/[^0-9]/', '', $identifier);
if (str_starts_with($cleanPhone, '237')) {
    $cleanPhone = substr($cleanPhone, 3);
}

try {
    $db = getDB();

    // 1. Recherche de l'utilisateur
    $stmt = $db->prepare('SELECT id, email, phone FROM users WHERE email = ? OR phone = ? OR phone = ?');
    $stmt->execute([$identifier, $identifier, $cleanPhone]);
    $user = $stmt->fetch();

    if (!$user) {
        json(404, ['error' => 'Utilisateur introuvable.']);
    }

    $userEmail = $user['email'];
    $userPhone = $user['phone'];

    // 2. Vérification du code OTP (soit sur l'identifiant saisi, soit sur l'email/phone de l'utilisateur)
    $stmt = $db->prepare(
        'SELECT id FROM otp_codes
         WHERE (phone = ? OR phone = ? OR phone = ? OR phone = ?)
           AND code = ?
           AND used = 0
           AND expires_at > NOW()
         ORDER BY id DESC LIMIT 1'
    );
    $stmt->execute([$identifier, $userEmail, $userPhone, $cleanPhone, $code]);
    $otpRow = $stmt->fetch();

    if (!$otpRow) {
        json(400, ['error' => 'Code de vérification invalide ou expiré.']);
    }

    // 3. Marquer le code OTP comme utilisé
    $stmt = $db->prepare('UPDATE otp_codes SET used = 1 WHERE id = ?');
    $stmt->execute([$otpRow['id']]);

    // 4. Mettre à jour le mot de passe
    $hashedPassword = password_hash($newPassword, PASSWORD_BCRYPT);
    $stmt = $db->prepare('UPDATE users SET password_hash = ? WHERE id = ?');
    $stmt->execute([$hashedPassword, $user['id']]);

    json(200, [
        'success' => true,
        'message' => 'Mot de passe réinitialisé avec succès ! Vous pouvez maintenant vous connecter.'
    ]);

} catch (PDOException $e) {
    json(500, ['error' => 'Erreur de base de données : ' . $e->getMessage()]);
} catch (Exception $e) {
    json(500, ['error' => 'Erreur interne : ' . $e->getMessage()]);
}
