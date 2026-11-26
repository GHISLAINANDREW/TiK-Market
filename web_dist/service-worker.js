/// <reference lib="webworker" />

// ── Cache names (v7 - Realtime Sync) ──
const CACHES = {
  STATIC: 'tik-market-static-v7',
  MEDIA: 'tik-market-media-v7'
};

// ── Install: skip waiting immediately ──
self.addEventListener('install', event => {
  self.skipWaiting();
});

// ── Activate: delete ALL old stale caches ──
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(names => {
      return Promise.all(
        names.map(name => {
          if (!Object.values(CACHES).includes(name)) {
            console.log('[SW] Deleting old cache:', name);
            return caches.delete(name);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

// ── Helper: detect API calls ──
function isApiCall(url) {
  return url.pathname.includes('/api/') ||
         url.pathname.includes('/live/') ||
         url.pathname.includes('/products/') ||
         url.pathname.includes('/shops/') ||
         url.pathname.includes('/orders/') ||
         url.pathname.includes('/reels/') ||
         url.pathname.includes('/messages/') ||
         url.pathname.includes('/auth/') ||
         url.pathname.includes('/admin/') ||
         url.pathname.endsWith('.php');
}

// ── Fetch: routing ──
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  // 1. ALL API calls (live, products, shops, etc.) → NETWORK ONLY (No stale caching)
  if (isApiCall(url)) {
    event.respondWith(
      fetch(event.request, { cache: 'no-store' }).catch(() => {
        return new Response(JSON.stringify({ error: 'Réseau indisponible' }), {
          status: 503,
          headers: { 'Content-Type': 'application/json' }
        });
      })
    );
    return;
  }

  // 2. Media (images, audio, video) → Network first, cache fallback
  if (url.pathname.match(/\.(png|jpg|jpeg|gif|webp|svg|ico|mp3|mp4|webm)$/i)) {
    event.respondWith(
      fetch(event.request).then(response => {
        if (response.ok) {
          const clone = response.clone();
          caches.open(CACHES.MEDIA).then(cache => cache.put(event.request, clone));
        }
        return response;
      }).catch(() => caches.match(event.request))
    );
    return;
  }

  // 3. Static assets & HTML → Network first, cache fallback
  event.respondWith(
    fetch(event.request).then(response => {
      if (response.ok && event.request.method === 'GET') {
        const clone = response.clone();
        caches.open(CACHES.STATIC).then(cache => cache.put(event.request, clone));
      }
      return response;
    }).catch(() => caches.match(event.request))
  );
});
