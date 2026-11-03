<?php
/**
 * whatsapp.php — Module d'envoi de messages via Meta WhatsApp Business Cloud API
 */

define('WA_PHONE_NUMBER_ID', getenv('WHATSAPP_PHONE_NUMBER_ID') ?: '');
define('WA_ACCESS_TOKEN',    getenv('WHATSAPP_ACCESS_TOKEN')    ?: '');

/**
 * Envoie un code OTP par WhatsApp via Meta Cloud API
 *
 * @param string $phone Numéro au format local (6XXXXXXXX) ou international (+2376XXXXXXXX)
 * @param string $code  Code OTP à 6 chiffres
 * @return bool         true si envoyé avec succès
 */
function sendWhatsAppOTP(string $phone, string $code): bool {
    // Nettoyer et mettre au format international sans le +
    $cleanPhone = preg_replace('/[^0-9]/', '', $phone);
    if (!str_starts_with($cleanPhone, '237')) {
        $cleanPhone = '237' . $cleanPhone;
    }

    $phoneNumberId = WA_PHONE_NUMBER_ID;
    $accessToken   = WA_ACCESS_TOKEN;

    $messageText = "🔐 *TiK-Market*\nVotre code de réinitialisation de mot de passe est : *$code*\nCe code expire dans 5 minutes.";

    if (empty($phoneNumberId) || empty($accessToken)) {
        // En mode dev/fallback, on consigne dans les logs PHP pour faciliter le débogage
        error_log("[TiK-Market WhatsApp Dev] Code $code destine au $cleanPhone : $messageText");
        return true;
    }

    $url = "https://graph.facebook.com/v19.0/{$phoneNumberId}/messages";

    $payload = [
        'messaging_product' => 'whatsapp',
        'recipient_type'    => 'individual',
        'to'                => $cleanPhone,
        'type'              => 'text',
        'text'              => [
            'preview_url' => false,
            'body'        => $messageText
        ]
    ];

    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST           => true,
        CURLOPT_POSTFIELDS     => json_encode($payload),
        CURLOPT_HTTPHEADER     => [
            'Authorization: Bearer ' . $accessToken,
            'Content-Type: application/json'
        ],
        CURLOPT_TIMEOUT        => 15,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_SSL_VERIFYPEER => false,
    ]);

    $response  = curl_exec($ch);
    $httpCode  = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    if ($curlError) {
        error_log("[TiK-Market WhatsApp] cURL error: $curlError");
        return false;
    }

    if ($httpCode >= 200 && $httpCode < 300) {
        return true;
    }

    error_log("[TiK-Market WhatsApp] HTTP $httpCode response: " . $response);
    return false;
}
