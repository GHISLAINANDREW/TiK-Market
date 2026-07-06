<?php
/**
 * API: GET /api/group-buy/details.php?id=X
 * Détails d'un achat groupé avec la liste des participants.
 */
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'GET') json(405, ['error' => 'GET only']);

$userId = getAuthUserId();
$db = getDB();

$groupId = (int)($_GET['id'] ?? 0);
if ($groupId <= 0) json(400, ['error' => 'ID requis']);

$stmt = $db->prepare("
    SELECT gb.*, p.title as product_title, p.price as original_price, p.image_url, 
           s.name as shop_name, u.name as creator_name
    FROM group_buys gb
    JOIN products p ON gb.product_id = p.id
    JOIN shops s ON gb.shop_id = s.id
    JOIN users u ON gb.creator_id = u.id
    WHERE gb.id = ?
");
$stmt->execute([$groupId]);
$gb = $stmt->fetch();
if (!$gb) json(404, ['error' => 'Achat groupé introuvable']);

$gb['id'] = (int)$gb['id'];
$gb['min_quantity'] = (int)$gb['min_quantity'];
$gb['current_qty'] = (int)$gb['current_qty'];
$gb['original_price'] = (float)$gb['original_price'];
$gb['target_price'] = (float)$gb['target_price'];
$gb['discount_pct'] = (float)$gb['discount_pct'];

// Participants
$stmt = $db->prepare("
    SELECT u.id, u.name, u.avatar, gbp.quantity, gbp.joined_at
    FROM group_buy_participants gbp
    JOIN users u ON gbp.user_id = u.id
    WHERE gbp.group_buy_id = ?
    ORDER BY gbp.joined_at ASC
");
$stmt->execute([$groupId]);
$participants = $stmt->fetchAll();
foreach ($participants as &$p) {
    $p['id'] = (int)$p['id'];
    $p['quantity'] = (int)$p['quantity'];
}

$gb['participants'] = $participants;
$gb['participants_count'] = count($participants);

json(200, ['success' => true, 'group_buy' => $gb]);
