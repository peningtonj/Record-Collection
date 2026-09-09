package io.github.peningtonj.recordcollection.network.openAi

// No readability4j on web (JVM-only) and the "import from an article" feature is hidden
// there — see docs/WEB_TARGET_PLAN.md. Crude fallback: strip tags/scripts/styles.
// `[\s\S]` stands in for a dot-matches-all (RegexOption.DOT_MATCHES_ALL is JVM/Native only).
private val scriptOrStyle = Regex("<(script|style)[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE)
private val tag = Regex("<[^>]+>")
private val whitespace = Regex("\\s+")

actual fun extractReadableText(url: String, html: String): String =
    html.replace(scriptOrStyle, " ")
        .replace(tag, " ")
        .replace(whitespace, " ")
        .trim()
