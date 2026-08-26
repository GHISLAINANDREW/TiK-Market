const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');

const DIST = path.join(__dirname, 'web_dist');
const PORT = 3000;
const API_BACKEND = 'https://tik-market.onrender.com';

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
  } else if (ext === '.js' || ext === '.css' || ext === '.html') {
    headers['Cache-Control'] = 'no-store, no-cache, must-revalidate';
    headers['Pragma'] = 'no-cache';
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

// Proxy /api/* to the real backend (same as Vercel rewrites)
function proxyAPI(req, res) {
  const apiPath = req.url; // e.g. /api/products/products.php
  const targetUrl = API_BACKEND + apiPath;

  const headers = { ...req.headers };
  headers['host'] = new URL(API_BACKEND).host;
  // Remove problematic headers
  delete headers['origin'];
  delete headers['referer'];

  const proxyReq = https.request(targetUrl, {
    method: req.method,
    headers: headers,
  }, (proxyRes) => {
    // Forward response with CORS headers
    const respHeaders = { ...proxyRes.headers };
    respHeaders['access-control-allow-origin'] = '*';
    respHeaders['access-control-allow-methods'] = 'GET, POST, PUT, DELETE, OPTIONS';
    respHeaders['access-control-allow-headers'] = 'Content-Type, Authorization';
    res.writeHead(proxyRes.statusCode, respHeaders);
    proxyRes.pipe(res);
  });

  proxyReq.on('error', (err) => {
    console.error('  API proxy error:', err.message);
    res.writeHead(502);
    res.end(JSON.stringify({ error: 'Backend unreachable', message: err.message }));
  });

  req.pipe(proxyReq);
}

const server = http.createServer((req, res) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    });
    res.end();
    return;
  }

  // Proxy API requests to backend
  if (req.url.startsWith('/api/')) {
    proxyAPI(req, res);
    return;
  }

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
      const ext = path.extname(filePath).toLowerCase();
      // Static assets → 404 (no SPA fallback)
      if (['.wasm', '.js', '.css', '.svg', '.png', '.jpg', '.map'].includes(ext)) {
        res.writeHead(404);
        res.end('Not Found');
        return;
      }
      // SPA fallback for routes (no extension)
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
  console.log('   API proxy: ' + API_BACKEND + '/api/*');
  console.log('  ==========================================');
  console.log('');
});
