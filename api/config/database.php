<?php
date_default_timezone_set('Africa/Douala');

/**
 * CORS : liste blanche d'origines au lieu de "*".
 * Le client natif (Android) n'envoie pas d'en-tête Origin → aucune contrainte pour lui.
 * Les navigateurs ne reçoivent l'autorisation que pour les origines connues.
 */
function sendCorsHeaders(): void {
    $origin = $_SERVER['HTTP_ORIGIN'] ?? '';
    if ($origin !== '') {
        $host = strtolower(parse_url($origin, PHP_URL_HOST) ?: '');
        $allowed =
            str_ends_with($host, '.vercel.app') ||   // prod + déploiements de prévisualisation
            $host === 'localhost' ||
            $host === '127.0.0.1';                    // développement local
        if ($allowed) {
            header('Access-Control-Allow-Origin: ' . $origin);
            header('Vary: Origin');
        }
    }
    header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Platform');
}

header('Content-Type: application/json; charset=utf-8');
sendCorsHeaders();

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }

/**
 * Connect to the database using Environment Variables.
 */
function getDB(): PDO {
    $host = getenv('DB_HOST') ?: '127.0.0.1';
    $port = getenv('DB_PORT') ?: '3306';
    $name = getenv('DB_NAME') ?: 'defaultdb';
    $user = getenv('DB_USER') ?: 'root';
    $pass = getenv('DB_PASS') ?: '';

    $dsn = "mysql:host=$host;port=$port;dbname=$name;charset=utf8mb4";
    $options = [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
        PDO::ATTR_TIMEOUT => 10,
    ];

    // Aiven SSL Support
    $localCa = __DIR__ . '/ca.pem';
    $caCert = getenv('DB_SSL_CA') ?: (file_exists($localCa) ? $localCa : null);

    if ($caCert && file_exists($caCert)) {
        $options[PDO::MYSQL_ATTR_SSL_CA] = $caCert;
        $options[PDO::MYSQL_ATTR_SSL_VERIFY_SERVER_CERT] = false;
    }

    try {
        $pdo = new PDO($dsn, $user, $pass, $options);
        // Ensure we are using UTF-8 and Cameroon time
        $pdo->exec("SET names utf8mb4");
        $pdo->exec("SET time_zone = '+01:00'");
        return $pdo;
    } catch (PDOException $e) {
        // Repli : nouvelle tentative SANS SSL (certains serveurs préfèrent ça)
        if (isset($options[PDO::MYSQL_ATTR_SSL_CA])) {
            try {
                unset($options[PDO::MYSQL_ATTR_SSL_CA]);
                unset($options[PDO::MYSQL_ATTR_SSL_VERIFY_SERVER_CERT]);
                $pdo = new PDO($dsn, $user, $pass, $options);
                $pdo->exec("SET names utf8mb4");
                $pdo->exec("SET time_zone = '+01:00'");
                return $pdo;
            } catch (PDOException $e2) {
                error_log('[TiK-Market] DB connection failed (avec et sans SSL): ' . $e2->getMessage());
            }
        } else {
            error_log('[TiK-Market] DB connection failed: ' . $e->getMessage());
        }
        // Détails complets côté logs serveur uniquement — jamais renvoyés au client.
        json(500, ['error' => 'Service temporairement indisponible']);
        exit;
    }
}

/**
 * Replaces local URLs with the production URL in the JSON response.
 */
function rewriteUrls($data) {
    $oldBase = 'http://192.168.1.230:8081';
    $newBase = getenv('APP_URL') ?: ('https://tik-market.onrender.com');
    $cloudName = getenv('CLOUDINARY_CLOUD_NAME');

    if (is_string($data)) {
        $data = str_replace($oldBase, $newBase, $data);
        if (str_starts_with($data, 'uploads/') || str_starts_with($data, '/uploads/')) {
            $data = $newBase . '/' . ltrim($data, '/');
        }
        return $data;
    }
    if (is_array($data)) {
        foreach ($data as $key => $value) {
            $data[$key] = rewriteUrls($value);
        }
    }
    if (is_object($data)) {
        foreach ($data as $key => $value) {
            $data->$key = rewriteUrls($value);
        }
    }
    return $data;
}

