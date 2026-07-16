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
$referralCode = trim($input['referral_code'] ?? '');

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

    // Referral logic
    $referrerId = null;
    if (!empty($referralCode)) {
        $stmtRef = $db->prepare("SELECT id FROM users WHERE referral_code = ?");
        $stmtRef->execute([$referralCode]);
        $referrer = $stmtRef->fetch();
        if ($referrer) {
            $referrerId = (int)$referrer['id'];
        }
    }

    // Generate unique referral code for the new user
    $myReferralCode = strtoupper(substr(md5($email . time()), 0, 8));

    $stmt = $db->prepare('INSERT INTO users (name, email, phone, password, role, avatar, referral_code, referred_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)');
    $stmt->execute([$name, $email, $phone, $hashedPassword, $role, $avatar, $myReferralCode, $referrerId]);

    $userId = (int)$db->lastInsertId();

    // Reward referrer if exists
    if ($referrerId) {
        try {
            $rewardPoints = 500; // 500 pts for referral
            $stmtWallet = $db->prepare("UPDATE wallets SET current_points = current_points + ?, total_points = total_points + ? WHERE user_id = ?");
            $stmtWallet->execute([$rewardPoints, $rewardPoints, $referrerId]);

            // Log transaction
            $stmtLog = $db->prepare("INSERT INTO wallet_transactions (wallet_id, type, amount_fcfa, points, description, reference_type, reference_id)
                                     SELECT id, 'bonus', 0, ?, ?, 'user', ? FROM wallets WHERE user_id = ?");
            $stmtLog->execute([$rewardPoints, "Parrainage de $name", $userId, $referrerId]);

            sendNotification($referrerId, "Bonus Parrainage !", "Vous avez gagné $rewardPoints points car $name a rejoint DschangMarket via votre lien !", "promo", $userId);
        } catch (Exception $e) {}
    }

    $token = generateToken($userId, $email);

    // Notifier les admins pour toute nouvelle inscription
    try {
        $stmtAdmin = $db->prepare("SELECT id FROM users WHERE role = 'admin'");
        $stmtAdmin->execute();
        $admins = $stmtAdmin->fetchAll();
        $title = $role === 'vendor' ? 'Nouveau vendeur inscrit' : 'Nouvel acheteur inscrit';
        $message = "$name ($email) s'est inscrit" . ($role === 'vendor' ? ' en tant que vendeur.' : '.');
        foreach ($admins as $admin) {
            sendNotification((int)$admin['id'], $title, $message, 'system', $userId);
        }
    } catch (Exception $e) {}

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
