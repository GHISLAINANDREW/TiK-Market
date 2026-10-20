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

// Get MIME type robustly
$mimeType = 'image/jpeg';
if (function_exists('finfo_open')) {
    $finfo = @finfo_open(FILEINFO_MIME_TYPE);
    if ($finfo) {
        $detected = @finfo_buffer($finfo, $fileData);
        @finfo_close($finfo);
        if ($detected) $mimeType = $detected;
    }
}
if ($mimeType === 'application/octet-stream' || !$mimeType) {
    if (str_starts_with($fileData, "\xFF\xD8\xFF")) $mimeType = 'image/jpeg';
    elseif (str_starts_with($fileData, "\x89PNG\r\n\x1a\n")) $mimeType = 'image/png';
    elseif (str_starts_with($fileData, "RIFF") && substr($fileData, 8, 4) === 'WEBP') $mimeType = 'image/webp';
}

// Upload using helper
try {
    $url = uploadToCloudinary($fileData, $mimeType, 'stories', 'story_');
    if ($url) {
        json(200, [
            'success' => true,
            'image_url' => $url,
            'filename' => basename($url)
        ]);
    } else {
        json(500, ['error' => 'Échec de l\'upload du média (Stockage local échoué)']);
    }
} catch (Exception $e) {
    json(500, ['error' => 'Erreur serveur upload: ' . $e->getMessage()]);
}
