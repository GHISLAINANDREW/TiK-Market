<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // Auto-migration (for development)
    try {
        $db->exec("ALTER TABLE messages ADD COLUMN audio_url TEXT NULL AFTER text");
        $db->exec("ALTER TABLE messages ADD COLUMN duration INT DEFAULT 0 AFTER audio_url");
        $db->exec("ALTER TABLE messages ADD COLUMN product_image_url TEXT NULL AFTER product_id");
    } catch (Exception $e) { /* Already exists */ }

    $userId = getAuthUserId();

    if ($method === 'GET') {
        $conversationWith = isset($_GET['conversation_with']) ? (int)$_GET['conversation_with'] : 0;

        if ($conversationWith > 0) {
            $stmt = $db->prepare('
                SELECT m.*, u.name AS sender_name
                FROM messages m
                JOIN users u ON m.sender_id = u.id
                WHERE (m.sender_id = ? AND m.receiver_id = ?)
                   OR (m.sender_id = ? AND m.receiver_id = ?)
                ORDER BY m.created_at ASC
            ');
            $stmt->execute([$userId, $conversationWith, $conversationWith, $userId]);
            $messages = $stmt->fetchAll();

            foreach ($messages as &$m) {
                $m['id'] = (int)$m['id'];
                $m['sender_id'] = (int)$m['sender_id'];
                $m['receiver_id'] = (int)$m['receiver_id'];
                if ($m['product_id']) $m['product_id'] = (int)$m['product_id'];
                $m['is_read'] = (bool)$m['is_read'];
                $m['duration'] = (int)($m['duration'] ?? 0);
                $m['product_image_url'] = $m['product_image_url'] ?? null;
            }
            unset($m);

            json(200, ['messages' => $messages]);
        }

        $stmt = $db->prepare('
            SELECT
                u.id AS user_id,
                u.name AS user_name,
                u.avatar,
                u.last_seen,
                CASE WHEN u.last_seen >= DATE_SUB(NOW(), INTERVAL 5 MINUTE) THEN 1 ELSE 0 END AS is_online,
                m.text AS last_message,
                m.created_at AS last_message_at,
                m.sender_id AS last_sender_id,
                COALESCE(unread.unread_count, 0) AS unread_count
            FROM (
                SELECT
                    CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END AS interlocutor_id,
                    MAX(id) AS max_message_id
                FROM messages
                WHERE sender_id = ? OR receiver_id = ?
                GROUP BY interlocutor_id
            ) AS latest
            JOIN messages m ON m.id = latest.max_message_id
            JOIN users u ON u.id = latest.interlocutor_id
            LEFT JOIN (
                SELECT sender_id AS interlocutor_id, COUNT(*) AS unread_count
                FROM messages
                WHERE receiver_id = ? AND is_read = 0
                GROUP BY sender_id
            ) AS unread ON unread.interlocutor_id = latest.interlocutor_id
            ORDER BY m.created_at DESC
        ');
        $stmt->execute([$userId, $userId, $userId, $userId]);
        $conversations = $stmt->fetchAll();

        foreach ($conversations as &$c) {
            $c['user_id'] = (int)$c['user_id'];
            $c['last_sender_id'] = (int)$c['last_sender_id'];
            $c['unread_count'] = (int)$c['unread_count'];
            $c['is_online'] = (bool)(int)($c['is_online'] ?? 0);
        }
        unset($c);

        json(200, ['conversations' => $conversations]);
    }

    if ($method === 'PUT') {
        $readContactId = isset($_GET['read_contact_id']) ? (int)$_GET['read_contact_id'] : 0;
        if ($readContactId > 0) {
            $stmt = $db->prepare('UPDATE messages SET is_read = 1 WHERE receiver_id = ? AND sender_id = ?');
            $stmt->execute([$userId, $readContactId]);
            json(200, ['success' => true]);
        }
        json(400, ['error' => 'Paramètre manquant']);
    }

    if ($method === 'POST') {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $receiver_id = (int)($input['receiver_id'] ?? 0);
        $product_id = isset($input['product_id']) ? (int)$input['product_id'] : null;
        $product_image_url = $input['product_image_url'] ?? null;
        $text = trim($input['text'] ?? '');
        $audio_url = $input['audio_url'] ?? null;
        $duration = (int)($input['duration'] ?? 0);

        // If audio_url contains base64 data, upload it (Cloudinary preferred, local fallback)
        if ($audio_url && (strpos($audio_url, 'data:') === 0 || strlen($audio_url) > 1000)) {
            $base64 = $audio_url;
            $mimeType = 'audio/mp4'; // default for voice messages

            if (strpos($base64, 'data:') === 0) {
                if (preg_match('/^data:([^;]+);base64,/', $base64, $matches)) {
                    $mimeType = strtolower($matches[1]);
                }
                $base64 = substr($base64, strpos($base64, ',') + 1);
            }

            $fileData = base64_decode($base64);
            if ($fileData) {
                $folder = ($duration > 0) ? 'voices' : 'chat_files';
                $uploadedUrl = uploadToCloudinary($fileData, $mimeType, $folder, 'msg_');
                if ($uploadedUrl) {
                    $audio_url = $uploadedUrl;
                }
                // If uploadToCloudinary returns null, keep original audio_url (data URI)
                // so the client can attempt local playback from the raw data
            }
        }

        if ($receiver_id <= 0 || ($text === '' && !$audio_url)) {
            json(400, ['error' => 'receiver_id et text ou audio_url requis']);
        }

        if ($receiver_id === $userId) {
            json(400, ['error' => 'Vous ne pouvez pas vous envoyer un message à vous-même']);
        }

        $stmt = $db->prepare('SELECT id FROM users WHERE id = ?');
        $stmt->execute([$receiver_id]);
        if (!$stmt->fetch()) json(404, ['error' => 'Destinataire non trouvé']);

        if ($product_id) {
            $stmt = $db->prepare('SELECT id FROM products WHERE id = ?');
            $stmt->execute([$product_id]);
            if (!$stmt->fetch()) json(404, ['error' => 'Produit non trouvé']);
        }

        $stmt = $db->prepare('INSERT INTO messages (sender_id, receiver_id, product_id, product_image_url, text, audio_url, duration) VALUES (?, ?, ?, ?, ?, ?, ?)');
        $stmt->execute([$userId, $receiver_id, $product_id ?: null, $product_image_url, $text, $audio_url, $duration]);
        $messageId = (int)$db->lastInsertId();

        // Notifier le destinataire
        $stmtSender = $db->prepare('SELECT name FROM users WHERE id = ?');
        $stmtSender->execute([$userId]);
        $sender = $stmtSender->fetch();
        $senderName = $sender ? $sender['name'] : 'Quelqu\'un';
        $notifMessage = mb_strlen($text) > 80 ? mb_substr($text, 0, 80) . '...' : $text;
        sendNotification($receiver_id, "Nouveau message de $senderName", $notifMessage, 'message', null);

        $stmt = $db->prepare('
            SELECT m.*, u.name AS sender_name
            FROM messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.id = ?
        ');
        $stmt->execute([$messageId]);
        $message = $stmt->fetch();
        $message['id'] = (int)$message['id'];
        $message['sender_id'] = (int)$message['sender_id'];
        $message['receiver_id'] = (int)$message['receiver_id'];
        if ($message['product_id']) $message['product_id'] = (int)$message['product_id'];
        $message['is_read'] = (bool)$message['is_read'];
        $message['product_image_url'] = $message['product_image_url'] ?? null;

        json(201, ['message' => $message]);
    }

    if ($method === 'DELETE') {
        $messageId = isset($_GET['id']) ? (int)$_GET['id'] : 0;
        if ($messageId <= 0) json(400, ['error' => 'ID du message requis']);

        // Only the sender can delete their own message
        $stmt = $db->prepare('SELECT sender_id FROM messages WHERE id = ?');
        $stmt->execute([$messageId]);
        $msg = $stmt->fetch();
        if (!$msg) json(404, ['error' => 'Message non trouvé']);
        if ((int)$msg['sender_id'] !== $userId) json(403, ['error' => 'Vous ne pouvez supprimer que vos propres messages']);

        $stmt = $db->prepare('DELETE FROM messages WHERE id = ?');
        $stmt->execute([$messageId]);
        json(200, ['success' => true, 'message' => 'Message supprimé']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur']);
}
