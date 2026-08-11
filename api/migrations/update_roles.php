<?php
require_once __DIR__ . '/../config/database.php';

try {
    $db = getDB();

    // Update users table role enum and add managed_city
    echo "Updating users table...\n";

    // Check if managed_city exists
    $check = $db->query("SHOW COLUMNS FROM users LIKE 'managed_city'")->fetch();
    if (!$check) {
        $db->exec("ALTER TABLE users MODIFY COLUMN role ENUM('buyer','vendor','admin','super_admin') NOT NULL DEFAULT 'buyer'");
        $db->exec("ALTER TABLE users ADD COLUMN managed_city VARCHAR(100) DEFAULT NULL AFTER location");
        echo "Table users updated successfully.\n";
    } else {
        echo "Table users already has managed_city column.\n";
    }

    // Set a default super admin if none exists
    $stmt = $db->query("SELECT id FROM users WHERE role = 'super_admin' LIMIT 1");
    if (!$stmt->fetch()) {
        echo "No super_admin found. You should promote an existing admin or user to super_admin.\n";
        // Optionally: promote the first admin to super_admin
        $stmt = $db->query("SELECT id FROM users WHERE role = 'admin' LIMIT 1");
        $firstAdmin = $stmt->fetch();
        if ($firstAdmin) {
            $db->exec("UPDATE users SET role = 'super_admin' WHERE id = " . $firstAdmin['id']);
            echo "User ID " . $firstAdmin['id'] . " promoted to super_admin.\n";
        }
    }

    echo "Migration completed.\n";
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