function json(int $code, $data): void {
    http_response_code($code);
    echo json_encode(rewriteUrls($data), JSON_UNESCAPED_UNICODE);
    exit;
}

/**
 * SÉCURITÉ : le secret JWT doit être fourni via la variable d'environnement JWT_SECRET.
 * - En production (DB_HOST défini, ex: Render + Aiven) : refus explicite si absent,
 *   sinon quiconque lit le dépôt peut forger des tokens valides.
 * - En local (XAMPP, DB_HOST absent) : secret de développement isolé.
 */
$jwtSecret = getenv('JWT_SECRET');
if (!$jwtSecret) {
    if (getenv('DB_HOST')) {
        error_log('[TiK-Market] FATAL: variable d\'environnement JWT_SECRET manquante en production.');
        http_response_code(500);
        echo json_encode(['error' => 'Configuration serveur incomplète']);
        exit;
    }
    $jwtSecret = 'dev_only_tik_market_local_secret_never_in_prod';
}
define('JWT_SECRET', $jwtSecret);
define('JWT_EXPIRY', 86400 * 30); // 30 days

function base64url_encode(string $data): string {
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}

function base64url_decode(string $data): string {
    $remainder = strlen($data) % 4;
    if ($remainder > 0) { $data .= str_repeat('=', 4 - $remainder); }
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
    $token = '';
    $headers = function_exists('getallheaders') ? getallheaders() : [];
    if (!empty($headers['Authorization'])) {
        $token = $headers['Authorization'];
    } elseif (!empty($_SERVER['HTTP_AUTHORIZATION'])) {
        $token = $_SERVER['HTTP_AUTHORIZATION'];
    }

    if (!str_starts_with($token, 'Bearer ')) json(401, ['error' => 'Non authentifié']);
    $jwt = substr($token, 7);
    $payload = jwt_decode($jwt);
    if (!$payload || !isset($payload['user_id'])) json(401, ['error' => 'Token invalide ou expiré']);
    $userId = (int)$payload['user_id'];

    try {
        $db = getDB();
        $db->exec("UPDATE users SET last_seen = NOW() WHERE id = $userId");
    } catch (Exception $e) {}

    return $userId;
}

function getUserRole(): string {
    $userId = getAuthUserId();
    try {
        $db = getDB();
        $stmt = $db->prepare('SELECT role FROM users WHERE id = ?');
        $stmt->execute([$userId]);
        return $stmt->fetchColumn() ?: 'buyer';
    } catch (Exception $e) {
        return 'buyer';
    }
}

function generateToken(int $userId, string $email): string {
    return jwt_encode(['user_id' => $userId, 'email' => $email]);
}

function uploadToCloudinary(string $fileData, string $mimeType = 'audio/mp4', string $folder = 'voices', string $prefix = 'msg_'): ?string {
    $cloudName = getenv('CLOUDINARY_CLOUD_NAME');
    $apiKey = getenv('CLOUDINARY_API_KEY');
    $apiSecret = getenv('CLOUDINARY_API_SECRET');

    if ($cloudName && $apiKey && $apiSecret && function_exists('curl_init')) {
        $timestamp = time();
        $resourceType = 'auto';
        $url = "https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload";
        $base64Data = base64_encode($fileData);
        $dataUri = "data:$mimeType;base64,$base64Data";
        $signature = sha1("timestamp=$timestamp$apiSecret");
        $postData = [
            'file' => $dataUri,
            'timestamp' => $timestamp,
            'api_key' => $apiKey,
            'signature' => $signature
        ];
        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
        curl_setopt($ch, CURLOPT_TIMEOUT, 30);
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        if ($response && $httpCode === 200) {
            $result = json_decode($response, true);
            if (isset($result['secure_url'])) { return $result['secure_url']; }
        }
    }

    $subDir = $folder;
    $targetDir = __DIR__ . '/../uploads/' . $subDir . '/';
    if (!is_dir($targetDir)) mkdir($targetDir, 0777, true);
    $extMap = [ 'audio/mp4' => 'mp4', 'audio/mpeg' => 'mp3', 'video/mp4' => 'mp4', 'image/jpeg' => 'jpg', 'image/png' => 'png' ];
    $extension = $extMap[$mimeType] ?? 'bin';
    $filename = $prefix . time() . '_' . bin2hex(random_bytes(4)) . '.' . $extension;
    if (file_put_contents($targetDir . $filename, $fileData)) {
        $protocol = 'https';
        $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
        return "$protocol://$host/uploads/$subDir/$filename";
    }
    return null;
}

