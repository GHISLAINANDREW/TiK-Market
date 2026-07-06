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
    json(400, ['error' => 'Image invalide ou trop volumineuse (Max 10MB)']);
}

// --- Cloudinary Integration ---
$cloudName = getenv('CLOUDINARY_CLOUD_NAME');
$apiKey = getenv('CLOUDINARY_API_KEY');
$apiSecret = getenv('CLOUDINARY_API_SECRET');

if ($cloudName && $apiKey && $apiSecret) {
    // Upload to Cloudinary using cURL
    $timestamp = time();
    $signature = sha1("timestamp=$timestamp$apiSecret");

    $url = "https://api.cloudinary.com/v1_1/$cloudName/auto/upload";

    $postData = [
        'file' => 'data:image/jpeg;base64,' . base64_encode($fileData),
        'timestamp' => $timestamp,
        'api_key' => $apiKey,
        'signature' => $signature
    ];

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);

    $response = curl_exec($ch);
    $result = json_decode($response, true);
    curl_close($ch);

    if (isset($result['secure_url'])) {
        json(200, [
            'success' => true,
            'image_url' => $result['secure_url'],
            'filename' => $result['public_id']
        ]);
    } else {
        json(500, ['error' => 'Cloudinary upload failed: ' . ($result['error']['message'] ?? 'Unknown error')]);
    }
} else {
    // --- Fallback to Local Storage (Useful for local testing) ---
    $uploadDir = __DIR__ . '/products/';
    if (!is_dir($uploadDir)) mkdir($uploadDir, 0755, true);

    $finfo = finfo_open(FILEINFO_MIME_TYPE);
    $mimeType = finfo_buffer($finfo, $fileData);
    finfo_close($finfo);

    $allowedMimes = ['image/jpeg'=>'jpg', 'image/png'=>'png', 'image/gif'=>'gif', 'image/webp'=>'webp'];
    $ext = $allowedMimes[$mimeType] ?? 'jpg';
    $newFilename = 'prod_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $ext;

    if (file_put_contents($uploadDir . $newFilename, $fileData)) {
        $protocol = 'https';
        $host = $_SERVER['HTTP_HOST'];
        $baseDir = dirname($_SERVER['SCRIPT_NAME']);
        $imageUrl = "$protocol://$host$baseDir/products/$newFilename";

        json(200, [
            'success' => true,
            'image_url' => $imageUrl,
            'filename' => $newFilename
        ]);
    } else {
        json(500, ['error' => 'Local storage failed']);
    }
}
