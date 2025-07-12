package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.Tags
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.db.domain.TagType

object TagMapper {
    fun toDomain(tag: Tags) : Tag {
        return Tag(
            id = tag.tag_id,
            key = tag.tag_key,
            value = tag.tag_value,
            type = TagType.fromString(tag.tag_type) ?: TagType.AI_GENERATED
        )
    }
}