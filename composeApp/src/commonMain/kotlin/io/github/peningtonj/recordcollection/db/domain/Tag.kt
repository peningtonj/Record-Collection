package io.github.peningtonj.recordcollection.db.domain

data class Tag(
    val key: String,
    val value: String,
    val type: TagType,
    val id: String = "${key}-${value}",
)

enum class TagType(val value: String) {
    METADATA("metadata"),
    USER("user_generated"),
    AI_GENERATED("ai_generated");

    companion object {
        fun fromString(value: String): TagType? {
            return entries.find { it.value == value }
        }
    }
}
