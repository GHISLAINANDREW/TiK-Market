<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }

function getDB(): PDO {
    $host = getenv('DB_HOST') ?: 'localhost';
    $name = getenv('DB_NAME') ?: 'dschang_market';
    $user = getenv('DB_USER') ?: 'root';
    $pass = getenv('DB_PASS') ?: '';
    $pdo = new PDO("mysql:host=$host;dbname=$name;charset=utf8mb4", $user, $pass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false
    ]);
    return $pdo;
}

function json(int $code, $data): void {
    http_response_code($code);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

define('JWT_SECRET', 'dschang_market_jwt_secret_2026_change_in_production');
define('JWT_EXPIRY', 86400 * 30); // 30 days

function base64url_encode(string $data): string {
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}

function base64url_decode(string $data): string {
    // Restore padding if needed (base64_decode handles missing padding in PHP 7+, but be explicit)
    $remainder = strlen($data) % 4;
    if ($remainder > 0) {
        $data .= str_repeat('=', 4 - $remainder);
    }
    $decoded = base64_decode(strtr($data, '-_', '+/'), true);
    return $decoded !== false ? $decoded : base64_decode(strtr($data, '-_', '+/'));
}

function jwt_encode(array $payload): string {
    $header = base64url_encode(json_encode(['typ' => 'JWT', 'alg' => 'HS256']));
    $payload['iat'] = time();
    $payload['exp'] = time() + JWT_EXPIRY;
    $payloadEncoded = base64url_encode(json_encode($payload));
    $signature = base64url_encode(hash_hmac('sha256', "$header.$payloadEncoded", JWT_SECRET, true));
    return "$header.$payloadEncoded.$signature";
}

function jwt_decode(string $token): ?array {
    $parts = explode('.', $token);
    if (count($parts) !== 3) return null;
    [$header, $payload, $signature] = $parts;
    $expectedSig = base64url_encode(hash_hmac('sha256', "$header.$payload", JWT_SECRET, true));
    if (!hash_equals($expectedSig, $signature)) return null;
    $data = json_decode(base64url_decode($payload), true);
    if (!$data || !isset($data['exp']) || $data['exp'] < time()) return null;
    return $data;
}

function getAuthUserId(): int {
    $userId = 0;
    // Fallback: PHP built-in server does not always populate getallheaders()
    $token = '';
    $headers = function_exists('getallheaders') ? getallheaders() : [];
    if (!empty($headers['Authorization'])) {
        $token = $headers['Authorization'];
    } elseif (!empty($_SERVER['HTTP_AUTHORIZATION'])) {
        $token = $_SERVER['HTTP_AUTHORIZATION'];
    } elseif (!empty($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) {
        $token = $_SERVER['REDIRECT_HTTP_AUTHORIZATION'];
    }
    if (!str_starts_with($token, 'Bearer ')) json(401, ['error' => 'Non authentifié']);
    $jwt = substr($token, 7);
    $payload = jwt_decode($jwt);
    if (!$payload || !isset($payload['user_id'])) json(401, ['error' => 'Token invalide ou expiré']);
    $userId = (int)$payload['user_id'];

    // Update last seen
    try {
        $db = getDB();
        $db->exec("UPDATE users SET last_seen = NOW() WHERE id = $userId");
    } catch (Exception $e) {}

    return $userId;
}

function generateToken(int $userId, string $email): string {
    return jwt_encode(['user_id' => $userId, 'email' => $email]);
}

function sendNotification(?int $userId, string $title, string $message, string $type = 'system', ?int $relatedId = null): void {
    try {
        $db = getDB();
        $stmt = $db->prepare('INSERT INTO notifications (user_id, title, message, type, related_id) VALUES (?, ?, ?, ?, ?)');
        $stmt->execute([$userId, $title, $message, $type, $relatedId]);
    } catch (Exception $e) {
        error_log("sendNotification error: " . $e->getMessage());
    }
}
