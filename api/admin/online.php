<?php
/**
 * API: GET /api/admin/online.php
 * Retourne la liste des utilisateurs en ligne (dernières 5 minutes).
 * Réservé aux administrateurs.
 */
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'GET') json(405, ['error' => 'GET only']);

$adminId = getAuthUserId();
$db = getDB();

// Vérifier admin
$stmt = $db->prepare('SELECT role FROM users WHERE id = ?');
$stmt->execute([$adminId]);
$currentUser = $stmt->fetch();
if (!$currentUser || $currentUser['role'] !== 'admin') {
    json(403, ['error' => 'Accès refusé']);
}

try {
    $stmt = $db->query("
        SELECT id, name, email, phone, role, avatar, last_seen
        FROM users
        WHERE last_seen >= DATE_SUB(NOW(), INTERVAL 5 MINUTE)
        ORDER BY last_seen DESC
    ");
    $users = $stmt->fetchAll();
    
    $onlineUsers = [];
    foreach ($users as $u) {
        $secondsAgo = time() - strtotime($u['last_seen']);
        $onlineUsers[] = [
            'id' => (int)$u['id'],
            'name' => $u['name'],
            'email' => $u['email'],
            'phone' => $u['phone'],
            'role' => $u['role'],
            'avatar' => $u['avatar'] ?? '',
            'last_seen' => $u['last_seen'],
            'seconds_ago' => $secondsAgo
        ];
    }

    json(200, [
        'success' => true,
        'online_users' => $onlineUsers,
        'total_online' => count($onlineUsers)
    ]);
} catch (Exception $e) {
    json(500, ['error' => $e->getMessage()]);
}
