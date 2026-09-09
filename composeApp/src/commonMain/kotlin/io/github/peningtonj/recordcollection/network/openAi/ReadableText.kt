package io.github.peningtonj.recordcollection.network.openAi

/**
 * Extracts the main article text from a page's [html]. JVM/Android use readability4j;
 * web has no equivalent (and the "import from an article" feature is hidden there — see
 * docs/WEB_TARGET_PLAN.md), so its `actual` is a crude tag strip.
 */
expect fun extractReadableText(url: String, html: String): String
