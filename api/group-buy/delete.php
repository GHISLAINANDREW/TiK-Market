<?php
/**
 * API: POST /api/group-buy/delete.php
 * Supprime définitivement un achat groupé (hard delete).
 * Seul le vendeur de la boutique peut supprimer.
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

// Vérifier que l'utilisateur est le vendeur de la boutique de ce group-buy
$stmt = $db->prepare("
    SELECT gb.*, s.vendor_id FROM group_buys gb 
    JOIN shops s ON gb.shop_id = s.id 
    WHERE gb.id = ?
");
$stmt->execute([$groupId]);
$gb = $stmt->fetch();
if (!$gb) json(404, ['error' => 'Achat groupé introuvable']);

// Seul le vendeur peut supprimer définitivement
$isVendor = (int)$gb['vendor_id'] === $userId;
if (!$isVendor) json(403, ['error' => 'Vous n\'êtes pas autorisé à supprimer cet achat groupé']);

// Hard delete
$db->beginTransaction();
try {
    // Supprimer les participants d'abord
    $stmt = $db->prepare("DELETE FROM group_buy_participants WHERE group_buy_id = ?");
    $stmt->execute([$groupId]);
    
    // Supprimer le group buy
    $stmt = $db->prepare("DELETE FROM group_buys WHERE id = ?");
    $stmt->execute([$groupId]);
    
    $db->commit();
    json(200, ['success' => true, 'message' => 'Achat groupé supprimé définitivement']);
} catch (Exception $e) {
    $db->rollBack();
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
