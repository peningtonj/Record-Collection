package io.github.peningtonj.recordcollection.network.openAi

import net.dankito.readability4j.Readability4J

actual fun extractReadableText(url: String, html: String): String =
    Readability4J(url, html).parse().textContent ?: ""
