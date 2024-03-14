const relatedAppVersion = "daad64f8f1b1a4e9591f2ac5fe3ff83b1dd8aa5f";
const urlsToCache = ["index.html","manifest.json","img/icon.svg","js/app.6FB3D15406889517A485622F4146E6F7.js","css/styles.124c630f5944a41a0a1eabf3a12fff14a927512b.css"];

const cacheKeyPrefix = 'nenadalm.player-order-selector.';
const cacheKey = `${cacheKeyPrefix}resources.${relatedAppVersion}`;

function ensureHtmlVersionMatches(cache) {
    return cache.match(new Request('index.html'))
        .then(response => response.text())
        .then(html => html.match(/<meta name="app-version" content="(.*?)">/)[1])
        .then(version => {
            if (version !== relatedAppVersion) {
                return Promise.reject(`Incorrect index.html version ${version} doesn't match worker.js version ${relatedAppVersion}`);
            }
        })
}

self.addEventListener('install', event => {
    event.waitUntil(caches.open(cacheKey).then(
        cache => cache.addAll(urlsToCache)
            .then(() => ensureHtmlVersionMatches(cache))));
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys()
            .then(keys => keys.filter(key => key.startsWith(cacheKeyPrefix)))
            .then(keys => keys.filter(key => key !== cacheKey))
            .then(oldKeys => Promise.all(oldKeys.map(key => caches.delete(key))))
    );
});

self.addEventListener('fetch', event => {
    event.respondWith(caches.open(cacheKey)
                      .then(cache => cache.match(event.request))
                      .then(response => response ?? fetch(event.request)));
});
