<?php
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$input = json_decode(file_get_contents('php://input'), true);
if (!$input) json(400, ['error' => 'Corps de requête invalide']);

$name = trim($input['name'] ?? '');
$email = trim($input['email'] ?? '');
$phone = trim($input['phone'] ?? '');
$password = $input['password'] ?? '';
$role = trim($input['role'] ?? 'buyer');
$avatar = trim($input['avatar'] ?? '');
// Accept both French and English role names, store as English
$roleMap = ['buyer' => 'buyer', 'acheteur' => 'buyer', 'vendor' => 'vendor', 'vendeur' => 'vendor', 'admin' => 'admin'];
$role = $roleMap[$role] ?? 'buyer';

if ($name === '' || $email === '' || $phone === '' || $password === '') {
    json(400, ['error' => 'Tous les champs sont obligatoires']);
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    json(400, ['error' => 'Email invalide']);
}

if (strlen($password) < 6) {
    json(400, ['error' => 'Le mot de passe doit contenir au moins 6 caractères']);
}

$allowedRoles = ['buyer', 'vendor', 'admin'];
if (!in_array($role, $allowedRoles)) {
    json(400, ['error' => 'Rôle invalide: ' . $role]);
}

try {
    $db = getDB();

    $stmt = $db->prepare('SELECT id FROM users WHERE email = ?');
    $stmt->execute([$email]);
    if ($stmt->fetch()) {
        json(409, ['error' => 'Cet email est déjà utilisé']);
    }

    $hashedPassword = password_hash($password, PASSWORD_BCRYPT);

    $stmt = $db->prepare('INSERT INTO users (name, email, phone, password, role, avatar) VALUES (?, ?, ?, ?, ?, ?)');
    $stmt->execute([$name, $email, $phone, $hashedPassword, $role, $avatar]);

    $userId = (int)$db->lastInsertId();
    $token = generateToken($userId, $email);

    // Notifier les admins pour une nouvelle inscription vendeur
    if ($role === 'vendor') {
        try {
            $stmtAdmin = $db->prepare("SELECT id FROM users WHERE role = 'admin'");
            $stmtAdmin->execute();
            $admins = $stmtAdmin->fetchAll();
            foreach ($admins as $admin) {
                sendNotification((int)$admin['id'], "Nouveau vendeur inscrit", "$name ($email) s'est inscrit en tant que vendeur.", 'system', $userId);
            }
        } catch (Exception $e) {}
    }

    json(201, [
        'token' => $token,
        'user' => [
            'id' => $userId,
            'name' => $name,
            'email' => $email,
            'phone' => $phone,
            'role' => $role,
            'avatar' => $avatar,
            'location' => '',
        ]
    ]);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur BD : ' . $e->getMessage()]);
} catch (\Throwable $e) {
    json(500, ['error' => $e->getMessage()]);
}
