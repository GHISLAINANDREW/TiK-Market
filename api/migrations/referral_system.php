<?php
require_once __DIR__ . '/../config/database.php';

try {
    $db = getDB();
    echo "Running referral system migration...\n";

    // 1. Add referral_code to users
    try {
        $db->exec("ALTER TABLE users ADD COLUMN referral_code VARCHAR(10) UNIQUE AFTER role");
        $db->exec("ALTER TABLE users ADD COLUMN referred_by INT NULL AFTER referral_code");
        echo "  - Added referral columns to users\n";
    } catch (Exception $e) { echo "  - Referral columns already exist\n"; }

    // 2. Generate referral codes for existing users
    $stmt = $db->query("SELECT id, name FROM users WHERE referral_code IS NULL");
    $users = $stmt->fetchAll();
    $update = $db->prepare("UPDATE users SET referral_code = ? WHERE id = ?");
    foreach ($users as $u) {
        $code = strtoupper(substr(md5($u['name'] . $u['id']), 0, 8));
        $update->execute([$code, $u['id']]);
    }
    echo "  - Generated codes for " . count($users) . " users\n";

    // 3. Add foreign key for referred_by
    try {
        $db->exec("ALTER TABLE users ADD CONSTRAINT fk_referred_by FOREIGN KEY (referred_by) REFERENCES users(id) ON DELETE SET NULL");
    } catch (Exception $e) { }

    echo "Migration complete!\n";
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