function awardPoints(PDO $db, int $userId, int $points, string $description, string $referenceType = 'order', int $referenceId = 0): bool {
    try {
        if ($points <= 0) return true;
        $stmt = $db->prepare('SELECT id, total_points, tier FROM wallets WHERE user_id = ?');
        $stmt->execute([$userId]);
        $wallet = $stmt->fetch();
        if (!$wallet) {
            $stmt = $db->prepare('INSERT INTO wallets (user_id) VALUES (?)');
            $stmt->execute([$userId]);
            $walletId = (int)$db->lastInsertId();
            $currentTier = 'bronze';
        } else {
            $walletId = (int)$wallet['id'];
            $currentTier = $wallet['tier'];
        }
        $db->beginTransaction();
        $stmt = $db->prepare('UPDATE wallets SET total_points = COALESCE(total_points, 0) + ?, current_points = COALESCE(current_points, 0) + ? WHERE id = ?');
        $stmt->execute([$points, $points, $walletId]);
        $stmt = $db->prepare('INSERT INTO wallet_transactions (wallet_id, type, points, description, reference_type, reference_id) VALUES (?, ?, ?, ?, ?, ?)');
        $stmt->execute([$walletId, 'bonus', $points, $description, $referenceType, $referenceId]);
        $stmt = $db->prepare('SELECT total_points FROM wallets WHERE id = ?');
        $stmt->execute([$walletId]);
        $newTotal = (int)$stmt->fetchColumn();
        $tiers = $db->query('SELECT name, min_points FROM loyalty_tiers ORDER BY min_points DESC')->fetchAll();
        $newTier = 'bronze';
        foreach ($tiers as $t) { if ($newTotal >= (int)$t['min_points']) { $newTier = $t['name']; break; } }
        if ($newTier !== $currentTier) { $stmt = $db->prepare('UPDATE wallets SET tier = ? WHERE id = ?'); $stmt->execute([$newTier, $walletId]); }
        $db->commit();
        return true;
    } catch (Exception $e) { if (isset($db) && $db->inTransaction()) $db->rollBack(); return false; }
}

