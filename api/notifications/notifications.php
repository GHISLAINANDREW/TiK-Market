<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$userId = getAuthUserId();

try {
    $db = getDB();

    if ($method === 'GET') {
        // Vérifier si l'admin veut voir tout l'historique
        $isAdminRequest = isset($_GET['admin']) && (int)$_GET['admin'] === 1;

        if ($isAdminRequest) {
            $stmt = $db->prepare('SELECT role FROM users WHERE id = ?');
            $stmt->execute([$userId]);
            $currentUser = $stmt->fetch();
            if (!$currentUser || $currentUser['role'] !== 'admin') {
                json(403, ['error' => 'Accès refusé']);
            }

            // Pour l'admin, on renvoie les 100 dernières notifications distinctes ou groupées
            $stmt = $db->prepare('
                SELECT n.*, u.name as user_name
                FROM notifications n
                LEFT JOIN users u ON n.user_id = u.id
                ORDER BY n.created_at DESC
                LIMIT 100
            ');
            $stmt->execute();
        } else {
            // Get user notifications + global broadcasts (user_id IS NULL)
            $stmt = $db->prepare('
                SELECT * FROM notifications
                WHERE user_id = ? OR user_id IS NULL
                ORDER BY created_at DESC
                LIMIT 50
            ');
            $stmt->execute([$userId]);
        }
        $notifications = $stmt->fetchAll();

        foreach ($notifications as &$n) {
            $n['id'] = (int)$n['id'];
            $n['user_id'] = $n['user_id'] ? (int)$n['user_id'] : null;
            $n['related_id'] = $n['related_id'] ? (int)$n['related_id'] : null;
            $n['is_read'] = (bool)$n['is_read'];
        }
        unset($n);

        json(200, $notifications);
    }

    if ($method === 'POST') {
        // Admin broadcast ou envoi individuel
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $title = trim($input['title'] ?? '');
        $message = trim($input['message'] ?? '');
        $targetUserId = isset($input['user_id']) ? (int)$input['user_id'] : null;

        if ($title === '' || $message === '') {
            json(400, ['error' => 'Le titre et le message sont obligatoires']);
        }

        // Vérifier que l'utilisateur est admin
        $stmt = $db->prepare('SELECT role FROM users WHERE id = ?');
        $stmt->execute([$userId]);
        $currentUser = $stmt->fetch();
        if (!$currentUser || $currentUser['role'] !== 'admin') {
            json(403, ['error' => 'Seuls les administrateurs peuvent envoyer des notifications']);
        }

        if ($targetUserId !== null) {
            // Envoi individuel à un utilisateur spécifique
            $stmt = $db->prepare('SELECT id, name FROM users WHERE id = ?');
            $stmt->execute([$targetUserId]);
            $targetUser = $stmt->fetch();
            if (!$targetUser) json(404, ['error' => 'Utilisateur introuvable']);

            sendNotification($targetUserId, $title, $message, 'system');

            // Récupérer la notification créée
            $stmt = $db->prepare('SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 1');
            $stmt->execute([$targetUserId]);
            $notif = $stmt->fetch();
            if ($notif) {
                $notif['id'] = (int)$notif['id'];
                $notif['is_read'] = (bool)$notif['is_read'];
            }

            json(200, [
                'success' => true,
                'message' => "Notification envoyée à {$targetUser['name']}",
                'notification' => $notif
            ]);
        } else {
            // Broadcast à tous les utilisateurs
            sendNotification(null, $title, $message, 'system');

            json(200, [
                'success' => true,
                'message' => 'Notification envoyée à tous les utilisateurs'
            ]);
        }
    }

    if ($method === 'PUT') {
        // Mark as read
        $input = json_decode(file_get_contents('php://input'), true);
        $notifId = isset($input['id']) ? (int)$input['id'] : null;

        if ($notifId) {
            $stmt = $db->prepare('UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?');
            $stmt->execute([$notifId, $userId]);
        } else {
            // Mark all as read
            $stmt = $db->prepare('UPDATE notifications SET is_read = 1 WHERE user_id = ?');
            $stmt->execute([$userId]);
        }

        json(200, ['message' => 'Notifications marquées comme lues']);
    }

    if ($method === 'DELETE') {
        $notifId = isset($_GET['id']) ? (int)$_GET['id'] : null;
        if (!$notifId) json(400, ['error' => 'ID requis']);

        // Autoriser l'admin à supprimer n'importe quelle notification
        $stmt = $db->prepare('SELECT role FROM users WHERE id = ?');
        $stmt->execute([$userId]);
        $role = $stmt->fetchColumn();

        if ($role === 'admin') {
            $stmt = $db->prepare('DELETE FROM notifications WHERE id = ?');
            $stmt->execute([$notifId]);
        } else {
            $stmt = $db->prepare('DELETE FROM notifications WHERE id = ? AND user_id = ?');
            $stmt->execute([$notifId, $userId]);
        }

        json(200, ['message' => 'Notification supprimée']);
    }

    json(405, ['error' => 'Méthode non autorisée']);

} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur: ' . $e->getMessage()]);
}
