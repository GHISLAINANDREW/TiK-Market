<?php
require_once __DIR__ . '/../config/database.php';

try {
    // getDB() utilise automatiquement les variables d'environnement de Render (Aiven)
    $db = getDB();

    echo "<h1>Utilisateurs Privilégiés sur AIVEN</h1>";
    echo "<table border='1' cellpadding='10'>";
    echo "<tr><th>ID</th><th>Nom</th><th>Email</th><th>Rôle</th><th>Ville Gérée</th><th>Dernière Connexion</th></tr>";

    $stmt = $db->query("SELECT id, name, email, role, managed_city, last_seen FROM users WHERE role IN ('admin', 'super_admin') ORDER BY role DESC");

    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $roleColor = ($row['role'] === 'super_admin') ? 'red' : 'blue';
        echo "<tr>";
        echo "<td>{$row['id']}</td>";
        echo "<td>{$row['name']}</td>";
        echo "<td>{$row['email']}</td>";
        echo "<td style='color: $roleColor; font-weight: bold;'>" . strtoupper($row['role']) . "</td>";
        echo "<td>" . ($row['managed_city'] ?: 'Toutes') . "</td>";
        echo "<td>{$row['last_seen']}</td>";
        echo "</tr>";
    }
    echo "</table>";

    echo "<p><em>Note : Si vous ne voyez pas 'SUPER_ADMIN', exécutez d'abord <a href='/api/fix_super_admin.php'>fix_super_admin.php</a></em></p>";

} catch (Exception $e) {
    echo "<h2>Erreur de connexion à Aiven</h2>";
    echo "<p>" . $e->getMessage() . "</p>";
}
