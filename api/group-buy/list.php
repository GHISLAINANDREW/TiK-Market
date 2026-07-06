<?php
/**
 * API: GET /api/group-buy/list.php?product_id=X
 * Liste les achats groupés ouverts pour un produit.
 * GET /api/group-buy/list.php?my=1 — liste ceux de l'utilisateur connecté
 * GET /api/group-buy/list.php?shop_id=X — liste ceux d'une boutique (pour le vendeur)
 */
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'GET') json(405, ['error' => 'GET only']);

$db = getDB();

// ── Filtrer par produit (Accessible aux visiteurs) ──
if (isset($_GET['product_id'])) {
    $productId = (int)$_GET['product_id'];
    $stmt = $db->prepare("
        SELECT gb.*, p.title as product_title, p.price as original_price, s.name as shop_name,
               (SELECT COUNT(*) FROM group_buy_participants WHERE group_buy_id = gb.id) as participants
        FROM group_buys gb
        JOIN products p ON gb.product_id = p.id
        JOIN shops s ON gb.shop_id = s.id
        WHERE gb.product_id = ? AND gb.status = 'open'
        ORDER BY gb.created_at DESC
    ");
    $stmt->execute([$productId]);
    $list = $stmt->fetchAll();
    foreach ($list as &$gb) {
        $gb['id'] = (int)$gb['id'];
        $gb['min_quantity'] = (int)$gb['min_quantity'];
        $gb['current_qty'] = (int)$gb['current_qty'];
        $gb['participants'] = (int)$gb['participants'];
        $gb['original_price'] = (float)$gb['original_price'];
        $gb['target_price'] = (float)$gb['target_price'];
        $gb['discount_pct'] = (float)$gb['discount_pct'];
    }
    json(200, ['success' => true, 'group_buys' => $list]);
}

// ── Autres filtres (Nécessitent une authentification) ──
$userId = getAuthUserId();

if (isset($_GET['my'])) {
    $stmt = $db->prepare("
        SELECT gb.*, p.title as product_title, p.price as original_price, p.image_url, s.name as shop_name,
               (SELECT COUNT(*) FROM group_buy_participants WHERE group_buy_id = gb.id) as participants
        FROM group_buys gb
        JOIN products p ON gb.product_id = p.id
        JOIN shops s ON gb.shop_id = s.id
        WHERE gb.creator_id = ? OR gb.id IN (SELECT group_buy_id FROM group_buy_participants WHERE user_id = ?)
        ORDER BY gb.created_at DESC
        LIMIT 20
    ");
    $stmt->execute([$userId, $userId]);
    $list = $stmt->fetchAll();
    foreach ($list as &$gb) {
        $gb['id'] = (int)$gb['id'];
        $gb['min_quantity'] = (int)$gb['min_quantity'];
        $gb['current_qty'] = (int)$gb['current_qty'];
        $gb['participants'] = (int)$gb['participants'];
        $gb['original_price'] = (float)$gb['original_price'];
        $gb['target_price'] = (float)$gb['target_price'];
        $gb['discount_pct'] = (float)$gb['discount_pct'];
    }
    json(200, ['success' => true, 'group_buys' => $list]);
}
// ── Filtrer par boutique (vendeur) ──
elseif (isset($_GET['shop_id'])) {
    $shopId = (int)$_GET['shop_id'];
    // Vérifier que le vendeur possède cette boutique
    $stmt = $db->prepare('SELECT id FROM shops WHERE id = ? AND vendor_id = ?');
    $stmt->execute([$shopId, $userId]);
    if (!$stmt->fetch()) json(403, ['error' => 'Cette boutique ne vous appartient pas']);

    $stmt = $db->prepare("
        SELECT gb.*, p.title as product_title, p.price as original_price, u.name as creator_name,
               (SELECT COUNT(*) FROM group_buy_participants WHERE group_buy_id = gb.id) as participants
        FROM group_buys gb
        JOIN products p ON gb.product_id = p.id
        JOIN users u ON gb.creator_id = u.id
        WHERE gb.shop_id = ?
        ORDER BY FIELD(gb.status, 'open', 'filled', 'completed', 'cancelled'), gb.created_at DESC
    ");
    $stmt->execute([$shopId]);
    $list = $stmt->fetchAll();
    foreach ($list as &$gb) {
        $gb['id'] = (int)$gb['id'];
        $gb['min_quantity'] = (int)$gb['min_quantity'];
        $gb['current_qty'] = (int)$gb['current_qty'];
        $gb['participants'] = (int)$gb['participants'];
        $gb['original_price'] = (float)$gb['original_price'];
        $gb['target_price'] = (float)$gb['target_price'];
        $gb['discount_pct'] = (float)$gb['discount_pct'];
    }
    json(200, ['success' => true, 'group_buys' => $list]);
} else {
    json(400, ['error' => 'Paramètre requis: product_id, my, ou shop_id']);
}