function handleOrderDelivery(PDO $db, int $orderId): void {
    try {
        $stmt = $db->prepare('SELECT user_id, total_amount, status FROM orders WHERE id = ?');
        $stmt->execute([$orderId]);
        $order = $stmt->fetch();
        if (!$order) return;
        $buyerId = (int)$order['user_id'];
        $amount = (float)$order['total_amount'];
        $stmtItems = $db->prepare('SELECT product_id, quantity FROM order_items WHERE order_id = ?');
        $stmtItems->execute([$orderId]);
        $items = $stmtItems->fetchAll();
        foreach ($items as $item) { $stmtUpd = $db->prepare('UPDATE products SET total_sales = COALESCE(total_sales, 0) + ? WHERE id = ?'); $stmtUpd->execute([(int)$item['quantity'], (int)$item['product_id']]); }
        $db->prepare('INSERT IGNORE INTO wallets (user_id) VALUES (?)')->execute([$buyerId]);
        $stmtW = $db->prepare('SELECT w.id, w.tier, lt.bonus_pct, lt.cashback_pct FROM wallets w LEFT JOIN loyalty_tiers lt ON w.tier = lt.name WHERE w.user_id = ?');
        $stmtW->execute([$buyerId]);
        $wallet = $stmtW->fetch();
        if ($wallet) {
            $walletId = (int)$wallet['id'];
            $stmtCheck = $db->prepare("SELECT id FROM wallet_transactions WHERE wallet_id = ? AND reference_type = 'order' AND reference_id = ? AND (type = 'bonus' OR type = 'earn') LIMIT 1");
            $stmtCheck->execute([$walletId, $orderId]);
            if (!$stmtCheck->fetch()) {
                $bonusPct = (float)($wallet['bonus_pct'] ?? 0);
                $cashbackPct = (float)($wallet['cashback_pct'] ?? 1.0);

                // Calcul plus généreux : 1 point pour 10 FCFA (au lieu de 100)
                $basePoints = (int)floor($amount / 10);
                $bonusPoints = (int)floor($basePoints * $bonusPct / 100);
                $totalBuyerPoints = $basePoints + $bonusPoints;

                $cashbackAmount = (int)round($amount * $cashbackPct / 100);
                if ($totalBuyerPoints > 0 || $cashbackAmount > 0) {
                    $wasInTransaction = $db->inTransaction();
                    if (!$wasInTransaction) $db->beginTransaction();
                    $stmtUpdWallet = $db->prepare('UPDATE wallets SET balance = balance + ?, total_points = total_points + ?, current_points = current_points + ?, lifetime_spent = lifetime_spent + ? WHERE id = ?');
                    $stmtUpdWallet->execute([$cashbackAmount, $totalBuyerPoints, $totalBuyerPoints, $amount, $walletId]);
                    $stmtTrans = $db->prepare('INSERT INTO wallet_transactions (wallet_id, type, amount_fcfa, points, description, reference_type, reference_id) VALUES (?, ?, ?, ?, ?, ?, ?)');
                    if ($cashbackAmount > 0) { $stmtTrans->execute([$walletId, 'earn', (float)$cashbackAmount, 0, "Cashback $cashbackPct% sur achat #$orderId", 'order', $orderId]); }
                    if ($totalBuyerPoints > 0) { $stmtTrans->execute([$walletId, 'bonus', 0, (int)$totalBuyerPoints, "Points fidélité sur achat #$orderId", 'order', $orderId]); }
                    if (!$wasInTransaction) $db->commit();
                    sendNotification($buyerId, "Fidélité récompensée 🎉", "Vous avez gagné " . ($totalBuyerPoints > 0 ? "$totalBuyerPoints points" : "") . ($cashbackAmount > 0 ? " et $cashbackAmount FCFA" : "") . " !", 'order', $orderId);
                }
            }
        }
        $stmtV = $db->prepare('SELECT DISTINCT s.vendor_id FROM order_items oi JOIN products p ON oi.product_id = p.id JOIN shops s ON p.shop_id = s.id WHERE oi.order_id = ?');
        $stmtV->execute([$orderId]);
        $vendorIds = $stmtV->fetchAll(PDO::FETCH_COLUMN);
        foreach ($vendorIds as $vId) {
            $vId = (int)$vId;
            if ($vId > 0) {
                $stmtCheckV = $db->prepare("SELECT id FROM wallet_transactions WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = ?) AND reference_type = 'order' AND reference_id = ? AND type = 'bonus' LIMIT 1");
                $stmtCheckV->execute([$vId, $orderId]);
                if (!$stmtCheckV->fetch()) {
                    awardPoints($db, $vId, 5, "Vente réussie #$orderId", 'order', $orderId);
                    sendNotification($vId, "Point fidélité gagné 🎉", "Vous avez gagné 5 points pour la vente #$orderId.", 'order', $orderId);
                }
            }
        }
    } catch (Exception $e) {}
}

function sendNotification(?int $userId, string $title, string $message, string $type = 'system', ?int $relatedId = null): void {
    try {
        $db = getDB();
        $stmt = $db->prepare('INSERT INTO notifications (user_id, title, message, type, related_id) VALUES (?, ?, ?, ?, ?)');
        $stmt->execute([$userId, $title, $message, $type, $relatedId]);
    } catch (Exception $e) {}
}
