package io.github.peningtonj.recordcollection.util

/**
 * [byteCount] cryptographically-secure random bytes, hex-encoded (so `2 * byteCount`
 * lowercase hex chars). Used for the OAuth PKCE code verifier and `state` — those must
 * come from a CSPRNG, not `kotlin.random.Random`.
 *
 * Hex chars are all in the PKCE "unreserved" set, so the output is a valid code verifier
 * for any `byteCount` in `22..64` (43–128 chars).
 */
expect fun secureRandomHex(byteCount: Int): String
