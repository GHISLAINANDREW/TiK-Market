<?php
require_once __DIR__ . '/../config/database.php';
$db = getDB();
$stmt = $db->query("DESCRIBE orders");
echo json_encode($stmt->fetchAll());
