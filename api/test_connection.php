<?php
header('Content-Type: application/json');

// On n'utilise pas require config pour tester en isolation totale
$host = getenv('DB_HOST');
$user = getenv('DB_USER');
$pass = getenv('DB_PASS');

$report = [
    'env_check' => [
        'host' => $host ? 'DÉFINI' : 'VIDE',
        'user' => $user ? 'DÉFINI' : 'VIDE',
        'pass' => $pass ? 'DÉFINI' : 'VIDE',
    ],
    'attempts' => []
];

$test_cases = [
    ['db' => 'defaultdb', 'ssl' => true],
    ['db' => 'defaultdb', 'ssl' => false],
    ['db' => 'tik_market', 'ssl' => true],
    ['db' => 'tik_market', 'ssl' => false],
];

foreach ($test_cases as $case) {
    $db_name = $case['db'];
    $use_ssl = $case['ssl'];
    $label = "Test: DB=$db_name | SSL=" . ($use_ssl ? 'OUI' : 'NON');

    try {
        $dsn = "mysql:host=$host;port=10180;dbname=$db_name;charset=utf8mb4";
        $options = [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_TIMEOUT => 5
        ];

        if ($use_ssl) {
            $ca = __DIR__ . '/config/ca.pem';
            if (file_exists($ca)) {
                $options[PDO::MYSQL_ATTR_SSL_CA] = $ca;
                $options[PDO::MYSQL_ATTR_SSL_VERIFY_SERVER_CERT] = false;
            } else {
                throw new Exception("Fichier ca.pem manquant");
            }
        }

        $db = new PDO($dsn, $user, $pass, $options);
        $report['attempts'][$label] = "✅ SUCCÈS";
        $report['working_config'] = ['db' => $db_name, 'ssl' => $use_ssl];
    } catch (Exception $e) {
        $report['attempts'][$label] = "❌ " . $e->getMessage();
    }
}

echo json_encode($report, JSON_PRETTY_PRINT);
