<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Platform');

date_default_timezone_set('Africa/Douala');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }

/**
 * Connect to the database using Environment Variables.
 * On Render/Aiven/TiDB, these will be set in the dashboard.
 * 
 * Required env vars for production:
 *   DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS
 *   CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET
 */
function getDB(): PDO {
    // ⚠️ IMPORTANT: For remote DB (Aiven, TiDB, etc.), set DB_HOST to the hostname.
    // Do NOT use "localhost" for remote databases — PHP interprets it as a Unix socket.
    $host = getenv('DB_HOST') ?: '127.0.0.1';
    $port = getenv('DB_PORT') ?: '3306';
    $name = getenv('DB_NAME') ?: 'tik_market';
    $user = getenv('DB_USER') ?: 'root';
    $pass = getenv('DB_PASS') ?: '';
    $sslMode = getenv('DB_SSL') ?: '';

    $dsn = "mysql:host=$host;port=$port;dbname=$name;charset=utf8mb4";
    $options = [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ];

    // Aiven: SSL cert doesn't work on this PHP build → connect without SSL
    // TLS encryption is still used if the server requires it.
    try {
        $pdo = new PDO($dsn, $user, $pass, $options);
        $pdo->exec("SET time_zone = '+01:00'"); // Cameroon time
        return $pdo;
    } catch (PDOException $e) {
        // Last resort: try with explicit SSL options
        try {
            $caCert = getenv('DB_SSL_CA') ?: '/etc/ssl/certs/ca-certificates.crt';
            if (file_exists($caCert)) {
                $options[PDO::MYSQL_ATTR_SSL_CA] = $caCert;
            }
            if (defined('PDO::MYSQL_ATTR_SSL_VERIFY_SERVER_CERT')) {
                $options[PDO::MYSQL_ATTR_SSL_VERIFY_SERVER_CERT] = false;
            }
            $pdo = new PDO($dsn, $user, $pass, $options);
            return $pdo;
        } catch (PDOException $e2) {
            json(500, ['error' => 'Database connection failed: ' . $e2->getMessage()]);
            exit;
        }
    }
}

/**
 * Replaces local URLs with the production URL in the JSON response.
 */
