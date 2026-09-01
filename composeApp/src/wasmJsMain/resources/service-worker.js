/// <reference lib="webworker" />

// ── Cache names ──
const CACHES = {
  STATIC: 'tik-market-static-v3',
  MEDIA: 'tik-market-media-v3',
  API: 'tik-market-api-v3'
};

const STATIC_URLS = [
  '/',
  '/index.html',
  '/manifest.json',
  '/favicon.svg',
  '/composeApp.js',
  '/e7534b326c4501910770.wasm',
  '/dd568dbcd078c0adf7cf.wasm'
];

// ── Install: precache known static assets ──
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHES.STATIC).then(cache => {
      return cache.addAll(STATIC_URLS).catch(() => {
        // Silently ignore individual failures (e.g. 0-byte placeholders)
      });
    }).then(() => self.skipWaiting())
  );
});

// ── Activate: clean old caches ──
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(names => {
      const valid = Object.values(CACHES);
      return Promise.all(
        names.filter(n => !valid.includes(n)).map(n => caches.delete(n))
      );
    }).then(() => self.clients.claim())
  );
});

// ── Helpers ──
function isRenderApi(url) {
  return url.hostname.endsWith('onrender.com') ||
         url.hostname === 'dschang-market.onrender.com';
}

function isCloudinary(url) {
  return url.hostname.endsWith('cloudinary.com');
}

function isStatic(url) {
  return url.origin === self.location.origin &&
    (url.pathname.endsWith('.wasm') ||
     url.pathname.endsWith('.js') ||
     url.pathname.endsWith('.mjs') ||
     url.pathname.endsWith('.map') ||
     url.pathname.endsWith('.css') ||
     url.pathname.endsWith('.json') ||
     url.pathname === '/' ||
     url.pathname === '/index.html' ||
     url.pathname === '/favicon.svg' ||
     url.pathname === '/manifest.json');
}

function isMedia(url) {
  return url.pathname.match(/\.(png|jpg|jpeg|gif|webp|svg|ico|mp3|mp4|webm|ogg|wav)$/i) ||
         isCloudinary(url) ||
         url.pathname.startsWith('/uploads/');
}

function isApiCall(url) {
  return isRenderApi(url) &&
    (url.pathname.includes('/orders/') ||
     url.pathname.includes('/products/') ||
     url.pathname.includes('/messages/') ||
     url.pathname.includes('/auth/') ||
     url.pathname.includes('/cart/') ||
     url.pathname.includes('/shops/') ||
     url.pathname.includes('/stories/') ||
     url.pathname.includes('/wallet/') ||
     url.pathname.includes('/loyalty/') ||
     url.pathname.includes('/live/') ||
     url.pathname.includes('/reels/'));
}

function isProductListing(url) {
  return isRenderApi(url) && url.pathname === '/products/products.php';
}

// ── Fetch: routing ──
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  // Static assets → cache-first
  if (isStatic(url)) {
    event.respondWith(
      caches.open(CACHES.STATIC).then(cache =>
        cache.match(event.request).then(cached => {
          const fetchPromise = fetch(event.request).then(response => {
            if (response.ok) cache.put(event.request, response.clone());
            return response;
          }).catch(() => cached);
          return cached || fetchPromise;
        })
      )
    );
    return;
  }

  // Media (images, audio, video) → cache-first, background refresh
  if (isMedia(url)) {
    event.respondWith(
      caches.open(CACHES.MEDIA).then(cache =>
        cache.match(event.request).then(cached => {
          if (cached) {
            // Refresh in background
            fetch(event.request).then(response => {
              if (response.ok) cache.put(event.request, response);
            }).catch(() => {});
            return cached;
          }
          return fetch(event.request).then(response => {
            if (response.ok) cache.put(event.request, response.clone());
            return response;
          }).catch(() => new Response('', { status: 408 }));
        })
      )
    );
    return;
  }

  // Product listing → cache-first (instant 2nd visit), background refresh
  if (isProductListing(url)) {
    event.respondWith(
      caches.open(CACHES.API).then(cache =>
        cache.match(event.request).then(cached => {
          if (cached) {
            const ts = parseInt(cached.headers.get('X-Cache-Timestamp') || '0');
            if (Date.now() - ts < 300000) return cached; // 5 min fresh
          }
          return fetch(event.request).then(response => {
            const clone = response.clone();
            const headers = new Headers(clone.headers);
            headers.append('X-Cache-Timestamp', Date.now().toString());
            cache.put(event.request, new Response(clone.body, {
              status: clone.status, statusText: clone.statusText, headers: headers
            }));
            return response;
          }).catch(() => cached || new Response('[]', {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
          }));
        })
      )
    );
    return;
  }

  // API calls → network-first, cache fallback (10 min TTL)
  if (isApiCall(url)) {
    event.respondWith(
      fetch(event.request).then(response => {
        const clone = response.clone();
        caches.open(CACHES.API).then(cache => {
          const headers = new Headers(clone.headers);
          headers.append('X-Cache-Timestamp', Date.now().toString());
          const cachedResponse = new Response(clone.body, {
            status: clone.status,
            statusText: clone.statusText,
            headers: headers
          });
          cache.put(event.request, cachedResponse);
        });
        return response;
      }).catch(() =>
        caches.open(CACHES.API).then(cache =>
          cache.match(event.request).then(cached => {
            if (cached) {
              const ts = parseInt(cached.headers.get('X-Cache-Timestamp') || '0');
              if (Date.now() - ts < 600000) return cached; // 10 min valid
            }
            return new Response(JSON.stringify({ error: 'Offline' }), {
              status: 503,
              headers: { 'Content-Type': 'application/json' }
            });
          })
        )
      )
    );
    return;
  }

  // Cross-origin Render API (non-DB calls) → network only, no cache
  if (isRenderApi(url)) {
    event.respondWith(fetch(event.request).catch(() =>
      new Response(JSON.stringify({ error: 'Offline' }), {
        status: 503,
        headers: { 'Content-Type': 'application/json' }
      })
    ));
    return;
  }

  // Default: network-first, cache fallback
  event.respondWith(
    fetch(event.request)
      .then(response => {
        const clone = response.clone();
        caches.open(CACHES.STATIC).then(cache => cache.put(event.request, clone));
        return response;
      })
      .catch(() => caches.match(event.request))
  );
});
