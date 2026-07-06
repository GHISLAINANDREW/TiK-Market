<?php
/**
 * API: POST /api/group-buy/join.php
 * Rejoindre un achat groupé.
 * Body: { group_buy_id, quantity (optionnel, défaut=1) }
 */
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'POST only']);

$userId = getAuthUserId();
$input = json_decode(file_get_contents('php://input'), true);
if (!$input) json(400, ['error' => 'Corps invalide']);

$db = getDB();
$groupId = (int)($input['group_buy_id'] ?? 0);
$quantity = max(1, (int)($input['quantity'] ?? 1));

if ($groupId <= 0) json(400, ['error' => 'ID achat groupé requis']);

// Récupérer le group buy et le vendeur du produit
$stmt = $db->prepare("
    SELECT gb.*, p.shop_id, s.vendor_id as shop_vendor_id, s.name as shop_name, s.phone as shop_phone
    FROM group_buys gb
    JOIN products p ON gb.product_id = p.id
    JOIN shops s ON p.shop_id = s.id
    WHERE gb.id = ?
");
$stmt->execute([$groupId]);
$gb = $stmt->fetch();
if (!$gb) json(404, ['error' => 'Achat groupé introuvable']);

// ─── NOUVELLES CONTRAINTES ───
// 1. Un vendeur ne peut pas participer à un achat groupé de son propre produit
if ((int)$gb['shop_vendor_id'] === $userId) {
    json(403, ['error' => 'En tant que vendeur de ce produit, vous ne pouvez pas participer à cet achat groupé']);
}

// 2. Un acheteur peut participer une seul fois à un même achat de groupe
$stmt = $db->prepare('SELECT id FROM group_buy_participants WHERE group_buy_id = ? AND user_id = ?');
$stmt->execute([$groupId, $userId]);
if ($stmt->fetch()) {
    json(400, ['error' => 'Vous participez déjà à cet achat groupé']);
}

if ($gb['status'] !== 'open') json(400, ['error' => 'Cet achat groupé n\'est plus ouvert']);
if ((int)$gb['current_qty'] + $quantity > (int)$gb['max_quantity']) json(400, ['error' => 'Quantité maximale dépassée']);

// Ajouter le participant
$stmt = $db->prepare('INSERT INTO group_buy_participants (group_buy_id, user_id, quantity) VALUES (?, ?, ?)');
$stmt->execute([$groupId, $userId, $quantity]);

// Mettre à jour le compteur
$stmt = $db->prepare('UPDATE group_buys SET current_qty = current_qty + ? WHERE id = ?');
$stmt->execute([$quantity, $groupId]);

// Vérifier si le seuil minimum est atteint → statut "filled"
$stmt = $db->prepare('SELECT current_qty, min_quantity FROM group_buys WHERE id = ?');
$stmt->execute([$groupId]);
$updated = $stmt->fetch();
$isFilled = (int)$updated['current_qty'] >= (int)$updated['min_quantity'];

if ($isFilled) {
    $db->prepare("UPDATE group_buys SET status = 'filled' WHERE id = ?")->execute([$groupId]);

    // Notifier le vendeur
    $vendorId = (int)$gb['shop_vendor_id'];
    sendNotification($vendorId, '🎊 Groupe d\'achat complet !', "Le groupe pour votre produit '{$gb['product_id']}' est maintenant complet. Vous pouvez contacter les participants.", 'order', $groupId);

    // Notifier tous les participants pour les mettre en contact avec le vendeur
    $stmt = $db->prepare('SELECT user_id FROM group_buy_participants WHERE group_buy_id = ?');
    $stmt->execute([$groupId]);
    $participantsList = $stmt->fetchAll();

    $msgToBuyers = "Félicitations ! Le groupe est complet. Vous pouvez maintenant contacter le vendeur {$gb['shop_name']} au {$gb['shop_phone']} pour finaliser votre achat au prix réduit.";
    foreach ($participantsList as $p) {
        if ((int)$p['user_id'] !== $vendorId) {
            sendNotification((int)$p['user_id'], '✅ Groupe complet !', $msgToBuyers, 'order', $groupId);
        }
    }
}

// Récupérer le nombre de participants unique
$stmt = $db->prepare('SELECT COUNT(*) as cnt FROM group_buy_participants WHERE group_buy_id = ?');
$stmt->execute([$groupId]);
$participantsCount = (int)$stmt->fetch()['cnt'];

// Notifier le créateur si ce n'est pas le remplissage final (déjà géré par isFilled)
if (!$isFilled) {
    $creatorId = (int)$gb['creator_id'];
    if ($creatorId !== $userId) {
        sendNotification($creatorId, '🎉 Nouveau participant !', "Quelqu'un a rejoint votre achat groupé !", 'system', $groupId);
    }
}

json(200, [
    'success' => true,
    'group_buy_id' => $groupId,
    'current_qty' => (int)$updated['current_qty'],
    'min_quantity' => (int)$updated['min_quantity'],
    'is_filled' => $isFilled,
    'participants' => $participantsCount,
    'message' => $isFilled ? '🎉 Seuil atteint ! Contactez le vendeur pour finaliser.' : 'Vous avez rejoint l\'achat groupé !'
]);

json(200, [
    'success' => true,
    'group_buy_id' => $groupId,
    'current_qty' => (int)$updated['current_qty'],
    'min_quantity' => (int)$updated['min_quantity'],
    'is_filled' => $isFilled,
    'participants' => $participants,
    'message' => $isFilled ? '🎉 Seuil atteint ! Vous pouvez maintenant passer commande au prix réduit.' : 'Vous avez rejoint l\'achat groupé !'
]);
