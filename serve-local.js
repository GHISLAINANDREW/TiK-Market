const http = require('http');
const fs = require('fs');
const path = require('path');

const DIST = path.join(__dirname, 'web_dist');
const PORT = 3000;

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.wasm': 'application/wasm',
  '.json': 'application/json',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.txt': 'text/plain',
  '.map': 'application/json',
  '.ico': 'image/x-icon',
};

// CORS headers required for Compose WasmJS (SharedArrayBuffer)
const CORS_HEADERS = {
  'Cross-Origin-Embedder-Policy': 'credentialless',
  'Cross-Origin-Opener-Policy': 'same-origin',
};

function serveFile(res, filePath, data) {
  const ext = path.extname(filePath).toLowerCase();
  const mime = MIME[ext] || 'application/octet-stream';
  const headers = { 'Content-Type': mime, ...CORS_HEADERS };

  if (ext === '.wasm') {
    headers['Cache-Control'] = 'public, max-age=31536000, immutable';
  } else if (ext === '.js' || ext === '.css') {
    headers['Cache-Control'] = 'public, max-age=86400';
  }

  res.writeHead(200, headers);
  res.end(data);
}

function serveIndex(res) {
  fs.readFile(path.join(DIST, 'index.html'), (err, data) => {
    if (err) {
      res.writeHead(404);
      res.end('Not Found');
    } else {
      serveFile(res, path.join(DIST, 'index.html'), data);
    }
  });
}

const server = http.createServer((req, res) => {
  let url = req.url.split('?')[0];
  if (url === '/') url = '/index.html';

  const filePath = path.join(DIST, url);
  const resolved = path.resolve(filePath);
  const distResolved = path.resolve(DIST);

  // Security: prevent path traversal
  if (!resolved.startsWith(distResolved)) {
    res.writeHead(403);
    res.end('Forbidden');
    return;
  }

  fs.readFile(filePath, (err, data) => {
    if (err) {
      // SPA fallback: serve index.html for non-file routes
      serveIndex(res);
      return;
    }
    serveFile(res, filePath, data);
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('');
  console.log('  ==========================================');
  console.log('   Tik-Market local server');
  console.log('   http://localhost:' + PORT);
  console.log('   Serving: ' + DIST);
  console.log('  ==========================================');
  console.log('');
});
