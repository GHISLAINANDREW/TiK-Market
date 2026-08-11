<?php
require_once __DIR__ . '/config/database.php';

try {
    $db = getDB();

    // 1. Update role enum to include super_admin
    $db->exec("ALTER TABLE users MODIFY COLUMN role ENUM('buyer', 'vendor', 'admin', 'super_admin') NOT NULL DEFAULT 'buyer'");
    echo "Rôles mis à jour (ajout super_admin).<br>";

    // 2. Add managed_city column if not exists
    $check = $db->query("SHOW COLUMNS FROM users LIKE 'managed_city'")->fetch();
    if (!$check) {
        $db->exec("ALTER TABLE users ADD COLUMN managed_city VARCHAR(100) DEFAULT NULL AFTER location");
        echo "Colonne managed_city ajoutée.<br>";
    }

    // 3. Ensure there is at least one super_admin (e.g., the first admin)
    $stmt = $db->prepare("SELECT id FROM users WHERE role = 'super_admin' LIMIT 1");
    $stmt->execute();
    if (!$stmt->fetch()) {
        $db->exec("UPDATE users SET role = 'super_admin' WHERE role = 'admin' LIMIT 1");
        echo "Un administrateur a été promu en Super Admin.<br>";
    }

    echo "Migration terminée avec succès.";
} catch (Exception $e) {
    echo "Erreur : " . $e->getMessage();
}
