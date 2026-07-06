<?php
/**
 * API: /api/promotions/promotions.php
 * Manage promo codes.
 * GET ?code=XXX&amount=YYY   → validate a promo code (returns discount)
 * GET ?shop_id=X             → list promos for a shop (vendor)
 * POST (shop_id, code, discount_pct, max_uses, expires_at) → create (vendor)
 * DELETE ?id=X              → delete a promotion (vendor)
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
if ($method === 'OPTIONS') { json(200, []); }

try {
    $db = getDB();

    // Auto-create table
    $db->exec("CREATE TABLE IF NOT EXISTS promotions (
        id INT AUTO_INCREMENT PRIMARY KEY,
        shop_id INT NOT NULL,
        code VARCHAR(50) NOT NULL,
        discount_pct DECIMAL(5,2) NOT NULL DEFAULT 0,
        discount_fixed INT NOT NULL DEFAULT 0,
        min_amount INT NOT NULL DEFAULT 0,
        max_uses INT NOT NULL DEFAULT 0,
        used_count INT NOT NULL DEFAULT 0,
        is_active TINYINT(1) NOT NULL DEFAULT 1,
        expires_at DATE DEFAULT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY unique_code (shop_id, code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    // ── Validate a code ──
    if ($method === 'GET' && isset($_GET['code'])) {
        $code = trim($_GET['code']);
        $amount = isset($_GET['amount']) ? (float)$_GET['amount'] : 0;

        $stmt = $db->prepare("SELECT * FROM promotions WHERE code = ? AND is_active = 1 AND (max_uses = 0 OR used_count < max_uses)");
        $stmt->execute([$code]);
        $promo = $stmt->fetch();

        if (!$promo) json(404, ['valid' => false, 'error' => 'Code invalide ou expiré']);

        // Check expiration
        if ($promo['expires_at'] && strtotime($promo['expires_at']) < time()) {
            json(404, ['valid' => false, 'error' => 'Code expiré']);
        }

        // Calculate discount
        $discount = 0;
        if ($promo['discount_pct'] > 0) {
            $discount = (int)($amount * $promo['discount_pct'] / 100);
        } else if ($promo['discount_fixed'] > 0) {
            $discount = (int)$promo['discount_fixed'];
        }
        if ($amount > 0 && $discount > $amount) $discount = (int)$amount;

        $promo['id'] = (int)$promo['id'];
        $promo['shop_id'] = (int)$promo['shop_id'];
        $promo['discount_pct'] = (float)$promo['discount_pct'];
        $promo['discount_fixed'] = (int)$promo['discount_fixed'];
        $promo['min_amount'] = (int)$promo['min_amount'];
        $promo['max_uses'] = (int)$promo['max_uses'];
        $promo['used_count'] = (int)$promo['used_count'];
        $promo['is_active'] = (bool)$promo['is_active'];

        json(200, ['valid' => true, 'promotion' => $promo, 'discount' => $discount]);
    }

    // ── List promos for a shop (vendor) ──
    if ($method === 'GET' && isset($_GET['shop_id'])) {
        $shopId = (int)$_GET['shop_id'];
        $userId = getAuthUserId();

        // Verify ownership
        $stmt = $db->prepare("SELECT id FROM shops WHERE id = ? AND vendor_id = ?");
        $stmt->execute([$shopId, $userId]);
        if (!$stmt->fetch()) json(403, ['error' => 'Non autorisé']);

        $stmt = $db->prepare("SELECT * FROM promotions WHERE shop_id = ? ORDER BY created_at DESC");
        $stmt->execute([$shopId]);
        $promos = $stmt->fetchAll();

        foreach ($promos as &$p) {
            $p['id'] = (int)$p['id'];
            $p['shop_id'] = (int)$p['shop_id'];
            $p['discount_pct'] = (float)$p['discount_pct'];
            $p['discount_fixed'] = (int)$p['discount_fixed'];
            $p['min_amount'] = (int)$p['min_amount'];
            $p['max_uses'] = (int)$p['max_uses'];
            $p['used_count'] = (int)$p['used_count'];
            $p['is_active'] = (bool)$p['is_active'];
        }

        json(200, ['promotions' => $promos]);
    }

    // ── Create a promo code (vendor) ──
    if ($method === 'POST') {
        $userId = getAuthUserId();
        $input = json_decode(file_get_contents('php://input'), true);

        $shopId = (int)($input['shop_id'] ?? 0);
        $code = strtoupper(trim($input['code'] ?? ''));
        $discountPct = (float)($input['discount_pct'] ?? 0);
        $discountFixed = (int)($input['discount_fixed'] ?? 0);
        $minAmount = (int)($input['min_amount'] ?? 0);
        $maxUses = (int)($input['max_uses'] ?? 0);
        $expiresAt = $input['expires_at'] ?? null;

        if (!$shopId || !$code) json(400, ['error' => 'shop_id et code requis']);
        if ($discountPct <= 0 && $discountFixed <= 0) json(400, ['error' => 'Discount requis']);

        // Verify ownership
        $stmt = $db->prepare("SELECT id FROM shops WHERE id = ? AND vendor_id = ?");
        $stmt->execute([$shopId, $userId]);
        if (!$stmt->fetch()) json(403, ['error' => 'Non autorisé']);

        $stmt = $db->prepare("INSERT INTO promotions (shop_id, code, discount_pct, discount_fixed, min_amount, max_uses, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([$shopId, $code, $discountPct, $discountFixed, $minAmount, $maxUses, $expiresAt]);

        json(201, ['message' => 'Code promo créé', 'code' => $code]);
    }

    // ── Delete a promo code (vendor) ──
    if ($method === 'DELETE') {
        $userId = getAuthUserId();
        $id = (int)($_GET['id'] ?? 0);
        if (!$id) json(400, ['error' => 'id requis']);

        // Verify ownership
        $stmt = $db->prepare("SELECT p.id FROM promotions p JOIN shops s ON p.shop_id = s.id WHERE p.id = ? AND s.vendor_id = ?");
        $stmt->execute([$id, $userId]);
        if (!$stmt->fetch()) json(403, ['error' => 'Non autorisé']);

        $db->prepare("DELETE FROM promotions WHERE id = ?")->execute([$id]);
        json(200, ['message' => 'Code promo supprimé']);
    }

    json(405, ['error' => 'Method not allowed']);
} catch (Exception $e) {
    json(500, ['error' => $e->getMessage()]);
}
