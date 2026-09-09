// Serve the dev build on the loopback IP + port the desktop OAuth handler already
// registers with Spotify (http://127.0.0.1:8888/callback). Open the app at
// http://127.0.0.1:8888 — not localhost — so window.location.origin matches.
if (config.devServer) {
    config.devServer.host = '127.0.0.1';
    config.devServer.port = 8888;
    config.devServer.allowedHosts = 'all';
    // Don't fall back to index.html for /callback — the OAuth popup only needs its URL read.
    config.devServer.historyApiFallback = false;
}
