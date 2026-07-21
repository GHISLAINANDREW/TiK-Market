<?php
/**
 * fix_products2.php - Restore products incorrectly deactivated by story cleanup
 * 
 * The old auto-cleanup in products.php deactivated "pure" stories (price <= 0) after 24h.
 * This was wrong — it should only remove the story flag. This script:
 * 1. Re-activates all products that were deactivated by the old story cleanup
 * 2. Removes their story flag so they appear as normal products again
 *
 * Access: ?key=fixwill2026
 */

require_once __DIR__ . '/config/database.php';

if (($_GET['key'] ?? '') !== 'fixwill2026') {
    json(403, ['error' => 'Clé invalide']);
}

try {
    $db = getDB();

    // Re-activate products that were deactivated by story cleanup
    // These are products with is_active=0, is_story=1, and price <= 0 or NULL
    $stmt = $db->prepare("
        SELECT id, shop_id, title, price, created_at 
        FROM products 
        WHERE is_active = 0 AND is_story = 1
    ");
    $stmt->execute();
    $deactivated = $stmt->fetchAll();

    $restored = 0;
    foreach ($deactivated as $p) {
        // Reactivate and remove story flag
        $update = $db->prepare("UPDATE products SET is_active = 1, is_story = 0 WHERE id = ?");
        $update->execute([$p['id']]);
        $restored++;
    }

    // Also remove story flag from any story products that are active but shouldn't expire anymore
    $db->exec("UPDATE products SET is_story = 0 WHERE is_story = 1 AND created_at < NOW() - INTERVAL 1 DAY");

    json(200, [
        'success' => true,
        'restored' => $restored,
        'message' => "$restored produit(s) réactivé(s) et retiré(s) des stories.",
        'details' => $deactivated
    ]);

} catch (Exception $e) {
    json(500, ['error' => $e->getMessage()]);
}
