<?php
/**
 * Live streams list API endpoint.
 *
 * GET /live/list.php → list currently live streams (with shop info)
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // ── Auto-migration: create live_streams table ──
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS live_streams (
            id INT AUTO_INCREMENT PRIMARY KEY,
            shop_id INT NOT NULL,
            title VARCHAR(200) NOT NULL,
            stream_url VARCHAR(500) DEFAULT '',
            viewer_count INT DEFAULT 0,
            is_live TINYINT(1) DEFAULT 1,
            pinned_product_id INT DEFAULT NULL,
            started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            ended_at TIMESTAMP NULL DEFAULT NULL,
            FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
            INDEX idx_live_active (is_live, started_at)
        )");
    } catch (Exception $e) { error_log("Migration live_streams table: " . $e->getMessage()); }

    if ($method === 'GET') {
        $shopId = isset($_GET['shop_id']) ? (int)$_GET['shop_id'] : 0;

        if ($shopId > 0) {
            $stmt = $db->prepare("
                SELECT ls.*, s.name AS shop_name, s.logo AS shop_logo
                FROM live_streams ls
                JOIN shops s ON ls.shop_id = s.id
                WHERE ls.shop_id = ? AND ls.is_live = 1
                ORDER BY ls.started_at DESC
            ");
            $stmt->execute([$shopId]);
            $streams = $stmt->fetchAll();
        } else {
            $stmt = $db->query("
                SELECT ls.*, s.name AS shop_name, s.logo AS shop_logo
                FROM live_streams ls
                JOIN shops s ON ls.shop_id = s.id
                WHERE ls.is_live = 1
                ORDER BY ls.started_at DESC
            ");
            $streams = $stmt->fetchAll();
        }

        foreach ($streams as &$st) {
            $st['id'] = (int)$st['id'];
            $st['shop_id'] = (int)$st['shop_id'];
            $st['viewer_count'] = (int)$st['viewer_count'];
            $st['is_live'] = (bool)$st['is_live'];
            $st['pinned_product_id'] = $st['pinned_product_id'] ? (int)$st['pinned_product_id'] : null;
        }
        unset($st);

        json(200, ['streams' => $streams]);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
