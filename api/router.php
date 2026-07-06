<?php
/**
 * Router for PHP built-in server (CORS + static files).
 * Adds CORS headers to ALL responses (including static files like images).
 *
 * Usage: php -S 0.0.0.0:8081 -t /path/to/api router.php
 */

// ── CORS headers for every response ──
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Platform');
header('Access-Control-Expose-Headers: Content-Type');

// ── Preflight ──
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// ── Fix Authorization header for PHP built-in server ──
// PHP built-in server sometimes strips or renames the Authorization header.
// This ensures it's available via both getallheaders() and $_SERVER.
if (!isset($_SERVER['HTTP_AUTHORIZATION'])) {
    // Try common alternative names
    if (isset($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) {
        $_SERVER['HTTP_AUTHORIZATION'] = $_SERVER['REDIRECT_HTTP_AUTHORIZATION'];
    } elseif (function_exists('apache_request_headers')) {
        $apacheHeaders = apache_request_headers();
        if (isset($apacheHeaders['Authorization'])) {
            $_SERVER['HTTP_AUTHORIZATION'] = $apacheHeaders['Authorization'];
        }
    }
    // Also check if it was passed as HTTP_AUTHORIZATION in env
    if (!isset($_SERVER['HTTP_AUTHORIZATION']) && !empty(getenv('HTTP_AUTHORIZATION'))) {
        $_SERVER['HTTP_AUTHORIZATION'] = getenv('HTTP_AUTHORIZATION');
    }
}

// ── Determine the file path ──
$docRoot = __DIR__;
$uri     = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$file    = $docRoot . $uri;

// ── If it's a PHP file, include it directly (avoid built-in server path resolution bug on Windows) ──
if (str_ends_with($uri, '.php')) {
    $file = is_file($file) ? $file : null;
    if ($file !== null) {
        // Remove any query string from the file path
        $realPath = realpath($file);
        if ($realPath !== false) {
            chdir(dirname($realPath));
            require $realPath;
            return true;
        }
    }
    // Fallback: let the built-in server try
    return false;
}

    // ── Static file (image, CSS, JS, etc.) ──
    if (is_file($file)) {
        // Determine MIME type
        $ext = strtolower(pathinfo($file, PATHINFO_EXTENSION));
        $mimeTypes = [
            // Images
            'jpg'  => 'image/jpeg',
            'jpeg' => 'image/jpeg',
            'png'  => 'image/png',
            'gif'  => 'image/gif',
            'webp' => 'image/webp',
            'svg'  => 'image/svg+xml',
            'ico'  => 'image/x-icon',
            'bmp'  => 'image/bmp',
            // Documents
            'pdf'  => 'application/pdf',
            'doc'  => 'application/msword',
            'docx' => 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'xls'  => 'application/vnd.ms-excel',
            'xlsx' => 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'ppt'  => 'application/vnd.ms-powerpoint',
            'pptx' => 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
            'odt'  => 'application/vnd.oasis.opendocument.text',
            'ods'  => 'application/vnd.oasis.opendocument.spreadsheet',
            // Archives
            'zip'  => 'application/zip',
            'rar'  => 'application/vnd.rar',
            'gz'   => 'application/gzip',
            '7z'   => 'application/x-7z-compressed',
            'tar'  => 'application/x-tar',
            // Text / Data
            'txt'  => 'text/plain',
            'csv'  => 'text/csv',
            'json' => 'application/json',
            'xml'  => 'application/xml',
            'html' => 'text/html',
            'vcf'  => 'text/vcard',
            'ics'  => 'text/calendar',
            'css'  => 'text/css',
            'js'   => 'application/javascript',
            // Audio / Video
            'mp3'  => 'audio/mpeg',
            'mp4'  => 'video/mp4',
            'webm' => 'video/webm',
            'ogg'  => 'audio/ogg',
            'wav'  => 'audio/wav',
            'aac'  => 'audio/aac',
            'flac' => 'audio/flac',
            // Fonts
            'woff' => 'font/woff',
            'woff2'=> 'font/woff2',
            'ttf'  => 'font/ttf',
            'eot'  => 'application/vnd.ms-fontobject',
        ];
        $contentType = $mimeTypes[$ext] ?? 'application/octet-stream';
        header("Content-Type: $contentType");
        header('Content-Length: ' . filesize($file));
        // Content-Disposition: inline for browsers that can display, attachment for unknown
        $inlineTypes = ['image/', 'text/', 'application/pdf', 'audio/', 'video/', 'font/'];
        $isInline = false;
        foreach ($inlineTypes as $prefix) {
            if (str_starts_with($contentType, $prefix)) {
                $isInline = true;
                break;
            }
        }
        $filename = basename($file);
        if ($isInline) {
            header("Content-Disposition: inline; filename=\"$filename\"");
        } else {
            header("Content-Disposition: attachment; filename=\"$filename\"");
        }
        readfile($file);
        exit;
    }

// ── Fallback: 404 ──
http_response_code(404);
header('Content-Type: application/json');
echo json_encode(['error' => 'Not found']);
exit;
