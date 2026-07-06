<?php
/**
 * API: POST /api/group-buy/cancel.php
 * Annuler un achat groupé.
 * Seul le créateur ou le vendeur de la boutique peut annuler.
 * Body: { group_buy_id }
 */
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'POST only']);

$userId = getAuthUserId();
$input = json_decode(file_get_contents('php://input'), true);
if (!$input) json(400, ['error' => 'Corps invalide']);

$db = getDB();
$groupId = (int)($input['group_buy_id'] ?? 0);
if ($groupId <= 0) json(400, ['error' => 'ID achat groupé requis']);

$stmt = $db->prepare("
    SELECT gb.*, s.vendor_id FROM group_buys gb 
    JOIN shops s ON gb.shop_id = s.id 
    WHERE gb.id = ?
");
$stmt->execute([$groupId]);
$gb = $stmt->fetch();
if (!$gb) json(404, ['error' => 'Achat groupé introuvable']);

// Seul le créateur ou le vendeur peuvent annuler
$isCreator = (int)$gb['creator_id'] === $userId;
$isVendor = (int)$gb['vendor_id'] === $userId;
if (!$isCreator && !$isVendor) json(403, ['error' => 'Vous n\'êtes pas autorisé à annuler cet achat groupé']);

$stmt = $db->prepare("UPDATE group_buys SET status = 'cancelled' WHERE id = ?");
$stmt->execute([$groupId]);

// Notifier les participants
$stmt = $db->prepare("SELECT user_id FROM group_buy_participants WHERE group_buy_id = ? AND user_id != ?");
$stmt->execute([$groupId, $userId]);
foreach ($stmt->fetchAll() as $p) {
    sendNotification((int)$p['user_id'], '❌ Achat groupé annulé', "L'achat groupé auquel vous avez participé a été annulé.", 'system', $groupId);
}

json(200, ['success' => true, 'message' => 'Achat groupé annulé']);
