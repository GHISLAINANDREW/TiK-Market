<?php
require_once __DIR__ . '/../config/database.php';
$db = getDB();
$stmt = $db->query("SELECT id, order_number, status, vendor_confirmed, client_confirmed FROM orders ORDER BY id DESC LIMIT 5");
echo json_encode($stmt->fetchAll());
