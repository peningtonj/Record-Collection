package io.github.peningtonj.recordcollection.util

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

private external interface Crypto {
    fun getRandomValues(array: Uint8Array): Uint8Array
}

// Browser global `crypto` (Web Crypto API).
private external val crypto: Crypto

actual fun secureRandomHex(byteCount: Int): String {
    val bytes = crypto.getRandomValues(Uint8Array(byteCount))
    val sb = StringBuilder(byteCount * 2)
    for (i in 0 until byteCount) {
        val v = bytes[i].toInt() and 0xFF
        sb.append("0123456789abcdef"[v ushr 4])
        sb.append("0123456789abcdef"[v and 0xF])
    }
    return sb.toString()
}
