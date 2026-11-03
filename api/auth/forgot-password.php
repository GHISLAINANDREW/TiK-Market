<?php
/**
 * forgot-password.php — Demande de réinitialisation de mot de passe
 *
 * Endpoint : POST /auth/forgot-password.php
 * Body     : { "identifier": "user@example.com ou 6XXXXXXXX", "method": "whatsapp" | "email" }
 */

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../config/whatsapp.php';
require_once __DIR__ . '/../config/email.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$input = json_decode(file_get_contents('php://input'), true);
if (!$input) json(400, ['error' => 'Corps de requête invalide']);

$identifier = trim($input['identifier'] ?? '');
$method     = strtolower(trim($input['method'] ?? 'whatsapp'));

if (empty($identifier)) {
    json(400, ['error' => 'Veuillez saisir votre adresse email ou votre numéro de téléphone.']);
}

// Nettoyage téléphone si numérique
$cleanPhone = preg_replace('/[^0-9]/', '', $identifier);
if (str_starts_with($cleanPhone, '237')) {
    $cleanPhone = substr($cleanPhone, 3);
}

try {
    $db = getDB();

    // Recherche de l'utilisateur par Email ou Téléphone
    $stmt = $db->prepare('SELECT id, name, email, phone FROM users WHERE email = ? OR phone = ? OR phone = ?');
    $stmt->execute([$identifier, $identifier, $cleanPhone]);
    $user = $stmt->fetch();

    if (!$user) {
        json(404, ['error' => 'Aucun compte TiK-Market associé à ces informations.']);
    }

    // Vérification anti-spam
    $targetKey = ($method === 'email') ? ($user['email'] ?: $identifier) : ($user['phone'] ?: $identifier);
    $stmt = $db->prepare(
        'SELECT COUNT(*) as attempts FROM otp_codes
         WHERE phone = ? AND created_at > DATE_SUB(NOW(), INTERVAL 10 MINUTE)'
    );
    $stmt->execute([$targetKey]);
    $recent = $stmt->fetch();
    if (($recent['attempts'] ?? 0) >= 5) {
        json(429, ['error' => 'Trop de demandes récentes. Réessayez dans 10 minutes.']);
    }

    // Génération du code OTP à 6 chiffres
    $otpCode = str_pad((string)random_int(0, 999999), 6, '0', STR_PAD_LEFT);
    $expiresAt = date('Y-m-d H:i:s', time() + 300); // 5 minutes

    // Enregistrement en base
    $stmt = $db->prepare('INSERT INTO otp_codes (phone, code, expires_at) VALUES (?, ?, ?)');
    $stmt->execute([$targetKey, $otpCode, $expiresAt]);

    // Envoi du message selon la méthode choisie
    if ($method === 'email') {
        $emailToSend = !empty($user['email']) ? $user['email'] : $identifier;
        if (!filter_var($emailToSend, FILTER_VALIDATE_EMAIL)) {
            json(400, ['error' => "Ce compte n'a pas d'adresse email valide. Utilisez la méthode WhatsApp."]);
        }
        $sent = sendEmailOTP($emailToSend, $otpCode);
        $channelName = "Email";
    } else {
        $phoneToSend = !empty($user['phone']) ? $user['phone'] : $identifier;
        $sent = sendWhatsAppOTP($phoneToSend, $otpCode);
        $channelName = "WhatsApp Business";
    }

    if (!$sent) {
        json(500, ['error' => "Échec de l'envoi du code via $channelName. Veuillez réessayer."]);
    }

    json(200, [
        'success'    => true,
        'message'    => "Un code de vérification à 6 chiffres a été envoyé par $channelName.",
        'expires_in' => 300
    ]);

} catch (PDOException $e) {
    json(500, ['error' => 'Erreur de base de données : ' . $e->getMessage()]);
} catch (Exception $e) {
    json(500, ['error' => 'Erreur interne : ' . $e->getMessage()]);
}
