<?php
<?php
// DEPLOYMENT TEST 12345
header('Content-Type: application/json');
echo json_encode(['status' => 'ok', 'server' => 'render', 'version' => '1.0.2', 'time' => time()]);
