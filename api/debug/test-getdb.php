<?php
require_once __DIR__ . '/../config/database.php';
$db = getDB();
$stmt = $db->query('SELECT 1 AS ok');
$row = $stmt->fetch();
echo json_encode(['ok' => $row['ok']]);
