<?php
/**
 * Chunked upload API endpoint.
 *
 * POST /uploads/chunked.php  body: {"upload_id":"...", "chunk_index":0, "total_chunks":10, "data":"<base64 chunk>"}
 * → {"success": true, "received": 1, "total": 10}
 *
 * After all chunks are received, call:
 * POST /uploads/chunked.php  body: {"upload_id":"...", "finalize":true, "filename":"video.mp4"}
 * → {"success": true, "image_url": "..."}
 *
 * Chunks are stored in a temp dir keyed by upload_id. On finalize, they are
 * concatenated and uploaded to Cloudinary (or saved locally).
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
if ($method !== 'POST') json(405, ['error' => 'Méthode non autorisée']);

$userId = getAuthUserId();
$input = json_decode(file_get_contents('php://input'), true);
if (!$input) json(400, ['error' => 'Corps de requête invalide']);

$uploadId = preg_replace('/[^a-zA-Z0-9_-]/', '', $input['upload_id'] ?? '');
if ($uploadId === '') json(400, ['error' => 'upload_id requis']);

$tmpDir = sys_get_temp_dir() . '/tik_chunks_' . $userId;
if (!is_dir($tmpDir)) mkdir($tmpDir, 0777, true);

// ── Finalize: concatenate chunks and upload ──
if (!empty($input['finalize'])) {
    $totalChunks = (int)($input['total_chunks'] ?? 0);
    $filename = preg_replace('/[^a-zA-Z0-9._-]/', '', $input['filename'] ?? 'video.mp4');
    if ($totalChunks <= 0) json(400, ['error' => 'total_chunks requis']);

    $combined = '';
    for ($i = 0; $i < $totalChunks; $i++) {
        $chunkFile = "$tmpDir/{$uploadId}_$i";
        if (!file_exists($chunkFile)) json(400, ['error' => "Chunk $i manquant"]);
        $combined .= file_get_contents($chunkFile);
        unlink($chunkFile);
    }

    // Clean up any leftover chunk files for this upload
    foreach (glob("$tmpDir/{$uploadId}_*") as $f) { @unlink($f); }

    if ($combined === '') json(400, ['error' => 'Aucune donnée reçue']);

    $maxSize = 15 * 1024 * 1024;
    if (strlen($combined) > $maxSize) {
        json(400, ['error' => 'Fichier trop volumineux (Max 15MB)']);
    }

    $finfo = finfo_open(FILEINFO_MIME_TYPE);
    $mimeType = finfo_buffer($finfo, $combined);
    finfo_close($finfo);

    $url = uploadToCloudinary($combined, $mimeType, 'stories', 'story_');
    if ($url) {
        json(200, ['success' => true, 'image_url' => $url, 'filename' => basename($url)]);
    } else {
        json(500, ['error' => 'Échec de l\'upload du média']);
    }
}

// ── Receive a chunk ──
$chunkIndex = (int)($input['chunk_index'] ?? -1);
$totalChunks = (int)($input['total_chunks'] ?? 0);
$data = $input['data'] ?? '';
if ($chunkIndex < 0 || $totalChunks <= 0) json(400, ['error' => 'chunk_index et total_chunks requis']);
if ($data === '') json(400, ['error' => 'Donnée de chunk manquante']);

$chunkBytes = base64_decode($data, true);
if ($chunkBytes === false) json(400, ['error' => 'Chunk invalide']);

file_put_contents("$tmpDir/{$uploadId}_$chunkIndex", $chunkBytes);

json(200, ['success' => true, 'received' => $chunkIndex + 1, 'total' => $totalChunks]);