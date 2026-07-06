<?php
/**
 * Migration: Ajoute la colonne `status` à la table users
 * Valeurs: 'active' (défaut), 'banned', 'suspended'
 * 
 * Usage: GET http://localhost:8081/migrations/add_status_column.php
 */
require_once __DIR__ . '/../config/database.php';

$db = getDB();
$migrations = [];

// 1. Ajouter status à users
try {
    $db->exec("ALTER TABLE users ADD COLUMN status ENUM('active','banned','suspended') NOT NULL DEFAULT 'active' AFTER role");
    $migrations[] = "✅ Colonne `status` ajoutée à users";
} catch (PDOException $e) {
    if (str_contains($e->getMessage(), 'Duplicate column')) {
        $migrations[] = "ℹ️ Colonne `status` existe déjà";
    } else {
        $migrations[] = "❌ Erreur: " . $e->getMessage();
    }
}

echo json_encode(['migrations' => $migrations]);
