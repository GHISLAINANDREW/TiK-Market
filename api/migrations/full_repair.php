<?php
require_once __DIR__ . '/../config/database.php';

try {
    $db = getDB();
    echo "<h1>🛠️ Réparation Globale TiK-Market</h1>";

    // 1. Table USERS
    echo "Vérification de la table 'users'... ";
    $db->exec("ALTER TABLE users MODIFY COLUMN role ENUM('buyer', 'vendor', 'admin', 'super_admin') NOT NULL DEFAULT 'buyer'");

    $check = $db->query("SHOW COLUMNS FROM users LIKE 'managed_city'")->fetch();
    if (!$check) {
        $db->exec("ALTER TABLE users ADD COLUMN managed_city VARCHAR(100) DEFAULT NULL AFTER location");
        echo "✅ Colonne 'managed_city' ajoutée.<br>";
    } else {
        echo "ℹ️ Table users déjà à jour.<br>";
    }

    // 2. Table PROMOTIONS
    echo "Vérification de la table 'promotions'... ";
    $cols = ['discount_fixed' => "INT NOT NULL DEFAULT 0", 'min_amount' => "INT NOT NULL DEFAULT 0"];
    foreach ($cols as $col => $def) {
        $check = $db->query("SHOW COLUMNS FROM promotions LIKE '$col'")->fetch();
        if (!$check) {
            $db->exec("ALTER TABLE promotions ADD COLUMN $col $def");
            echo "✅ Colonne '$col' ajoutée.<br>";
        }
    }
    echo "ℹ️ Table promotions à jour.<br>";

    // 3. Table NOTIFICATIONS (Ajout de related_id si manquant)
    $check = $db->query("SHOW COLUMNS FROM notifications LIKE 'related_id'")->fetch();
    if (!$check) {
        $db->exec("ALTER TABLE notifications ADD COLUMN related_id INT DEFAULT NULL AFTER type");
        echo "✅ Colonne 'related_id' ajoutée.<br>";
    }

    // 4. S'assurer que VOUS êtes super_admin
    $db->exec("UPDATE users SET role = 'super_admin' WHERE role = 'admin' OR email LIKE 'admin%'");
    echo "✅ Permissions administrateurs synchronisées.<br>";

    echo "<h2>🚀 Réparation terminée avec succès !</h2>";
    echo "<p>Vous pouvez maintenant retourner sur l'application.</p>";

} catch (Exception $e) {
    echo "<h2 style='color:red;'>❌ Erreur : " . $e->getMessage() . "</h2>";
}
