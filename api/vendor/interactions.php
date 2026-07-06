<?php
/**
 * Endpoints vendeur pour les interactions clients:
 * - GET /vendor/interactions.php?product_id=X → likes, reviews, subscribers
 * - GET /vendor/interactions.php?shop_id=X → shop subscribers
 */
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$userId = getAuthUserId();

if ($method !== 'GET') json(405, ['error' => 'Méthode non autorisée']);

$db = getDB();

// Vérifier que l'utilisateur est un vendeur
$stmt = $db->prepare('SELECT role FROM users WHERE id = ?');
$stmt->execute([$userId]);
$currentUser = $stmt->fetch();
if (!$currentUser || !in_array($currentUser['role'], ['vendor', 'admin'])) {
    json(403, ['error' => 'Accès réservé aux vendeurs']);
}

$productId = isset($_GET['product_id']) ? (int)$_GET['product_id'] : 0;
$shopId = isset($_GET['shop_id']) ? (int)$_GET['shop_id'] : 0;

$response = [];

// ─── Produit : likes (wishlist) ───
if ($productId > 0) {
    $stmt = $db->prepare('
        SELECT u.id, u.name, u.email, u.avatar, w.created_at as liked_at
        FROM wishlist w
        JOIN users u ON w.user_id = u.id
        WHERE w.product_id = ?
        ORDER BY w.created_at DESC
    ');
    $stmt->execute([$productId]);
    $likes = $stmt->fetchAll();
    foreach ($likes as &$l) {
        $l['id'] = (int)$l['id'];
    }
    unset($l);
    $response['likes'] = $likes;

    // ─── Produit : avis ───
    $stmt = $db->prepare('
        SELECT r.id, r.user_id, u.name as user_name, u.avatar as user_avatar, r.rating, r.comment, r.created_at
        FROM reviews r
        JOIN users u ON r.user_id = u.id
        WHERE r.product_id = ?
        ORDER BY r.created_at DESC
    ');
    $stmt->execute([$productId]);
    $reviews = $stmt->fetchAll();
    foreach ($reviews as &$r) {
        $r['id'] = (int)$r['id'];
        $r['user_id'] = (int)$r['user_id'];
        $r['rating'] = (int)$r['rating'];
    }
    unset($r);
    $response['reviews'] = $reviews;
}

// ─── Shop : abonnés (shop_favorites) ───
if ($shopId > 0 || $productId > 0) {
    // Si on a un product_id, trouver le shop_id correspondant
    if ($shopId <= 0 && $productId > 0) {
        $stmt = $db->prepare('SELECT shop_id FROM products WHERE id = ?');
        $stmt->execute([$productId]);
        $row = $stmt->fetch();
        $shopId = $row ? (int)$row['shop_id'] : 0;
    }

    if ($shopId > 0) {
        $stmt = $db->prepare('
            SELECT u.id, u.name, u.email, u.avatar, f.created_at as subscribed_at
            FROM shop_favorites f
            JOIN users u ON f.user_id = u.id
            WHERE f.shop_id = ?
            ORDER BY f.created_at DESC
        ');
        $stmt->execute([$shopId]);
        $subscribers = $stmt->fetchAll();
        foreach ($subscribers as &$s) {
            $s['id'] = (int)$s['id'];
        }
        unset($s);
        $response['subscribers'] = $subscribers;
    }
}

json(200, $response);
