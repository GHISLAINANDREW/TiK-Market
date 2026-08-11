<?php
require_once __DIR__ . '/config/database.php';
try {
    $db = getDB();
    $stmt = $db->query("SELECT name, email, role, managed_city FROM users WHERE role IN ('admin', 'super_admin') ORDER BY role DESC");
    $results = $stmt->fetchAll();
    if (empty($results)) {
        echo "Aucun administrateur trouvé.";
    } else {
        foreach ($results as $row) {
            echo "Role: " . strtoupper($row['role']) . " | Nom: " . $row['name'] . " | Email: " . $row['email'] . ($row['managed_city'] ? " | Ville: " . $row['managed_city'] : "") . "\n";
        }
    }
} catch (Exception $e) {
    echo "Erreur : " . $e->getMessage();
}
