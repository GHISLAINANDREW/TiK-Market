<?php
/**
 * send-otp.php — Envoie un code OTP par SMS via Africa's Talking
 * 
 * Endpoint : POST /auth/send-otp.php
 * Body     : { "phone": "6XXXXXXXX" }  (format local, sans +237)
 * 
 * Réponse  : { "success": true, "message": "Code envoyé", "expires_in": 300 }
 * Erreur   : { "error": "..." } avec code HTTP 4xx/5xx
 */

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../config/sms.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$input = json_decode(file_get_contents('php://input'), true);
if (!$input) json(400, ['error' => 'Corps de requête invalide']);

$phone = trim($input['phone'] ?? '');
// Nettoyer le numéro : enlever +237, espaces, etc.
$phone = preg_replace('/[^0-9]/', '', $phone);
// Si commence par 237, enlever le préfixe
if (str_starts_with($phone, '237')) {
    $phone = substr($phone, 3);
}

if (strlen($phone) < 8 || strlen($phone) > 9) {
    json(400, ['error' => 'Numéro de téléphone invalide (format: 6XXXXXXXX)']);
}

// Format international pour Africa's Talking
$internationalPhone = '+237' . $phone;

try {
    $db = getDB();

    // Vérifier le nombre de tentatives récentes (anti-spam)
    $stmt = $db->prepare(
        'SELECT COUNT(*) as attempts FROM otp_codes 
         WHERE phone = ? AND created_at > DATE_SUB(NOW(), INTERVAL 10 MINUTE)'
    );
    $stmt->execute([$phone]);
    $recent = $stmt->fetch();
    if ($recent['attempts'] >= 5) {
        json(429, ['error' => 'Trop de tentatives. Réessayez dans 10 minutes.']);
    }

    // Générer un code OTP à 6 chiffres
    $otpCode = str_pad((string)random_int(0, 999999), 6, '0', STR_PAD_LEFT);
    $expiresAt = date('Y-m-d H:i:s', time() + 300); // 5 minutes

    // Envoyer le SMS via Africa's Talking
    sendSmsAfricaTalking($internationalPhone, "Votre code DschangMarket : $otpCode. Valable 5 minutes.");

    // Stocker le code en base (uniquement si le SMS a été envoyé avec succès)
    $stmt = $db->prepare(
        'INSERT INTO otp_codes (phone, code, expires_at) VALUES (?, ?, ?)'
    );
    $stmt->execute([$phone, $otpCode, $expiresAt]);

    json(200, [
        'success'    => true,
        'message'    => 'Code de vérification envoyé par SMS.',
        'expires_in' => 300,
    ]);

} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
} catch (Exception $e) {
    json(500, ['error' => 'Erreur d\'envoi SMS : ' . $e->getMessage()]);
}
