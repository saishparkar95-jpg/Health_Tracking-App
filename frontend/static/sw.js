/**
 * HealthTrack AI - Progressive Web App (PWA) Service Worker
 * Version: 1.0.0
 * Provides offline asset caching, network resilience, and installable app lifecycle management.
 */

const CACHE_NAME = 'healthtrack-ai-v1';
const CORE_ASSETS = [
    '/',
    '/static/css/style.css',
    '/static/js/main.js',
    '/static/manifest.json',
    '/static/images/logo.svg',
    '/static/images/icon-192.png',
    '/static/images/icon-512.png',
    'https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800;900&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap',
    'https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js'
];

// 1. Install Event - Pre-cache core application shell assets
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => {
                console.log('[Service Worker] Pre-caching core offline assets...');
                return cache.addAll(CORE_ASSETS);
            })
            .then(() => self.skipWaiting())
            .catch((err) => {
                console.warn('[Service Worker] Some assets failed to pre-cache:', err);
            })
    );
});

// 2. Activate Event - Clean up outdated cache versions
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((cacheNames) => {
            return Promise.all(
                cacheNames.map((name) => {
                    if (name !== CACHE_NAME) {
                        console.log('[Service Worker] Removing old cache:', name);
                        return caches.delete(name);
                    }
                })
            );
        }).then(() => self.clients.claim())
    );
});

// 3. Fetch Event - Stale-while-revalidate for static assets, Network-first for pages
self.addEventListener('fetch', (event) => {
    const request = event.request;
    
    // Ignore non-GET requests (POST form submissions should always reach server)
    if (request.method !== 'GET') {
        return;
    }

    const url = new URL(request.url);

    // Static assets (CSS, JS, Fonts, Images) -> Cache First with Network Fallback
    if (url.pathname.startsWith('/static/') || 
        url.hostname.includes('fonts.googleapis.com') || 
        url.hostname.includes('fonts.gstatic.com') ||
        url.hostname.includes('cdn.jsdelivr.net')) {
        
        event.respondWith(
            caches.match(request).then((cachedResponse) => {
                if (cachedResponse) {
                    // Fetch fresh copy in background to update cache
                    fetch(request).then((networkResponse) => {
                        if (networkResponse && networkResponse.status === 200) {
                            caches.open(CACHE_NAME).then((cache) => cache.put(request, networkResponse));
                        }
                    }).catch(() => {/* Offline background update ignored */});
                    
                    return cachedResponse;
                }

                return fetch(request).then((networkResponse) => {
                    if (networkResponse && networkResponse.status === 200) {
                        const responseClone = networkResponse.clone();
                        caches.open(CACHE_NAME).then((cache) => cache.put(request, responseClone));
                    }
                    return networkResponse;
                });
            })
        );
        return;
    }

    // HTML Navigation Requests -> Network First with Cache Fallback
    if (request.mode === 'navigate' || request.headers.get('accept')?.includes('text/html')) {
        event.respondWith(
            fetch(request)
                .then((networkResponse) => {
                    // Cache successful navigation responses
                    if (networkResponse && networkResponse.status === 200) {
                        const responseClone = networkResponse.clone();
                        caches.open(CACHE_NAME).then((cache) => cache.put(request, responseClone));
                    }
                    return networkResponse;
                })
                .catch(async () => {
                    // Network failed - Try serving cached page
                    const cachedResponse = await caches.match(request);
                    if (cachedResponse) {
                        return cachedResponse;
                    }
                    // If page is not in cache, fallback to cached dashboard root
                    const rootFallback = await caches.match('/');
                    if (rootFallback) {
                        return rootFallback;
                    }
                    return new Response(
                        `<!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Offline - HealthTrack AI</title>
                            <style>
                                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0a0f1d; color: #f9fafb; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; padding: 1.5rem; text-align: center; }
                                .card { background: #131b2e; border: 1px solid rgba(255,255,255,0.1); border-radius: 1rem; padding: 2.5rem; max-width: 420px; }
                                h1 { font-size: 1.75rem; margin-bottom: 0.5rem; }
                                p { color: #9ca3af; line-height: 1.5; margin-bottom: 1.5rem; font-size: 0.95rem; }
                                .btn { display: inline-block; background: #10b981; color: #fff; padding: 0.75rem 1.5rem; border-radius: 0.5rem; text-decoration: none; font-weight: 600; cursor: pointer; border: none; }
                            </style>
                        </head>
                        <body>
                            <div class="card">
                                <div style="font-size: 3rem; margin-bottom: 1rem;">📶</div>
                                <h1>You are Offline</h1>
                                <p>HealthTrack AI is currently running in offline mode. Reconnect to the internet to sync your health records.</p>
                                <button onclick="window.location.reload()" class="btn">Retry Connection</button>
                            </div>
                        </body>
                        </html>`,
                        { headers: { 'Content-Type': 'text/html' } }
                    );
                })
        );
        return;
    }

    // Default fallback
    event.respondWith(
        caches.match(request).then((cachedResponse) => {
            return cachedResponse || fetch(request);
        })
    );
});
