<?php
header('Content-Type: application/json; charset=utf-8');

$result = [
    'status' => 'running',
    'php_version' => phpversion(),
    'env' => [],
    'dns' => null,
    'tcp' => null,
    'pdo_without_ssl' => null,
    'pdo_with_ssl' => null,
    'pdo_drivers' => PDO::getAvailableDrivers(),
    'openssl' => extension_loaded('openssl'),
];

// ── Env vars (redacted) ──
foreach (['DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASS', 'DB_SSL'] as $key) {
    $val = getenv($key) ?: '(not set)';
    if ($key === 'DB_PASS' && $val !== '(not set)') {
        $val = substr($val, 0, 3) . '***';
    }
    $result['env'][$key] = $val;
}

// ── DNS resolution ──
$host = getenv('DB_HOST') ?: 'localhost';
$port = getenv('DB_PORT') ?: '3306';
$hostIp = gethostbyname($host);
$result['dns'] = "$host → $hostIp";
if ($hostIp === $host) {
    $result['dns'] .= ' (DNS LOOKUP FAILED)';
}

// ── TCP connection test ──
$result['tcp'] = 'testing...';
$errno = 0;
$errstr = '';
$fp = @fsockopen($host, (int)$port, $errno, $errstr, 5);
if ($fp) {
    $result['tcp'] = "SUCCESS - Port $port open";
    fclose($fp);
} else {
    $result['tcp'] = "FAILED - $errstr ($errno)";
}

// ── SSL CA certificates check ──
$caPaths = [
    '/etc/ssl/certs/ca-certificates.crt',
    '/etc/pki/tls/certs/ca-bundle.crt',
    '/etc/ssl/cert.pem',
    '/etc/pki/tls/cert.pem',
];
$result['ca_certs'] = [];
foreach ($caPaths as $path) {
    $result['ca_certs'][$path] = file_exists($path) ? 'EXISTS (' . filesize($path) . ' bytes)' : 'NOT FOUND';
}

// ── PDO without SSL ──
try {
    $dsn = "mysql:host=$host;port=$port;dbname=" . (getenv('DB_NAME') ?: 'defaultdb') . ";charset=utf8mb4";
    $user = getenv('DB_USER') ?: 'root';
    $pass = getenv('DB_PASS') ?: '';
    $pdo = new PDO($dsn, $user, $pass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_TIMEOUT => 5,
    ]);
    $result['pdo_without_ssl'] = 'SUCCESS';
} catch (Exception $e) {
    $result['pdo_without_ssl'] = $e->getMessage();
}

// ── PDO with SSL ──
try {
    $dsn = "mysql:host=$host;port=$port;dbname=" . (getenv('DB_NAME') ?: 'defaultdb') . ";charset=utf8mb4";
    $user = getenv('DB_USER') ?: 'root';
    $pass = getenv('DB_PASS') ?: '';
    $caCert = getenv('DB_SSL_CA') ?: '/etc/ssl/certs/ca-certificates.crt';
    $pdo = new PDO($dsn, $user, $pass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_TIMEOUT => 5,
        PDO::MYSQL_ATTR_SSL_CA => $caCert,
        PDO::MYSQL_ATTR_SSL_VERIFY_SERVER_CERT => true,
    ]);
    $result['pdo_with_ssl'] = 'SUCCESS';
} catch (Exception $e) {
    $result['pdo_with_ssl'] = $e->getMessage();
}

// ── Test query (non-SSL connection) ──
try {
    $dsn = "mysql:host=$host;port=$port;dbname=" . (getenv('DB_NAME') ?: 'defaultdb') . ";charset=utf8mb4";
    $user = getenv('DB_USER') ?: 'root';
    $pass = getenv('DB_PASS') ?: '';
    $pdo = new PDO($dsn, $user, $pass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    // Test simple query
    $stmt = $pdo->query('SELECT 1 AS test');
    $row = $stmt->fetch();
    $result['test_query'] = 'SUCCESS: ' . json_encode($row);
    
    // List tables
    $stmt = $pdo->query('SHOW TABLES');
    $tables = $stmt->fetchAll(PDO::FETCH_COLUMN);
    $result['tables'] = $tables;
} catch (Exception $e) {
    $result['test_query'] = 'FAILED: ' . $e->getMessage();
    $result['tables'] = [];
}

// ── Test the SAME DSN as database.php ──
try {
    $name = getenv('DB_NAME') ?: 'defaultdb';
    $dsn2 = "mysql:host=$host;port=$port;dbname=$name;charset=utf8mb4";
    $pdo2 = new PDO($dsn2, getenv('DB_USER') ?: 'root', getenv('DB_PASS') ?: '', [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
    // Test a real query that products.php uses
    $stmt2 = $pdo2->query('SELECT COUNT(*) as cnt FROM products');
    $row2 = $stmt2->fetch();
    $result['same_dsn_test'] = 'SUCCESS - ' . json_encode($row2);
    
    // Test products JOIN query (same as products.php)
    $joinStmt = $pdo2->prepare("
        SELECT p.id, p.title, p.image_url, s.name AS shop_name
        FROM products p
        JOIN shops s ON p.shop_id = s.id
        JOIN users u ON s.vendor_id = u.id
        WHERE p.is_active = 1 AND s.status = 'active' AND u.status = 'active'
        LIMIT 5
    ");
    $joinStmt->execute();
    $products = $joinStmt->fetchAll();
    $result['products_sample'] = $products;
} catch (Exception $e) {
    $result['same_dsn_test'] = 'FAILED: ' . $e->getMessage();
}

$result['status'] = 'done';
echo json_encode($result, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
