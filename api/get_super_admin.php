<?php
require_once __DIR__ . '/config/database.php';
try {
    $db = getDB();
    $stmt = $db->query("SELECT name, email, phone FROM users WHERE role = 'super_admin'");
    $results = $stmt->fetchAll();
    if (empty($results)) {
        echo "Aucun super_admin trouvé.";
    } else {
        foreach ($results as $row) {
            echo "Nom: " . $row['name'] . " | Email: " . $row['email'] . " | Tel: " . $row['phone'] . "\n";
        }
    }
} catch (Exception $e) {
    echo "Erreur : " . $e->getMessage();
}
