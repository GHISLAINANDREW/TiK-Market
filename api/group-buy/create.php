<?php
/**
 * API: POST /api/group-buy/create.php
 * Crée un achat groupé sur un produit.
 * Body: { product_id, min_quantity, discount_pct (optionnel), expires_in_hours (optionnel) }
 */
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'POST only']);

$userId = getAuthUserId();
$input = json_decode(file_get_contents('php://input'), true);
if (!$input) json(400, ['error' => 'Corps invalide']);

$db = getDB();

$productId = (int)($input['product_id'] ?? 0);
$minQty = max(2, (int)($input['min_quantity'] ?? 5));
$discountPct = min(50, max(0, (float)($input['discount_pct'] ?? 5)));
$expiresHours = min(168, max(1, (int)($input['expires_in_hours'] ?? 48)));

if ($productId <= 0) json(400, ['error' => 'ID produit requis']);

try {
    // Vérifier que le produit existe et récupérer la boutique
    $stmt = $db->prepare('SELECT p.id, p.title, p.price, p.shop_id, s.name as shop_name FROM products p JOIN shops s ON p.shop_id = s.id WHERE p.id = ? AND p.is_active = 1');
    $stmt->execute([$productId]);
    $product = $stmt->fetch();
    if (!$product) json(404, ['error' => 'Produit introuvable']);

    // Calcul du prix cible
    $targetPrice = round($product['price'] * (1 - $discountPct / 100), 0);

    // Vérifier si l'utilisateur a déjà un group-buy ouvert sur ce produit
    $stmt = $db->prepare("SELECT id FROM group_buys WHERE product_id = ? AND creator_id = ? AND status = 'open'");
    $stmt->execute([$productId, $userId]);
    if ($stmt->fetch()) json(409, ['error' => 'Vous avez déjà un achat groupé ouvert sur ce produit']);

    // Calculer la date d'expiration en PHP (plus fiable pour SQL)
    $expiresAt = date('Y-m-d H:i:s', strtotime("+$expiresHours hours"));

    // Créer le group buy
    $stmt = $db->prepare("INSERT INTO group_buys (product_id, shop_id, creator_id, min_quantity, discount_pct, target_price, status, expires_at) VALUES (?, ?, ?, ?, ?, ?, 'open', ?)");
    $stmt->execute([
        $productId,
        (int)$product['shop_id'],
        $userId,
        $minQty,
        (float)$discountPct,
        (float)$targetPrice,
        $expiresAt
    ]);

    $groupId = (int)$db->lastInsertId();

    // ─── NOUVELLE LOGIQUE CRÉATEUR ───
    // Si le créateur est le vendeur du produit, on ne l'ajoute pas comme participant
    // Sinon, on l'ajoute comme premier participant
    $isVendor = (int)$product['vendor_id'] === $userId;

    if (!$isVendor) {
        $stmt = $db->prepare('INSERT INTO group_buy_participants (group_buy_id, user_id, quantity) VALUES (?, ?, 1)');
        $stmt->execute([$groupId, $userId]);
    } else {
        // Pour un vendeur, la quantité de départ est 0 (il attend les acheteurs)
        $db->prepare('UPDATE group_buys SET current_qty = 0 WHERE id = ?')->execute([$groupId]);
    }

    // Récupérer le nom du créateur
    $stmt = $db->prepare('SELECT name FROM users WHERE id = ?');
    $stmt->execute([$userId]);
    $creator = $stmt->fetch();
    $creatorName = $creator ? $creator['name'] : '';
} catch (Exception $e) {
    json(500, ['error' => 'Erreur SQL : ' . $e->getMessage()]);
}

json(201, [
    'success' => true,
    'group_buy' => [
        'id' => $groupId,
        'product_id' => $productId,
        'shop_id' => (int)$product['shop_id'],
        'creator_id' => $userId,
        'creator_name' => $creatorName,
        'product_title' => $product['title'],
        'shop_name' => $product['shop_name'],
        'original_price' => (float)$product['price'],
        'target_price' => (float)$targetPrice,
        'discount_pct' => (float)$discountPct,
        'min_quantity' => $minQty,
        'current_qty' => 1,
        'participants_count' => 1,
        'status' => 'open',
        'created_at' => date('Y-m-d H:i:s')
    ]
]);
