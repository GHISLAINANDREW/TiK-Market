<?php
/**
 * Migration: Ajoute les colonnes de confirmation pour le vendeur et le client
 *
 * Usage: GET /api/migrations/add_order_confirmation_flags.php
 */
require_once __DIR__ . '/../config/database.php';

$db = getDB();
$migrations = [];

try {
    $db->exec("ALTER TABLE orders ADD COLUMN vendor_confirmed TINYINT(1) DEFAULT 0");
    $migrations[] = "✅ Colonne `vendor_confirmed` ajoutée à orders";
} catch (PDOException $e) {
    $migrations[] = "ℹ️ Colonne `vendor_confirmed` existe peut-être déjà : " . $e->getMessage();
}

try {
    $db->exec("ALTER TABLE orders ADD COLUMN client_confirmed TINYINT(1) DEFAULT 0");
    $migrations[] = "✅ Colonne `client_confirmed` ajoutée à orders";
} catch (PDOException $e) {
    $migrations[] = "ℹ️ Colonne `client_confirmed` existe peut-être déjà : " . $e->getMessage();
}

echo json_encode(['migrations' => $migrations]);