function rewriteUrls($data) {
    $oldBase = 'http://192.168.1.230:8081'; // Your old local IP
    $newBase = getenv('APP_URL') ?: ('https://tik-market.onrender.com');
    $cloudName = getenv('CLOUDINARY_CLOUD_NAME');

    if (is_string($data)) {
        // Replace old local IP with new Render URL
        $data = str_replace($oldBase, $newBase, $data);
        // If Cloudinary is configured and image URL is still local, rewrite to Cloudinary
        if ($cloudName && str_contains($data, $newBase . '/uploads/')) {
            // Image URL points to Render but file isn't there → keep it, user must re-upload
            // (old local images won't work until re-uploaded to Cloudinary)
        }
        // Fix relative upload paths
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

define('JWT_SECRET', getenv('JWT_SECRET') ?: 'tik_market_jwt_secret_2026_change_in_production');
define('JWT_EXPIRY', 86400 * 30); // 30 days

function base64url_encode(string $data): string {
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}

function base64url_decode(string $data): string {
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

    try {
        $db = getDB();
        $db->exec("UPDATE users SET last_seen = NOW() WHERE id = $userId");
    } catch (Exception $e) {}

    return $userId;
}

function generateToken(int $userId, string $email): string {
    return jwt_encode(['user_id' => $userId, 'email' => $email]);
}

/**
 * Upload file data to Cloudinary (or fallback to local storage).
 * 
 * @param string $fileData Raw binary file data
 * @param string $mimeType MIME type of the file (e.g. 'audio/mp4', 'image/jpeg')
 * @param string $folder   Cloudinary folder prefix (e.g. 'voices', 'chat_files')
 * @param string $prefix   File name prefix (e.g. 'msg_')
 * @return string|null     Public URL of the uploaded file, or null on failure
 */
function uploadToCloudinary(string $fileData, string $mimeType = 'audio/mp4', string $folder = 'voices', string $prefix = 'msg_'): ?string {
    $cloudName = getenv('CLOUDINARY_CLOUD_NAME');
    $apiKey = getenv('CLOUDINARY_API_KEY');
    $apiSecret = getenv('CLOUDINARY_API_SECRET');

    if ($cloudName && $apiKey && $apiSecret && function_exists('curl_init')) {
        // Upload to Cloudinary (signature = sha1 of sorted params + secret)
        $timestamp = time();
        
        // Auto-detect resource type
        $resourceType = 'auto';
        
        $url = "https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload";
        
        $base64Data = base64_encode($fileData);
        $dataUri = "data:$mimeType;base64,$base64Data";
        
        // Only sign timestamp (public_id not included, Cloudinary auto-generates it)
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
        $error = curl_error($ch);
        curl_close($ch);

        if ($response && $httpCode === 200) {
            $result = json_decode($response, true);
            if (isset($result['secure_url'])) {
                return $result['secure_url'];
            }
        }
        
        error_log("[uploadToCloudinary] HTTP $httpCode error: " . ($error ?: $response));
        // Fall through to local storage
    }

    // Fallback: local storage (files are lost on Render after restart)
    $subDir = $folder;
    $targetDir = __DIR__ . '/../uploads/' . $subDir . '/';
    if (!is_dir($targetDir)) mkdir($targetDir, 0777, true);

    // Determine extension from MIME
    $extMap = [
        'audio/mp4' => 'mp4',
        'audio/mpeg' => 'mp3',
        'audio/webm' => 'webm',
        'audio/ogg' => 'ogg',
        'audio/wav' => 'wav',
        'audio/amr' => 'amr',
        'audio/x-m4a' => 'm4a',
        'audio/aac' => 'aac',
        'video/mp4' => 'mp4',
        'video/webm' => 'webm',
    ];
    $extension = $extMap[$mimeType] ?? 'bin';
    $filename = $prefix . time() . '_' . bin2hex(random_bytes(4)) . '.' . $extension;

    if (file_put_contents($targetDir . $filename, $fileData)) {
        // Build URL relative to web root (no /api/ prefix on Docker)
        $protocol = 'https';
        $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
        $scriptPath = $_SERVER['SCRIPT_NAME'] ?? '/'; // e.g. /messages/messages.php
        $baseUrl = rtrim(dirname(dirname($scriptPath)), '/'); // /api/ or '' depending on deployment
        // If baseUrl ends with /api, strip it (Docker doesn't use /api/ prefix)
        if (str_ends_with($baseUrl, '/api') || $baseUrl === '/api') {
            $baseUrl = '';
        }
        return "$protocol://$host$baseUrl/uploads/$subDir/$filename";
    }

    return null;
}

/**
 * Award loyalty points to a user, creating wallet if needed.
 * Used e.g. after a successful transaction (1 point per actor).
 */
function awardPoints(PDO $db, int $userId, int $points, string $description, string $referenceType = 'order', int $referenceId = 0): bool {
    try {
        if ($points <= 0) return true;

        // Ensure wallet exists
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

        $stmt = $db->prepare('
            UPDATE wallets
            SET total_points = COALESCE(total_points, 0) + ?,
                current_points = COALESCE(current_points, 0) + ?
            WHERE id = ?
        ');
        $stmt->execute([$points, $points, $walletId]);

        // Record transaction
        $stmt = $db->prepare('
            INSERT INTO wallet_transactions (wallet_id, type, points, description, reference_type, reference_id)
            VALUES (?, ?, ?, ?, ?, ?)
        ');
        $stmt->execute([$walletId, 'bonus', $points, $description, $referenceType, $referenceId]);

        // Check tier upgrade
        $stmt = $db->prepare('SELECT total_points FROM wallets WHERE id = ?');
        $stmt->execute([$walletId]);
        $newTotal = (int)$stmt->fetchColumn();

        $tiers = $db->query('SELECT name, min_points FROM loyalty_tiers ORDER BY min_points DESC')->fetchAll();
        $newTier = 'bronze';
        foreach ($tiers as $t) {
            if ($newTotal >= (int)$t['min_points']) {
                $newTier = $t['name'];
                break;
            }
        }

        if ($newTier !== $currentTier) {
            $stmt = $db->prepare('UPDATE wallets SET tier = ? WHERE id = ?');
            $stmt->execute([$newTier, $walletId]);
        }

        $db->commit();
        return true;
    } catch (Exception $e) {
        if (isset($db) && $db->inTransaction()) $db->rollBack();
        error_log("awardPoints error (user #$userId): " . $e->getMessage());
        return false;
    }
}

/**
 * Handle all logic when an order is successfully delivered:
 * 1. Increment total_sales for each product
 * 2. Award points and cashback to the buyer (based on amount and tier)
 * 3. Award points to the vendor(s)
 */
function handleOrderDelivery(PDO $db, int $orderId): void {
    try {
        // 1. Get order details
        $stmt = $db->prepare('SELECT user_id, total_amount, status FROM orders WHERE id = ?');
        $stmt->execute([$orderId]);
        $order = $stmt->fetch();
        if (!$order) return;

        $buyerId = (int)$order['user_id'];
        $amount = (float)$order['total_amount'];

        // 2. Increment total_sales for each product
        try {
            $stmtItems = $db->prepare('SELECT product_id, quantity FROM order_items WHERE order_id = ?');
            $stmtItems->execute([$orderId]);
            $items = $stmtItems->fetchAll();

            foreach ($items as $item) {
                $stmtUpd = $db->prepare('UPDATE products SET total_sales = COALESCE(total_sales, 0) + ? WHERE id = ?');
                $stmtUpd->execute([(int)$item['quantity'], (int)$item['product_id']]);
            }
        } catch (Exception $e) {
            error_log("handleOrderDelivery sales update error (#$orderId): " . $e->getMessage());
        }

        // 3. Award points and cashback to Buyer
        // Ensure wallet exists
        $db->prepare('INSERT IGNORE INTO wallets (user_id) VALUES (?)')->execute([$buyerId]);

        $stmtW = $db->prepare('
            SELECT w.id, w.tier, lt.bonus_pct, lt.cashback_pct
            FROM wallets w
            LEFT JOIN loyalty_tiers lt ON w.tier = lt.name
            WHERE w.user_id = ?
        ');
        $stmtW->execute([$buyerId]);
        $wallet = $stmtW->fetch();

        if ($wallet) {
            $walletId = (int)$wallet['id'];

            // ─── Check if points already awarded for this order to this buyer ───
            $stmtCheck = $db->prepare("SELECT id FROM wallet_transactions WHERE wallet_id = ? AND reference_type = 'order' AND reference_id = ? AND (type = 'bonus' OR type = 'earn') LIMIT 1");
            $stmtCheck->execute([$walletId, $orderId]);
            if (!$stmtCheck->fetch()) {
                $bonusPct = (float)($wallet['bonus_pct'] ?? 0);
                $cashbackPct = (float)($wallet['cashback_pct'] ?? 1.0); // 1% par défaut pour bronze

                // Points: 1 point per 100 FCFA + tier bonus
                $basePoints = (int)floor($amount / 100);
                $bonusPoints = (int)floor($basePoints * $bonusPct / 100);
                $totalBuyerPoints = $basePoints + $bonusPoints;

                // Cashback: based on tier percentage
                $cashbackAmount = (int)round($amount * $cashbackPct / 100);

                if ($totalBuyerPoints > 0 || $cashbackAmount > 0) {
                    $wasInTransaction = $db->inTransaction();
                    if (!$wasInTransaction) $db->beginTransaction();

                    try {
                        $stmtUpdWallet = $db->prepare('
                            UPDATE wallets
                            SET balance = balance + ?,
                                total_points = total_points + ?,
                                current_points = current_points + ?,
                                lifetime_spent = lifetime_spent + ?
                            WHERE id = ?
                        ');
                        $stmtUpdWallet->execute([$cashbackAmount, $totalBuyerPoints, $totalBuyerPoints, $amount, $walletId]);

                        // Record transactions
                        $stmtTrans = $db->prepare('
                            INSERT INTO wallet_transactions (wallet_id, type, amount_fcfa, points, description, reference_type, reference_id)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                        ');
                        if ($cashbackAmount > 0) {
                            $stmtTrans->execute([$walletId, 'earn', (float)$cashbackAmount, 0, "Cashback " . ($cashbackPct) . "% sur achat #$orderId", 'order', $orderId]);
                        }
                        if ($totalBuyerPoints > 0) {
                            $stmtTrans->execute([$walletId, 'bonus', 0, (int)$totalBuyerPoints, "Points fidélité sur achat #$orderId", 'order', $orderId]);
                        }

                        // Tier check
                        $stmtSum = $db->prepare('SELECT total_points FROM wallets WHERE id = ?');
                        $stmtSum->execute([$walletId]);
                        $newTotal = (int)$stmtSum->fetchColumn();

                        $tiers = $db->query('SELECT name, min_points FROM loyalty_tiers ORDER BY min_points DESC')->fetchAll();
                        $newTier = 'bronze';
                        foreach ($tiers as $t) {
                            if ($newTotal >= (int)$t['min_points']) { $newTier = $t['name']; break; }
                        }
                        $db->prepare('UPDATE wallets SET tier = ? WHERE id = ?')->execute([$newTier, $walletId]);

                        if (!$wasInTransaction) $db->commit();

                        $notifMsg = "Vous avez gagné " . ($totalBuyerPoints > 0 ? "$totalBuyerPoints points" : "") .
                                   ($totalBuyerPoints > 0 && $cashbackAmount > 0 ? " et " : "") .
                                   ($cashbackAmount > 0 ? "$cashbackAmount FCFA de cashback" : "") . " !";
                        sendNotification($buyerId, "Fidélité récompensée 🎉", $notifMsg, 'order', $orderId);
                    } catch (Exception $e) {
                        if (!$wasInTransaction) $db->rollBack();
                        throw $e;
                    }
                }
            }
        }

        // 4. Award points to Vendors (Fixed 5 points per successful sale)
        $stmtV = $db->prepare('
            SELECT DISTINCT s.vendor_id
            FROM order_items oi
            JOIN products p ON oi.product_id = p.id
            JOIN shops s ON p.shop_id = s.id
            WHERE oi.order_id = ?
        ');
        $stmtV->execute([$orderId]);
        $vendorIds = $stmtV->fetchAll(PDO::FETCH_COLUMN);
        foreach ($vendorIds as $vId) {
            $vId = (int)$vId;
            if ($vId > 0) {
                // Check if vendor already awarded points for this order
                $stmtCheckV = $db->prepare("
                    SELECT wt.id FROM wallet_transactions wt
                    JOIN wallets w ON wt.wallet_id = w.id
                    WHERE w.user_id = ? AND wt.reference_type = 'order' AND wt.reference_id = ? AND wt.type = 'bonus'
                    LIMIT 1
                ");
                $stmtCheckV->execute([$vId, $orderId]);
                if (!$stmtCheckV->fetch()) {
                    awardPoints($db, $vId, 5, "Vente réussie #$orderId", 'order', $orderId);
                    sendNotification($vId, "Point fidélité gagné 🎉", "Vous avez gagné 5 points de fidélité pour la vente #$orderId.", 'order', $orderId);
                }
            }
        }

    } catch (Exception $e) {
        if ($db->inTransaction()) $db->rollBack();
        error_log("handleOrderDelivery error (#$orderId): " . $e->getMessage());
    }
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
