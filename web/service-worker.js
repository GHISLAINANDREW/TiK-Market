const CACHE_NAME = 'tik-market-v1';
const ASSETS_TO_CACHE = [
  '/',
  '/index.html',
  '/composeApp.js',
  '/composeApp.wasm',
  '/skiko.js',
  '/skiko.wasm',
  '/favicon.svg',
  '/manifest.json'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(ASSETS_TO_CACHE))
  );
});

self.addEventListener('fetch', event => {
  // Only cache GET requests for static assets
  if (event.request.method !== 'GET') return;

  event.respondWith(
    caches.match(event.request)
      .then(response => {
        return response || fetch(event.request).then(fetchResponse => {
          // Don't cache API calls or external domains in a deep way
          if (!event.request.url.includes('onrender.com') && fetchResponse.status === 200) {
             let responseToCache = fetchResponse.clone();
             caches.open(CACHE_NAME).then(cache => cache.put(event.request, responseToCache));
          }
          return fetchResponse;
        });
      })
  );
});
