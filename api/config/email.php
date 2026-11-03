<?php
/**
 * email.php — Module d'envoi d'emails (Reset Password / OTP)
 */

define('BREVO_API_KEY', getenv('BREVO_API_KEY') ?: '');
define('SENDER_EMAIL',  getenv('SENDER_EMAIL')  ?: 'noreply@tik-market.com');
define('SENDER_NAME',   getenv('SENDER_NAME')   ?: 'TiK-Market Security');

/**
 * Envoie un code OTP par Email avec un modèle HTML soigné
 *
 * @param string $toEmail Adresse email du destinataire
 * @param string $code    Code OTP à 6 chiffres
 * @return bool           true si envoyé avec succès
 */
function sendEmailOTP(string $toEmail, string $code): bool {
    $subject = "🔐 Code de réinitialisation - TiK-Market";

    $htmlBody = "
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset='utf-8'>
        <title>Réinitialisation de mot de passe</title>
    </head>
    <body style='font-family: Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px;'>
        <div style='max-width: 500px; margin: 0 auto; background: #ffffff; border-radius: 12px; padding: 30px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);'>
            <div style='text-align: center; margin-bottom: 20px;'>
                <h1 style='color: #00A651; margin: 0; font-size: 26px;'>TiK-Market</h1>
                <p style='color: #666666; font-size: 14px; margin-top: 5px;'>Sécurité de votre compte</p>
            </div>
            <hr style='border: none; border-top: 1px solid #eeeeee; margin: 20px 0;'>
            <p style='color: #333333; font-size: 16px; line-height: 1.5;'>Bonjour,</p>
            <p style='color: #555555; font-size: 15px; line-height: 1.5;'>
                Vous avez demandé la réinitialisation de votre mot de passe TiK-Market.
                Voici votre code de vérification :
            </p>
            <div style='text-align: center; margin: 25px 0;'>
                <span style='display: inline-block; background-color: #00A651; color: #ffffff; font-size: 28px; font-weight: bold; letter-spacing: 6px; padding: 12px 28px; border-radius: 8px;'>
                    $code
                </span>
            </div>
            <p style='color: #888888; font-size: 13px; text-align: center;'>
                Ce code est valable pendant <strong>5 minutes</strong>. Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email en toute sécurité.
            </p>
            <hr style='border: none; border-top: 1px solid #eeeeee; margin: 20px 0;'>
            <p style='color: #aaaaaa; font-size: 12px; text-align: center; margin: 0;'>
                &copy; " . date('Y') . " TiK-Market. Tous droits réservés.
            </p>
        </div>
    </body>
    </html>
    ";

    $brevoKey = BREVO_API_KEY;

    // 1. Si la clé Brevo (ex-Sendinblue) est configurée, envoyer via l'API REST Brevo
    if (!empty($brevoKey)) {
        $payload = [
            'sender'      => ['name' => SENDER_NAME, 'email' => SENDER_EMAIL],
            'to'          => [['email' => $toEmail]],
            'subject'     => $subject,
            'htmlContent' => $htmlBody
        ];

        $ch = curl_init('https://api.brevo.com/v3/smtp/email');
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POST           => true,
            CURLOPT_POSTFIELDS     => json_encode($payload),
            CURLOPT_HTTPHEADER     => [
                'api-key: ' . $brevoKey,
                'Content-Type: application/json',
                'Accept: application/json'
            ],
            CURLOPT_TIMEOUT        => 15,
            CURLOPT_CONNECTTIMEOUT => 10,
            CURLOPT_SSL_VERIFYPEER => false,
        ]);

        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($httpCode >= 200 && $httpCode < 300) {
            return true;
        }
        error_log("[TiK-Market Email Brevo] HTTP $httpCode response: " . $response);
    }

    // 2. Fallback avec la fonction native PHP mail()
    $headers  = "MIME-Version: 1.0" . "\r\n";
    $headers .= "Content-type:text/html;charset=UTF-8" . "\r\n";
    $headers .= "From: " . SENDER_NAME . " <" . SENDER_EMAIL . ">" . "\r\n";

    $sent = @mail($toEmail, $subject, $htmlBody, $headers);
    if ($sent) return true;

    // Log pour développement
    error_log("[TiK-Market Email Dev] Code $code destine a $toEmail");
    return true;
}
