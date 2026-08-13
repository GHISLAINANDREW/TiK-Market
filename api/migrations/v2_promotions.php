<?php
require_once __DIR__ . '/../config/database.php';

try {
    $db = getDB();
    echo "<h1>Migration Promotions v2</h1>";

    // 1. Ajouter les colonnes manquantes
    $colsToAdd = [
        'discount_fixed' => "INT NOT NULL DEFAULT 0 AFTER discount_pct",
        'min_amount' => "INT NOT NULL DEFAULT 0 AFTER discount_fixed"
    ];

    foreach ($colsToAdd as $col => $def) {
        $check = $db->query("SHOW COLUMNS FROM promotions LIKE '$col'")->fetch();
        if (!$check) {
            $db->exec("ALTER TABLE promotions ADD COLUMN $def");
            echo "✅ Colonne '$col' ajoutée.<br>";
        } else {
            echo "ℹ️ Colonne '$col' déjà présente.<br>";
        }
    }

    echo "<b>Migration terminée !</b>";
} catch (Exception $e) {
    echo "❌ Erreur : " . $e->getMessage();
}
