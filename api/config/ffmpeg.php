<?php
/**
 * FFmpeg video optimization helper.
 *
 * Compresses uploaded videos and generates 2 quality versions (HD + SD) to
 * replace Cloudinary's `q_auto` adaptive quality. Videos are stored locally
 * on the server (Oracle Cloud disk) instead of Cloudinary.
 *
 * Usage:
 *   $result = optimizeVideo($sourcePath, $targetDir, $prefix);
 *   // $result = ['hd' => '/uploads/videos/xxx_hd.mp4', 'sd' => '/uploads/videos/xxx_sd.mp4']
 *
 * Requires FFmpeg installed on the server (see README / deploy notes).
 */

/** Returns the FFmpeg binary path, or null if not available. */
function ffmpegPath(): ?string {
    $env = getenv('FFMPEG_PATH');
    if ($env && file_exists($env)) return $env;

    // Common locations.
    $candidates = [
        '/usr/bin/ffmpeg',
        '/usr/local/bin/ffmpeg',
        '/opt/ffmpeg/bin/ffmpeg',
        'C:\\ffmpeg\\bin\\ffmpeg.exe',
    ];
    foreach ($candidates as $c) {
        if (file_exists($c)) return $c;
    }

    // Try `which ffmpeg`.
    if (function_exists('shell_exec')) {
        $which = trim((string)shell_exec('which ffmpeg 2>/dev/null'));
        if ($which !== '') return $which;
    }
    return null;
}

/**
 * Compresses a video into 2 quality versions (HD + SD) using FFmpeg.
 *
 * @param string $sourcePath Absolute path to the source video file.
 * @param string $targetDir  Absolute path to the output directory (must exist).
 * @param string $prefix     Filename prefix (e.g. 'reel_123').
 * @return array|null        ['hd' => relUrl, 'sd' => relUrl] or null on failure.
 */
function optimizeVideo(string $sourcePath, string $targetDir, string $prefix): ?array {
    $ffmpeg = ffmpegPath();
    if (!$ffmpeg) {
        error_log('[TiK-Market] FFmpeg non trouvé — vidéo non optimisée');
        return null;
    }

    if (!is_dir($targetDir)) {
        if (!@mkdir($targetDir, 0777, true)) {
            error_log('[TiK-Market] Impossible de créer le dossier vidéo: ' . $targetDir);
            return null;
        }
    }

    $stamp = time() . '_' . bin2hex(random_bytes(4));
    $hdPath = $targetDir . '/' . $prefix . '_' . $stamp . '_hd.mp4';
    $sdPath = $targetDir . '/' . $prefix . '_' . $stamp . '_sd.mp4';

    // HD: 720p, H.264, ~1.5 Mbps, AAC audio.
    $hdCmd = escapeshellarg($ffmpeg) . ' -y -i ' . escapeshellarg($sourcePath)
        . ' -vf "scale=-2:720" -c:v libx264 -preset veryfast -crf 23 -maxrate 1500k -bufsize 3000k'
        . ' -c:a aac -b:a 96k -movflags +faststart ' . escapeshellarg($hdPath) . ' 2>&1';

    // SD: 480p, H.264, ~700 kbps, AAC audio.
    $sdCmd = escapeshellarg($ffmpeg) . ' -y -i ' . escapeshellarg($sourcePath)
        . ' -vf "scale=-2:480" -c:v libx264 -preset veryfast -crf 28 -maxrate 700k -bufsize 1400k'
        . ' -c:a aac -b:a 64k -movflags +faststart ' . escapeshellarg($sdPath) . ' 2>&1';

    $hdOk = false;
    $sdOk = false;

    if (function_exists('shell_exec')) {
        shell_exec($hdCmd);
        $hdOk = file_exists($hdPath) && filesize($hdPath) > 0;
        if (!$hdOk) {
            // Retry with a simpler command (some servers lack libx264).
            shell_exec(escapeshellarg($ffmpeg) . ' -y -i ' . escapeshellarg($sourcePath)
                . ' -vf "scale=-2:720" -c:v libx264 -preset veryfast -crf 23 -c:a aac -b:a 96k '
                . escapeshellarg($hdPath) . ' 2>&1');
            $hdOk = file_exists($hdPath) && filesize($hdPath) > 0;
        }

        shell_exec($sdCmd);
        $sdOk = file_exists($sdPath) && filesize($sdPath) > 0;
    }

    // If FFmpeg failed entirely, fall back to storing the original as both versions.
    if (!$hdOk && !$sdOk) {
        error_log('[TiK-Market] FFmpeg a échoué — stockage de l\'original');
        $origPath = $targetDir . '/' . $prefix . '_' . $stamp . '_orig.mp4';
        if (@copy($sourcePath, $origPath)) {
            $rel = '/uploads/videos/' . basename($origPath);
            return ['hd' => $rel, 'sd' => $rel];
        }
        return null;
    }

    // Build relative URLs (relative to the web root /api/../).
    $baseRel = '/uploads/videos/';
    $result = [];
    if ($hdOk) $result['hd'] = $baseRel . basename($hdPath);
    if ($sdOk) $result['sd'] = $baseRel . basename($sdPath);

    // If only one version succeeded, use it for both.
    if (!isset($result['hd']) && isset($result['sd'])) $result['hd'] = $result['sd'];
    if (isset($result['hd']) && !isset($result['sd'])) $result['sd'] = $result['hd'];

    return $result;
}

/**
 * Returns the absolute path to the local uploads directory.
 */
function uploadsDir(): string {
    return __DIR__ . '/../uploads';
}