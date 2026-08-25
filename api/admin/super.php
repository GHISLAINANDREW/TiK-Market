<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$userId = getAuthUserId();

$db = getDB();
$stmt = $db->prepare('SELECT role FROM users WHERE id = ?');
$stmt->execute([$userId]);
$userRole = $stmt->fetchColumn();

if ($userRole !== 'super_admin') {
    json(403, ['error' => 'Accès restreint au Super Administrateur uniquement.']);
}

try {
    if ($method === 'GET') {
        // 1. Global KPIs (Detailed)
        $stats = [];
        $stats['users'] = $db->query("SELECT role, status, COUNT(*) as count FROM users GROUP BY role, status")->fetchAll();
        $stats['shops'] = $db->query("SELECT status, is_verified, COUNT(*) as count FROM shops GROUP BY status, is_verified")->fetchAll();
        $stats['products'] = $db->query("SELECT is_active, COUNT(*) as count FROM products GROUP BY is_active")->fetchAll();
        $stats['orders'] = $db->query("SELECT status, COUNT(*) as count FROM orders GROUP BY status")->fetchAll();

        // Revenue by month
        $stats['revenue'] = $db->query("
            SELECT DATE_FORMAT(created_at, '%Y-%m') as month, SUM(total_amount) as total
            FROM orders WHERE status = 'delivered'
            GROUP BY month ORDER BY month DESC LIMIT 12
        ")->fetchAll();

        // 2. All Reports
        $reports = $db->query("
            SELECT r.*, u.name as reporter_name
            FROM reports r
            JOIN users u ON r.reporter_id = u.id
            ORDER BY r.created_at DESC
        ")->fetchAll();

        // 3. System Configuration (Mocked/Simple)
        $config = [
            'maintenance_mode' => false,
            'app_version' => '1.2.0',
            'min_version' => '1.0.0',
            'commission_rate' => 5.0
        ];

        json(200, [
            'stats' => $stats,
            'reports' => $reports,
            'config' => $config
        ]);
    }

    if ($method === 'POST') {
        $input = json_decode(file_get_contents('php://input'), true);
        $action = $input['action'] ?? '';

        if ($action === 'update_report_status') {
            $reportId = (int)($input['report_id'] ?? 0);
            $status = $input['status'] ?? 'resolved';
            $stmt = $db->prepare("UPDATE reports SET status = ? WHERE id = ?");
            $stmt->execute([$status, $reportId]);
            json(200, ['success' => true]);
        }

        if ($action === 'broadcast_system') {
            $title = $input['title'] ?? 'Message Système';
            $message = $input['message'] ?? '';
            $stmt = $db->prepare("INSERT INTO notifications (user_id, title, message, type) SELECT id, ?, ?, 'system' FROM users");
            $stmt->execute([$title, $message]);
            json(200, ['success' => true]);
        }

        json(400, ['error' => 'Action non supportée']);
    }

    json(405, ['error' => 'Méthode non autorisée']);
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
