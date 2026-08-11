<?php
/**
 * Script d'initialisation pour la production sur Render.
 * Ce script met à jour la base de données et crée le Super Admin.
 */
require_once __DIR__ . '/config/database.php';

try {
    $db = getDB();

    echo "<h1>Initialisation Production TiK-Market</h1>";

    // 1. Migration de la table users
    echo "Mise à jour de la table users... ";
    $check = $db->query("SHOW COLUMNS FROM users LIKE 'managed_city'")->fetch();
    if (!$check) {
        $db->exec("ALTER TABLE users MODIFY COLUMN role ENUM('buyer','vendor','admin','super_admin') NOT NULL DEFAULT 'buyer'");
        $db->exec("ALTER TABLE users ADD COLUMN managed_city VARCHAR(100) DEFAULT NULL AFTER location");
        echo "✅ Colonnes ajoutées.<br>";
    } else {
        echo "ℹ️ Déjà à jour.<br>";
    }

    // 2. Création/Mise à jour du Super Admin
    echo "Configuration du Super Admin... ";
    $email = 'AdminTikMarket@gmail.com';
    $password = 'Admin123';
    $hashedPassword = password_hash($password, PASSWORD_BCRYPT);

    // Supprimer si existe déjà (pour reset propre)
    $stmt = $db->prepare("DELETE FROM users WHERE email = ?");
    $stmt->execute([$email]);

    // Insérer le Super Admin
    $stmt = $db->prepare("INSERT INTO users (name, email, phone, password, role, status, location) VALUES (?, ?, ?, ?, ?, 'active', ?)");
    $stmt->execute(['Super Admin Tik-Market', $email, '000000000', $hashedPassword, 'super_admin', 'Dschang']);

    $userId = $db->lastInsertId();
    $db->prepare("INSERT IGNORE INTO wallets (user_id) VALUES (?)")->execute([$userId]);

    echo "✅ Super Admin créé.<br>";
    echo "<br><b>Configuration terminée !</b><br>";
    echo "Vous pouvez maintenant vous connecter avec : <br>";
    echo "Email: $email<br>";
    echo "Pass: $password<br>";

} catch (Exception $e) {
    echo "❌ Erreur : " . $e->getMessage();
}
