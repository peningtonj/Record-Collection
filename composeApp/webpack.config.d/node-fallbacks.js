// okio (pulled in transitively via coil) references Node's `os`/`path` when it builds
// its default FileSystem — e.g. coil's ImageLoader.Builder calls `os.tmpdir()`, which
// crashes in the browser. Point webpack at browser shims (npm deps declared in
// build.gradle.kts) so those code paths return something sane instead of throwing.
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign({}, config.resolve.fallback, {
    os: require.resolve("os-browserify/browser"),
    path: require.resolve("path-browserify"),
    fs: false,
});
