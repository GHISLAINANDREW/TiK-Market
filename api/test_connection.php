<?php
header('Content-Type: application/json');
require_once __DIR__ . '/config/database.php';

$debug = [
    'host' => getenv('DB_HOST'),
    'port' => getenv('DB_PORT'),
    'user' => getenv('DB_USER'),
    'db' => getenv('DB_NAME'),
    'ssl_env' => getenv('DB_SSL'),
    'ca_file_exists' => file_exists(__DIR__ . '/config/ca.pem'),
];

try {
    $db = getDB();
    echo json_encode([
        'success' => true,
        'message' => 'Connexion réussie !',
        'debug' => $debug
    ]);
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'error' => $e->getMessage(),
        'debug' => $debug
    ]);
}
