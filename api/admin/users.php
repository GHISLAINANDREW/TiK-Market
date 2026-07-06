<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$userId = getAuthUserId();

// Verify the user is admin
$db = getDB();
$stmt = $db->prepare('SELECT role FROM users WHERE id = ?');
$stmt->execute([$userId]);
$currentUser = $stmt->fetch();
if (!$currentUser || $currentUser['role'] !== 'admin') {
    json(403, ['error' => 'Accès refusé. Seuls les administrateurs peuvent accéder à cette ressource.']);
}

if ($method === 'GET') {
    // ── List all users ──
    $stmt = $db->query('SELECT id, name, email, phone, role, status, last_seen, created_at FROM users ORDER BY created_at DESC');
    $users = $stmt->fetchAll();
    foreach ($users as &$u) {
        $u['status'] = $u['status'] ?? 'active';
    }
    unset($u);
    json(200, ['users' => $users]);

} elseif ($method === 'POST') {
    // ── Ajouter un utilisateur (admin uniquement) ──
    $input = json_decode(file_get_contents('php://input'), true);
    if (!$input) json(400, ['error' => 'Corps de requête invalide']);

    $name = trim($input['name'] ?? '');
    $email = trim($input['email'] ?? '');
    $phone = trim($input['phone'] ?? '');
    $password = $input['password'] ?? '';
    $role = trim($input['role'] ?? 'buyer');
    $roleMap = ['buyer' => 'buyer', 'vendor' => 'vendor', 'admin' => 'admin'];
    $role = $roleMap[$role] ?? 'buyer';

    if ($name === '' || $email === '' || $phone === '' || $password === '') {
        json(400, ['error' => 'Tous les champs sont obligatoires']);
    }
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        json(400, ['error' => 'Email invalide']);
    }

    // Vérifier si l'email existe déjà
    $stmt = $db->prepare('SELECT id FROM users WHERE email = ?');
    $stmt->execute([$email]);
    if ($stmt->fetch()) {
        json(409, ['error' => 'Cet email est déjà utilisé']);
    }

    $hashedPassword = password_hash($password, PASSWORD_BCRYPT);
    $stmt = $db->prepare('INSERT INTO users (name, email, phone, password, role) VALUES (?, ?, ?, ?, ?)');
    $stmt->execute([$name, $email, $phone, $hashedPassword, $role]);
    $newId = (int)$db->lastInsertId();

    json(200, ['success' => true, 'message' => 'Utilisateur créé', 'user' => [
        'id' => $newId, 'name' => $name, 'email' => $email, 'phone' => $phone, 'role' => $role
    ]]);

} elseif ($method === 'PUT') {
    $targetId = (int)($_GET['id'] ?? 0);
    if ($targetId <= 0) json(400, ['error' => 'ID utilisateur requis']);
    if ($targetId === $userId && isset($_GET['status'])) json(403, ['error' => 'Vous ne pouvez pas bannir votre propre compte']);

    // ── Ban / Unban user ──
    if (isset($_GET['status'])) {
        $newStatus = $_GET['status'];
        if (!in_array($newStatus, ['active', 'banned', 'suspended'])) {
            json(400, ['error' => 'Statut invalide. Utilisez active, banned ou suspended']);
        }
        $stmt = $db->prepare('UPDATE users SET status = ? WHERE id = ?');
        $stmt->execute([$newStatus, $targetId]);
        json(200, ['success' => true, 'message' => $newStatus === 'banned' ? 'Utilisateur banni' : ($newStatus === 'suspended' ? 'Utilisateur suspendu' : 'Utilisateur réactivé')]);
    }

    // ── Update user role ──
    $role = $_GET['role'] ?? '';
    if (!in_array($role, ['buyer', 'vendor', 'admin'])) json(400, ['error' => 'Rôle invalide']);
    
    // Don't allow changing own role
    if ($targetId === $userId) json(403, ['error' => 'Vous ne pouvez pas modifier votre propre rôle']);
    
    $stmt = $db->prepare('UPDATE users SET role = ? WHERE id = ?');
    $stmt->execute([$role, $targetId]);
    json(200, ['success' => true, 'message' => 'Rôle mis à jour']);

} elseif ($method === 'DELETE') {
    // ── Supprimer un utilisateur ──
    $targetId = (int)($_GET['id'] ?? 0);
    
    if ($targetId <= 0) json(400, ['error' => 'ID utilisateur requis']);
    if ($targetId === $userId) json(403, ['error' => 'Vous ne pouvez pas supprimer votre propre compte']);
    
    // Supprimer les dépendances (shops, notifications, etc.)
    try { $db->prepare('DELETE FROM notifications WHERE user_id = ?')->execute([$targetId]); } catch (Exception $e) {}
    try { $db->prepare('DELETE FROM reviews WHERE user_id = ?')->execute([$targetId]); } catch (Exception $e) {}
    try { $db->prepare('DELETE FROM cart WHERE user_id = ?')->execute([$targetId]); } catch (Exception $e) {}
    try { $db->prepare('DELETE FROM wishlist WHERE user_id = ?')->execute([$targetId]); } catch (Exception $e) {}
    try { 
        $stmt = $db->prepare('SELECT id FROM shops WHERE vendor_id = ?');
        $stmt->execute([$targetId]);
        $shopIds = $stmt->fetchAll(PDO::FETCH_COLUMN);
        foreach ($shopIds as $sid) {
            $db->prepare('DELETE FROM products WHERE shop_id = ?')->execute([$sid]);
        }
        $db->prepare('DELETE FROM shops WHERE vendor_id = ?')->execute([$targetId]);
    } catch (Exception $e) {}
    
    $stmt = $db->prepare('DELETE FROM users WHERE id = ?');
    $stmt->execute([$targetId]);
    
    if ($stmt->rowCount() > 0) {
        json(200, ['success' => true, 'message' => 'Utilisateur supprimé']);
    } else {
        json(404, ['error' => 'Utilisateur introuvable']);
    }

} else {
    json(405, ['error' => 'Méthode non autorisée']);
}
