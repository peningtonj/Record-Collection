package io.github.peningtonj.recordcollection.util

import java.security.SecureRandom

private val secureRandom = SecureRandom()

actual fun secureRandomHex(byteCount: Int): String {
    val bytes = ByteArray(byteCount)
    secureRandom.nextBytes(bytes)
    return bytes.joinToString("") { byte -> ((byte.toInt() and 0xFF) or 0x100).toString(16).substring(1) }
}
