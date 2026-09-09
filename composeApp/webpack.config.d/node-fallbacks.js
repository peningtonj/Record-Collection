// okio-js (pulled in transitively) references Node's `os`/`path` on a code path that
// never runs in a browser. Tell webpack to resolve them to empty modules instead of
// warning about missing Node polyfills.
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign({}, config.resolve.fallback, {
    os: false,
    path: false,
    fs: false,
});
