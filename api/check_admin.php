<?php
require_once __DIR__ . '/config/database.php';
$db = getDB();
$stmt = $db->query("SELECT id, name, email, role, password FROM users WHERE email='admin@dschangmarket.com'");
$admin = $stmt->fetch();
echo json_encode($admin, JSON_UNESCAPED_UNICODE);
