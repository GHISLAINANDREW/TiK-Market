<?php
/**
 * Video upload endpoint with FFmpeg optimization.
 *
 * POST /uploads/video.php  (multipart/form-data with field "video")
 *   OR  (application/json with base64 "video")
 * → {
 *     "success": true,
 *     "video_url": "/uploads/videos/xxx_hd.mp4",   // HD version (primary)
 *     "hd_url": "/uploads/videos/xxx_hd.mp4",
 *     "sd_url": "/uploads/videos/xxx_sd.mp4",
 *     "filename": "xxx_hd.mp4"
 *   }
 *
 * Compresses the video into 2 quality versions (HD + SD) using FFmpeg and
 * stores them locally on the server (replacing Cloudinary for videos).
 */

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../config/ffmpeg.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json(405, ['error' => 'Méthode non autorisée']);
}

$userId = getAuthUserId();

// --- Configuration ---
$maxSize = 100 * 1024 * 1024; // 100MB for videos
$fileData = null;
$contentType = $_SERVER['CONTENT_TYPE'] ?? '';

if (strpos($contentType, 'application/json') !== false) {
    $input = json_decode(file_get_contents('php://input'), true);
    if (!$input || empty($input['video'])) {
        json(400, ['error' => 'Aucune vidéo reçue (JSON)']);
    }
    $fileData = base64_decode($input['video']);
} else {
    if (!isset($_FILES['video']) || $_FILES['video']['error'] !== UPLOAD_ERR_OK) {
        json(400, ['error' => 'Aucune vidéo reçue ou erreur d\'upload']);
    }
    $fileData = file_get_contents($_FILES['video']['tmp_name']);
}

if (!$fileData || strlen($fileData) > $maxSize) {
    $sizeMb = round(strlen($fileData) / (1024 * 1024), 2);
    json(400, ['error' => "Fichier trop volumineux: {$sizeMb}MB (Max 100MB)"]);
}

// Get MIME type
$finfo = finfo_open(FILEINFO_MIME_TYPE);
$mimeType = finfo_buffer($finfo, $fileData);
finfo_close($finfo);

// Only accept video files.
if (strpos($mimeType, 'video/') !== 0) {
    json(400, ['error' => 'Le fichier doit être une vidéo']);
}

// ── Save the original to a temp file ──
$uploadsDir = uploadsDir();
$videosDir = $uploadsDir . '/videos';
if (!is_dir($videosDir)) mkdir($videosDir, 0777, true);

$tmpFile = tempnam(sys_get_temp_dir(), 'tik_vid_');
file_put_contents($tmpFile, $fileData);

// ── Optimize with FFmpeg (2 quality versions) ──
$prefix = 'vid_' . $userId;
$result = optimizeVideo($tmpFile, $videosDir, $prefix);

// Clean up temp file.
@unlink($tmpFile);

if (!$result) {
    json(500, ['error' => 'Échec de l\'optimisation vidéo']);
}

// Build absolute URLs.
$base = getenv('APP_URL') ?: ('https://' . ($_SERVER['HTTP_HOST'] ?? 'tik-market.onrender.com'));
$hdUrl = $base . $result['hd'];
$sdUrl = $base . $result['sd'];

json(200, [
    'success' => true,
    'video_url' => $hdUrl,
    'hd_url' => $hdUrl,
    'sd_url' => $sdUrl,
    'filename' => basename($result['hd']),
]);