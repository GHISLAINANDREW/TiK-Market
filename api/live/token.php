<?php
/**
 * LiveKit access token endpoint.
 *
 * POST /live/token.php  body: {"room": "room_name", "identity": "optional_identity"}
 * → {"success": true, "token": "...", "url": "wss://..."}
 *
 * Issues a LiveKit access token (JWT, HS256) signed with the LiveKit API secret.
 * The token grants the authenticated user access to a specific room.
 *
 * Required env vars (set on the server):
 *   LIVEKIT_API_KEY    — LiveKit API key
 *   LIVEKIT_API_SECRET — LiveKit API secret
 *   LIVEKIT_URL        — LiveKit server WSS URL, e.g. wss://tik-market.duckdns.org
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    if ($method === 'POST') {
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $room = trim($input['room'] ?? '');
        if ($room === '') json(400, ['error' => 'Nom de salle requis']);

        // LiveKit credentials from environment.
        $apiKey = getenv('LIVEKIT_API_KEY');
        $apiSecret = getenv('LIVEKIT_API_SECRET');
        $livekitUrl = getenv('LIVEKIT_URL') ?: 'wss://tik-market.duckdns.org';

        if (!$apiKey || !$apiSecret) {
            error_log('[TiK-Market] LIVEKIT_API_KEY / LIVEKIT_API_SECRET non définis');
            json(500, ['error' => 'LiveKit non configuré sur le serveur']);
        }

        // Identity: use the authenticated user's id (or a provided identity).
        $identity = trim($input['identity'] ?? '');
        if ($identity === '') $identity = 'user_' . $userId;

        // Room name sanitization: only [a-zA-Z0-9-_] allowed by LiveKit.
        $room = preg_replace('/[^a-zA-Z0-9_\-]/', '_', $room);

        // ── Build LiveKit access token (JWT HS256) ──
        $header = base64url_encode(json_encode(['typ' => 'JWT', 'alg' => 'HS256']));
        $now = time();
        $payload = [
            'iss' => $apiKey,
            'sub' => $identity,
            'nbf' => $now - 5,
            'exp' => $now + 3600, // 1 hour
            'video' => [
                'room' => $room,
                'roomJoin' => true,
                'canPublish' => true,
                'canSubscribe' => true,
                'canPublishData' => true,
            ],
        ];
        $payloadEncoded = base64url_encode(json_encode($payload));
        $signature = base64url_encode(hash_hmac('sha256', "$header.$payloadEncoded", $apiSecret, true));
        $token = "$header.$payloadEncoded.$signature";

        json(200, [
            'success' => true,
            'token' => $token,
            'url' => $livekitUrl,
            'room' => $room,
            'identity' => $identity,
        ]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
