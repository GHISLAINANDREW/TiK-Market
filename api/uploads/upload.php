<?php
require_once __DIR__ . '/../config/database.php';

// Only POST method
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json(405, ['error' => 'Méthode non autorisée']);
}

// Auth required
$userId = getAuthUserId();

// --- Configuration ---
$maxSize = 15 * 1024 * 1024; // 15MB (photos HD + videos courtes; PHP post_max_size=25M via php.ini)
$fileData = null;
$contentType = $_SERVER['CONTENT_TYPE'] ?? '';

if (strpos($contentType, 'application/json') !== false) {
    $input = json_decode(file_get_contents('php://input'), true);
    if (!$input || empty($input['image'])) {
        json(400, ['error' => 'Aucune image reçue (JSON)']);
    }
    $fileData = base64_decode($input['image']);
} else {
    if (!isset($_FILES['image']) || $_FILES['image']['error'] !== UPLOAD_ERR_OK) {
        json(400, ['error' => 'Aucune image reçue ou erreur d\'upload']);
    }
    $fileData = file_get_contents($_FILES['image']['tmp_name']);
}

if (!$fileData || strlen($fileData) > $maxSize) {
    $sizeMb = round(strlen($fileData) / (1024 * 1024), 2);
    json(400, ['error' => "Fichier trop volumineux: {$sizeMb}MB (Max 15MB)"]);
}

// Get MIME type
$finfo = finfo_open(FILEINFO_MIME_TYPE);
$mimeType = finfo_buffer($finfo, $fileData);
finfo_close($finfo);

// Upload using helper
$url = uploadToCloudinary($fileData, $mimeType, 'stories', 'story_');

if ($url) {
    json(200, [
        'success' => true,
        'image_url' => $url,
        'filename' => basename($url)
    ]);
} else {
    json(500, ['error' => 'Échec de l\'upload du média']);
}
