<?php
/**
 * API: POST /api/group-buy/notify.php
 * Envoyer une notification à tous les participants d'un achat groupé.
 * Seul le vendeur de la boutique peut notifier.
 * Body: { group_buy_id, title, message }
 */
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'POST only']);

$userId = getAuthUserId();
$input = json_decode(file_get_contents('php://input'), true);
if (!$input) json(400, ['error' => 'Corps invalide']);

$db = getDB();
$groupId = (int)($input['group_buy_id'] ?? 0);
$title = trim($input['title'] ?? 'Notification groupe');
$message = trim($input['message'] ?? '');

if ($groupId <= 0) json(400, ['error' => 'ID achat groupé requis']);

// Vérifier que le vendeur est bien le propriétaire de la boutique
$stmt = $db->prepare("
    SELECT gb.*, s.vendor_id FROM group_buys gb 
    JOIN shops s ON gb.shop_id = s.id 
    WHERE gb.id = ?
");
$stmt->execute([$groupId]);
$gb = $stmt->fetch();
if (!$gb) json(404, ['error' => 'Achat groupé introuvable']);

$isVendor = (int)$gb['vendor_id'] === $userId;
if (!$isVendor) json(403, ['error' => 'Vous n\'êtes pas autorisé à notifier les participants']);

// Récupérer les participants
$stmt = $db->prepare("SELECT user_id FROM group_buy_participants WHERE group_buy_id = ?");
$stmt->execute([$groupId]);
$participants = $stmt->fetchAll(PDO::FETCH_COLUMN);

if (empty($participants)) {
    json(200, ['success' => true, 'message' => 'Aucun participant à notifier']);
}

$count = 0;
foreach ($participants as $pid) {
    sendNotification((int)$pid, $title, $message, 'group_buy', $groupId);
    $count++;
}

json(200, ['success' => true, 'message' => "$count participant(s) notifié(s)"]);
