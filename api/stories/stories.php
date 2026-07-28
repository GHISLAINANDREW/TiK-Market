<?php
/**
 * Stories API endpoint.
 *
 * GET    /stories/stories.php           → list active stories grouped by shop (not expired)
 * GET    /stories/stories.php?id=X      → single story with replies
 * GET    /stories/stories.php?shop_id=X → stories for a specific shop
 * POST   /stories/stories.php           → create a story (vendor only)
 * POST   /stories/stories.php?reply=X   → reply to a story
 * DELETE /stories/stories.php?id=X      → delete story (owner only)
 *
 * Stories auto-delete after 24h (permanent delete, not soft).
 * Supports: image + video (mp4, webm), caption/note, reply.
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    $db = getDB();

    // ── Auto-migration: create stories + story_replies tables ──
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS stories (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL,
            shop_id INT NOT NULL,
            media_url VARCHAR(500) NOT NULL,
            media_type VARCHAR(10) NOT NULL DEFAULT 'image',
            caption TEXT,
            duration INT DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
            INDEX idx_stories_created (created_at),
            INDEX idx_stories_user_shop (user_id, shop_id)
        )");
    } catch (Exception $e) { error_log("Migration stories table: " . $e->getMessage()); }

    try {
        $db->exec("CREATE TABLE IF NOT EXISTS story_replies (
            id INT AUTO_INCREMENT PRIMARY KEY,
            story_id INT NOT NULL,
            user_id INT NOT NULL,
            text TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            INDEX idx_story_replies_story (story_id)
        )");
    } catch (Exception $e) { error_log("Migration story_replies table: " . $e->getMessage()); }

    // Auto-migration: add is_admin column to stories if missing
    try {
        $db->exec("ALTER TABLE stories ADD COLUMN is_admin TINYINT(1) NOT NULL DEFAULT 0 AFTER duration");
    } catch (Exception $e) { /* column already exists */ }

    // ── AUTO-CLEANUP: permanently delete stories older than 24h ──
    $deleted = 0;
    try {
        $stmt = $db->prepare("DELETE FROM stories WHERE created_at < NOW() - INTERVAL 1 DAY");
        $stmt->execute();
        $deleted = $stmt->rowCount();
    } catch (Exception $e) { error_log("Stories cleanup: " . $e->getMessage()); }

    // ── GET ──
    if ($method === 'GET') {
        $id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
        $shopId = isset($_GET['shop_id']) ? (int)$_GET['shop_id'] : 0;
        $getReplies = isset($_GET['replies']) ? (int)$_GET['replies'] : 0;

        // Single story with replies
        if ($id > 0) {
            $stmt = $db->prepare("
                SELECT s.*, u.name AS user_name, u.avatar AS user_avatar,
                       sh.name AS shop_name, sh.logo AS shop_logo
                FROM stories s
                JOIN users u ON s.user_id = u.id
                JOIN shops sh ON s.shop_id = sh.id
                WHERE s.id = ?
            ");
            $stmt->execute([$id]);
            $story = $stmt->fetch();
            if (!$story) json(404, ['error' => 'Story non trouvée']);

            $story['id'] = (int)$story['id'];
            $story['user_id'] = (int)$story['user_id'];
            $story['shop_id'] = (int)$story['shop_id'];
            $story['duration'] = (int)$story['duration'];

            // Get replies
            $stmtR = $db->prepare("
                SELECT sr.*, u.name AS user_name
                FROM story_replies sr
                JOIN users u ON sr.user_id = u.id
                WHERE sr.story_id = ?
                ORDER BY sr.created_at ASC
            ");
            $stmtR->execute([$id]);
            $replies = $stmtR->fetchAll();
            foreach ($replies as &$r) {
                $r['id'] = (int)$r['id'];
                $r['story_id'] = (int)$r['story_id'];
                $r['user_id'] = (int)$r['user_id'];
            }
            unset($r);
            $story['replies'] = $replies;
            $story['reply_count'] = count($replies);

            json(200, $story);
        }

        // Stories for a specific shop
        if ($shopId > 0) {
            $stmt = $db->prepare("
                SELECT s.*, u.name AS user_name, u.avatar AS user_avatar,
                       sh.name AS shop_name, sh.logo AS shop_logo
                FROM stories s
                JOIN users u ON s.user_id = u.id
                JOIN shops sh ON s.shop_id = sh.id
                WHERE s.shop_id = ? AND s.created_at >= NOW() - INTERVAL 24 HOUR
                ORDER BY s.created_at DESC
            ");
            $stmt->execute([$shopId]);
            $stories = $stmt->fetchAll();
        } else {
            // All active stories grouped by shop
            $stmt = $db->prepare("
                SELECT s.*, u.name AS user_name, u.avatar AS user_avatar,
                       sh.name AS shop_name, sh.logo AS shop_logo
                FROM stories s
                JOIN users u ON s.user_id = u.id
                JOIN shops sh ON s.shop_id = sh.id
                WHERE s.created_at >= NOW() - INTERVAL 24 HOUR
                ORDER BY s.created_at DESC
            ");
            $stmt->execute();
            $stories = $stmt->fetchAll();
        }

        // Normalize types
        foreach ($stories as &$s) {
            $s['id'] = (int)$s['id'];
            $s['user_id'] = (int)$s['user_id'];
            $s['shop_id'] = (int)$s['shop_id'];
            $s['duration'] = (int)$s['duration'];
        }
        unset($s);

        // If requesting replies for all stories
        if ($getReplies > 0) {
            $storyIds = array_column($stories, 'id');
            $replyCounts = [];
            if (!empty($storyIds)) {
                $placeholders = implode(',', array_fill(0, count($storyIds), '?'));
                $stmtR = $db->prepare("SELECT story_id, COUNT(*) AS cnt FROM story_replies WHERE story_id IN ($placeholders) GROUP BY story_id");
                $stmtR->execute($storyIds);
                foreach ($stmtR->fetchAll() as $rc) {
                    $replyCounts[(int)$rc['story_id']] = (int)$rc['cnt'];
                }
            }
            foreach ($stories as &$s) {
                $s['reply_count'] = $replyCounts[$s['id']] ?? 0;
            }
            unset($s);
        }

        json(200, [
            'stories' => $stories,
            'deleted_expired' => $deleted
        ]);
    }

    // ── POST : Create story ──
    if ($method === 'POST') {
        $userId = getAuthUserId();
        // Handle reply to story
        $replyToStoryId = isset($_GET['reply']) ? (int)$_GET['reply'] : 0;
        if ($replyToStoryId > 0) {
            $input = json_decode(file_get_contents('php://input'), true);
            if (!$input) json(400, ['error' => 'Corps de requête invalide']);
            $text = trim($input['text'] ?? '');
            if ($text === '') json(400, ['error' => 'Texte requis']);

            // Verify story exists
            $stmt = $db->prepare("SELECT id FROM stories WHERE id = ?");
            $stmt->execute([$replyToStoryId]);
            if (!$stmt->fetch()) json(404, ['error' => 'Story introuvable']);

            $stmt = $db->prepare("INSERT INTO story_replies (story_id, user_id, text) VALUES (?, ?, ?)");
            $stmt->execute([$replyToStoryId, $userId, $text]);
            $replyId = (int)$db->lastInsertId();

            // Send notification to story owner
            $stmt = $db->prepare("SELECT user_id FROM stories WHERE id = ?");
            $stmt->execute([$replyToStoryId]);
            $storyOwner = $stmt->fetch();
            if ($storyOwner && (int)$storyOwner['user_id'] !== $userId) {
                $currentUser = $db->prepare("SELECT name FROM users WHERE id = ?");
                $currentUser->execute([$userId]);
                $cu = $currentUser->fetch();
                sendNotification((int)$storyOwner['user_id'], "Réponse à votre story", $cu['name'] . ' a répondu à votre story : ' . $text, 'story', $replyToStoryId);
            }

            json(201, ['success' => true, 'reply_id' => $replyId]);
        }

        // Create new story
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) json(400, ['error' => 'Corps de requête invalide']);

        $shopId = (int)($input['shop_id'] ?? 0);
        $mediaUrl = trim($input['media_url'] ?? '');
        $mediaType = trim($input['media_type'] ?? 'image');
        $caption = trim($input['caption'] ?? '');
        $duration = (int)($input['duration'] ?? 0);

        if ($shopId <= 0 || $mediaUrl === '') {
            json(400, ['error' => 'shop_id et media_url requis']);
        }

        // Verify vendor owns the shop (skip for admin)
        $isAdmin = false;
        $stmt = $db->prepare("SELECT role FROM users WHERE id = ?");
        $stmt->execute([$userId]);
        $userRow = $stmt->fetch();
        if ($userRow && $userRow['role'] === 'admin') {
            $isAdmin = true;
        } else {
            $stmt = $db->prepare("SELECT id FROM shops WHERE id = ? AND vendor_id = ?");
            $stmt->execute([$shopId, $userId]);
            if (!$stmt->fetch()) json(403, ['error' => 'Vous n\'êtes pas le propriétaire de cette boutique']);
        }

        // Validate media type
        if (!in_array($mediaType, ['image', 'video', 'text'])) {
            $mediaType = 'image';
        }

        $stmt = $db->prepare("INSERT INTO stories (user_id, shop_id, media_url, media_type, caption, duration, is_admin) VALUES (?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([$userId, $shopId, $mediaUrl, $mediaType, $caption ?: null, $duration, $isAdmin ? 1 : 0]);
        $storyId = (int)$db->lastInsertId();

        // Return the created story
        $stmt = $db->prepare("
            SELECT s.*, u.name AS user_name, u.avatar AS user_avatar,
                   sh.name AS shop_name, sh.logo AS shop_logo
            FROM stories s
            JOIN users u ON s.user_id = u.id
            JOIN shops sh ON s.shop_id = sh.id
            WHERE s.id = ?
        ");
        $stmt->execute([$storyId]);
        $story = $stmt->fetch();
        $story['id'] = (int)$story['id'];
        $story['user_id'] = (int)$story['user_id'];
        $story['shop_id'] = (int)$story['shop_id'];
        $story['duration'] = (int)$story['duration'];
        $story['replies'] = [];
        $story['reply_count'] = 0;

        // Notify shop subscribers
        $stmtSubs = $db->prepare("SELECT DISTINCT user_id FROM shop_favorites WHERE shop_id = ?");
        $stmtSubs->execute([$shopId]);
        $subs = $stmtSubs->fetchAll(PDO::FETCH_COLUMN);
        $shopName = $story['shop_name'];
        foreach ($subs as $subId) {
            $subId = (int)$subId;
            if ($subId !== $userId) {
                sendNotification($subId, "Nouvelle story de $shopName", $caption ?: "Découvrez la nouvelle story de $shopName", 'story', $storyId);
            }
        }

        json(201, $story);
    }

    // ── DELETE : Delete story ──
    if ($method === 'DELETE') {
        $userId = getAuthUserId();
        $id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
        if ($id <= 0) json(400, ['error' => 'ID story requis']);

        $stmt = $db->prepare("SELECT s.user_id, u.role AS owner_role FROM stories s JOIN users u ON s.user_id = u.id WHERE s.id = ?");
        $stmt->execute([$id]);
        $story = $stmt->fetch();
        if (!$story) json(404, ['error' => 'Story non trouvée']);
        // Allow admin to delete any story
        $callerStmt = $db->prepare("SELECT role FROM users WHERE id = ?");
        $callerStmt->execute([$userId]);
        $caller = $callerStmt->fetch();
        $isAdminCaller = $caller && $caller['role'] === 'admin';
        if ((int)$story['user_id'] !== $userId && !$isAdminCaller) {
            json(403, ['error' => 'Vous ne pouvez supprimer que vos propres stories']);
        }

        $stmt = $db->prepare("DELETE FROM stories WHERE id = ?");
        $stmt->execute([$id]);

        json(200, ['success' => true, 'message' => 'Story supprimée']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (PDOException $e) {
    json(500, ['error' => 'Erreur serveur: ' . $e->getMessage()]);
} catch (Exception $e) {
    json(500, ['error' => 'Erreur: ' . $e->getMessage()]);
}
