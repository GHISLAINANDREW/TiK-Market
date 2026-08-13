<?php
require_once __DIR__ . '/../config/database.php';

try {
    $db = getDB();
    echo "<h1>🛠️ Réparation Globale TiK-Market</h1>";

    // 1. Table USERS
    echo "Mise à jour des rôles utilisateurs... ";
    $db->exec("ALTER TABLE users MODIFY COLUMN role ENUM('buyer', 'vendor', 'admin', 'super_admin') NOT NULL DEFAULT 'buyer'");

    $check = $db->query("SHOW COLUMNS FROM users LIKE 'managed_city'")->fetch();
    if (!$check) {
        $db->exec("ALTER TABLE users ADD COLUMN managed_city VARCHAR(100) DEFAULT NULL AFTER location");
        echo "✅ Colonne 'managed_city' ajoutée.<br>";
    }

    // 2. Table STORIES (Le point bloquant pour les vendeurs/admins)
    echo "Vérification de la table 'stories'... ";
    $db->exec("CREATE TABLE IF NOT EXISTS stories (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        shop_id INT NULL,
        media_url VARCHAR(500) NOT NULL,
        media_type VARCHAR(10) NOT NULL DEFAULT 'image',
        caption TEXT,
        duration INT DEFAULT 0,
        is_admin TINYINT(1) NOT NULL DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE SET NULL
    )");
    // S'assurer que shop_id peut être NULL (pour les stories admin)
    $db->exec("ALTER TABLE stories MODIFY COLUMN shop_id INT NULL");

    // Ajouter is_admin si manquant
    $check = $db->query("SHOW COLUMNS FROM stories LIKE 'is_admin'")->fetch();
    if (!$check) {
        $db->exec("ALTER TABLE stories ADD COLUMN is_admin TINYINT(1) NOT NULL DEFAULT 0 AFTER duration");
    }
    echo "✅ Table stories réparée.<br>";

    // 3. Table PROMOTIONS
    echo "Mise à jour des promotions... ";
    $cols = ['discount_fixed' => "INT NOT NULL DEFAULT 0", 'min_amount' => "INT NOT NULL DEFAULT 0"];
    foreach ($cols as $col => $def) {
        $check = $db->query("SHOW COLUMNS FROM promotions LIKE '$col'")->fetch();
        if (!$check) $db->exec("ALTER TABLE promotions ADD COLUMN $col $def");
    }
    echo "✅ Table promotions à jour.<br>";

    // 4. Droits Super Admin
    $db->exec("UPDATE users SET role = 'super_admin' WHERE email = 'AdminTikMarket@gmail.com' OR email = 'admin@dschangmarket.com'");
    echo "✅ Votre compte est maintenant Super Admin.<br>";

    echo "<h2>🚀 TOUT EST RÉPARÉ !</h2>";
    echo "<p>Veuillez rafraîchir l'application maintenant.</p>";

} catch (Exception $e) {
    echo "<h2 style='color:red;'>❌ Erreur : " . $e->getMessage() . "</h2>";
}
