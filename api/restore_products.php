<?php
require_once __DIR__ . '/config/database.php';

try {
    $db = getDB();

    // Reactivate all products that were marked inactive
    $stmt = $db->prepare("UPDATE products SET is_active = 1 WHERE is_active = 0");
    $stmt->execute();
    $count = $stmt->rowCount();

    // Also ensuring stories are active (if they are within 24h)
    // But since the request is general, we just reactivate products.

    echo json_encode([
        "success" => true,
        "message" => "$count produits restaurés avec succès."
    ]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["error" => $e->getMessage()]);
}
