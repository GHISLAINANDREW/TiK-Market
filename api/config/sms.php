<?php
/**
 * sms.php — Configuration et envoi SMS via Africa's Talking
 * 
 * 🔑 La clé API doit être configurée (constante ou variable d'env AT_API_KEY).
 *    Sans clé, l'envoi SMS échoue avec une erreur (pas de mode démo).
 */

// ── Configuration Africa's Talking ──────────────────────────────
define('AT_API_KEY',     'atsk_fb64c3b1ea8b940ef094991e30d3b4f6513a28763a5dd68c6af140e9035a3ed34feff424');
define('AT_USERNAME',    'sandbox');
define('AT_MODE',        'sandbox'); // 'sandbox' ou 'production'

/**
 * Envoie un SMS via l'API Africa's Talking
 * 
 * @param string $to   Numéro international (ex: +2376XXXXXXXX)
 * @param string $text Message à envoyer
 * @return bool        true si envoyé avec succès
 * @throws Exception   Si la clé API est manquante ou l'envoi échoue
 */
function sendSmsAfricaTalking(string $to, string $text): bool {
    // Vérifier que la clé API est configurée
    if (empty(AT_API_KEY)) {
        throw new Exception("Afrique's Talking API key is not configured");
    }

    $apiKey   = AT_API_KEY;
    $username = AT_USERNAME;
    $isSandbox = (AT_MODE === 'sandbox');
    
    $endpoint = $isSandbox
        ? 'https://api.sandbox.africastalking.com/version1/messaging'
        : 'https://api.africastalking.com/version1/messaging';

    $postData = [
        'username' => $username,
        'to'       => $to,
        'message'  => $text,
    ];

    $ch = curl_init();
    curl_setopt_array($ch, [
        CURLOPT_URL            => $endpoint,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST           => true,
        CURLOPT_POSTFIELDS     => http_build_query($postData),
        CURLOPT_HTTPHEADER     => [
            'Accept: application/json',
            'ApiKey: ' . $apiKey,
        ],
        CURLOPT_TIMEOUT        => 15,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_SSL_VERIFYPEER => false,
    ]);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    if ($curlError) {
        throw new Exception("cURL error: $curlError");
    }

    if ($httpCode !== 200 && $httpCode !== 201) {
        $detail = $response ?: '(empty response)';
        throw new Exception("Africa's Talking API HTTP $httpCode: $detail");
    }

    $result = json_decode($response, true);
    if (!$result) {
        throw new Exception("Invalid JSON response from Africa's Talking: " . ($response ?: '(empty)'));
    }

    $recipients = $result['SMSMessageData']['Recipients'] ?? [];
    foreach ($recipients as $r) {
        if (($r['status'] ?? '') === 'Success') {
            return true;
        }
    }

    throw new Exception("SMS delivery failed: " . json_encode($result));
}
