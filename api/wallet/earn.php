<?php
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$data = json_decode(file_get_contents('php://input'), true);
if (!$data) json(400, ['error' => 'Corps de requête invalide']);

$amount = (int)($data['amount'] ?? 0);
$orderId = (int)($data['order_id'] ?? 0);

if ($amount <= 0) json(400, ['error' => 'Montant invalide']);
if ($orderId <= 0) json(400, ['error' => 'ID commande requis']);

try {
    $db = getDB();
    $userId = getAuthUserId();

    $stmt = $db->prepare('
        SELECT w.*, lt.cashback_pct, lt.bonus_pct
        FROM wallets w
        JOIN loyalty_tiers lt ON w.tier = lt.name
        WHERE w.user_id = ?
    ');
    $stmt->execute([$userId]);
    $wallet = $stmt->fetch();

    if (!$wallet) {
        $stmt = $db->prepare('INSERT INTO wallets (user_id) VALUES (?)');
        $stmt->execute([$userId]);
        $stmt = $db->prepare('
            SELECT w.*, lt.cashback_pct, lt.bonus_pct
            FROM wallets w
            JOIN loyalty_tiers lt ON w.tier = lt.name
            WHERE w.user_id = ?
        ');
        $stmt->execute([$userId]);
        $wallet = $stmt->fetch();
    }

    $cashbackPct = (float)$wallet['cashback_pct'];
    $bonusPct = (float)$wallet['bonus_pct'];

    $cashbackAmount = (int)round($amount * $cashbackPct / 100);
    $basePoints = (int)floor($amount / 10);
    $bonusPoints = (int)floor($basePoints * $bonusPct / 100);
    $totalPoints = $basePoints + $bonusPoints;

    $db->beginTransaction();

    $stmt = $db->prepare('
        UPDATE wallets
        SET balance = balance + ?,
            total_points = total_points + ?,
            current_points = current_points + ?,
            lifetime_spent = lifetime_spent + ?
        WHERE id = ?
    ');
    $stmt->execute([$cashbackAmount, $totalPoints, $totalPoints, $amount, $wallet['id']]);

    $newTotalPoints = (int)$wallet['total_points'] + $totalPoints;
    $tiers = $db->query('SELECT name, min_points FROM loyalty_tiers ORDER BY min_points DESC')->fetchAll();
    $newTier = 'bronze';
    foreach ($tiers as $t) {
        if ($newTotalPoints >= (int)$t['min_points']) {
            $newTier = $t['name'];
            break;
        }
    }

    if ($newTier !== $wallet['tier']) {
        $stmt = $db->prepare('UPDATE wallets SET tier = ? WHERE id = ?');
        $stmt->execute([$newTier, $wallet['id']]);
    }

    $stmt = $db->prepare('
        INSERT INTO wallet_transactions (wallet_id, type, amount_fcfa, points, description, reference_type, reference_id)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    ');
    $stmt->execute([
        $wallet['id'], 'earn', $cashbackAmount, 0,
        "Cashback {$cashbackPct}% sur commande #{$orderId}", 'order', $orderId
    ]);
    if ($totalPoints > 0) {
        $stmt->execute([
            $wallet['id'], 'bonus', 0, $totalPoints,
            "Points fidélité sur commande #{$orderId}", 'order', $orderId
        ]);
    }

    $db->commit();

    $stmt = $db->prepare('SELECT balance, current_points FROM wallets WHERE id = ?');
    $stmt->execute([$wallet['id']]);
    $updated = $stmt->fetch();

    json(200, [
        'success' => true,
        'earned_cashback' => $cashbackAmount,
        'earned_points' => $totalPoints,
        'new_balance' => (int)$updated['balance'],
        'new_points' => (int)$updated['current_points'],
        'new_tier' => $newTier
    ]);
} catch (PDOException $e) {
    if ($db->inTransaction()) $db->rollBack();
    json(500, ['error' => 'Erreur serveur']);
}
