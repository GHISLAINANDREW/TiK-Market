<?php
require_once __DIR__ . '/../config/database.php';

// Only POST method
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json(405, ['error' => 'Méthode non autorisée']);
}

// Auth required
$userId = getAuthUserId();

// --- Configuration ---
$maxSize = 10 * 1024 * 1024; // 10MB
$uploadDir = __DIR__ . '/products/';

if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

// --- Support both multipart (form) and JSON (base64) uploads ---

$fileData = null;
$contentType = $_SERVER['CONTENT_TYPE'] ?? '';

if (strpos($contentType, 'application/json') !== false) {
    // ---- JSON base64 upload ----
    $input = json_decode(file_get_contents('php://input'), true);
    if (!$input || empty($input['image'])) {
        json(400, ['error' => 'Aucune image reçue (JSON)']);
    }
    $fileData = base64_decode($input['image']);
    if ($fileData === false) {
        json(400, ['error' => 'Données base64 invalides']);
    }
} else {
    // ---- Standard form upload ----
    if (!isset($_FILES['image']) || $_FILES['image']['error'] !== UPLOAD_ERR_OK) {
        json(400, ['error' => 'Aucune image reçue ou erreur d\'upload']);
    }
    $fileData = file_get_contents($_FILES['image']['tmp_name']);
}

// Validate size
if (strlen($fileData) > $maxSize) {
    json(400, ['error' => 'Image trop volumineuse. Maximum 10 Mo.']);
}

// Determine extension from actual MIME type
$finfo = finfo_open(FILEINFO_MIME_TYPE);
$mimeType = finfo_buffer($finfo, $fileData);
finfo_close($finfo);

$allowedMimes = [
    'image/jpeg' => 'jpg',
    'image/png'  => 'png',
    'image/gif'  => 'gif',
    'image/webp' => 'webp'
];

if (!array_key_exists($mimeType, $allowedMimes)) {
    json(400, ['error' => 'Format non autorisé. Utilisez JPG, PNG, GIF ou WebP. Reçu: ' . $mimeType]);
}

$ext = $allowedMimes[$mimeType];
$newFilename = 'product_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $ext;
$uploadPath = $uploadDir . $newFilename;

if (file_put_contents($uploadPath, $fileData) === false) {
    json(500, ['error' => 'Erreur lors de la sauvegarde de l\'image']);
}

// Dynamic URL construction - Forced HTTPS for Tunnels (Localtunnel/Ngrok)
$protocol = 'https';
$host = $_SERVER['HTTP_HOST'];

// Detect if we are in a subfolder (like /api/uploads/)
$scriptName = $_SERVER['SCRIPT_NAME']; // e.g. /api/uploads/upload.php
$baseDir = dirname($scriptName);      // e.g. /api/uploads
$imageUrl = "$protocol://$host$baseDir/products/$newFilename";

json(200, [
    'success' => true,
    'image_url' => $imageUrl,
    'filename' => $newFilename
]);
