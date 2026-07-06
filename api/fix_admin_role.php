<?php
/**
 * Script de réparation : met à jour le rôle des utilisateurs admin
 * qui ont été créés avec le rôle 'buyer' à cause d'un bug dans register.php.
 * 
 * Usage: php fix_admin_role.php
 *        ou via HTTP: GET http://localhost:8081/fix_admin_role.php
 * 
 * SÉCURITÉ : Ce script ne devrait être exécuté qu'une fois.
 */

require_once __DIR__ . '/config/database.php';

// Liste des emails connus qui devraient être admin
// 🔧 Ajoute ici les emails des administrateurs
$adminEmails = [];

// Si passé en paramètre GET ?emails=admin1@test.com,admin2@test.com
if ($_SERVER['REQUEST_METHOD'] === 'GET' && isset($_GET['emails'])) {
    $adminEmails = array_merge($adminEmails, explode(',', $_GET['emails']));
}

// Si aucun email fourni, on liste les utilisateurs avec leurs rôles pour diagnostic
if (empty($adminEmails)) {
    $db = getDB();
    $stmt = $db->query('SELECT id, name, email, role FROM users ORDER BY id');
    $users = $stmt->fetchAll();
    
    echo "<h2>👥 Utilisateurs existants</h2>";
    echo "<table border='1' cellpadding='8' style='border-collapse:collapse'>";
    echo "<tr><th>ID</th><th>Nom</th><th>Email</th><th>Rôle actuel</th></tr>";
    foreach ($users as $u) {
        $roleClass = $u['role'] === 'admin' ? 'style="color:green;font-weight:bold"' : '';
        echo "<tr><td>{$u['id']}</td><td>{$u['name']}</td><td>{$u['email']}</td><td $roleClass>{$u['role']}</td></tr>";
    }
    echo "</table>";
    
    echo "<h3>🔧 Correction</h3>";
    echo "<p>Pour corriger, appelle : <code>?emails=email1@test.com,email2@test.com</code></p>";
    
    $stmt2 = $db->query('SELECT COUNT(*) as c FROM users WHERE role = "admin"');
    $adminCount = $stmt2->fetch()['c'];
    echo "<p>👑 Admins actuels : <strong>$adminCount</strong></p>";
    
    exit;
}

// Correction des rôles
$db = getDB();
$fixed = 0;
foreach ($adminEmails as $email) {
    $email = trim($email);
    if ($email === '') continue;
    $stmt = $db->prepare('UPDATE users SET role = "admin" WHERE email = ? AND role != "admin"');
    $stmt->execute([$email]);
    if ($stmt->rowCount() > 0) {
        echo "✅ <strong>$email</strong> → rôle promu admin<br>";
        $fixed++;
    } else {
        $stmt2 = $db->prepare('SELECT id, role FROM users WHERE email = ?');
        $stmt2->execute([$email]);
        $u = $stmt2->fetch();
        if ($u) {
            echo "ℹ️ $email est déjà rôle: <strong>{$u['role']}</strong><br>";
        } else {
            echo "❌ $email introuvable<br>";
        }
    }
}

echo "<hr><strong>$fixed utilisateur(s) promu(s) admin.</strong>";
