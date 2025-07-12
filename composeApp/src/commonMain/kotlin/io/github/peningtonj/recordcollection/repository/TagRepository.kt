package io.github.peningtonj.recordcollection.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.db.mapper.TagMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepository(
    private val database: RecordCollectionDatabase
) {
    
    fun getAllTags(): Flow<List<Tag>> = database.tagsQueries.selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { it.map { TagMapper.toDomain(it) } }
    
    fun getTagsByType(type: String): Flow<List<Tag>> = database.tagsQueries.getByType(type)
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { it.map { TagMapper.toDomain(it) } }
    
    fun getTagsByKey(key: String): Flow<List<Tag>> = database.tagsQueries.getByKey(key)
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { it.map { TagMapper.toDomain(it) } }
    
    fun insertTag(tag: Tag) =
        database.tagsQueries.insert(
                tag_id = tag.id,
                tag_key = tag.key,
                tag_value = tag.value,
                tag_type = tag.type.value
            )
    
    fun deleteTag(id: String) =
        database.tagsQueries.deleteById(id)

    
    fun updateTag(tag: Tag) =
        database.tagsQueries.insert(
                tag_id = tag.id,
                tag_key = tag.key,
                tag_value = tag.value,
                tag_type = tag.type.value
            )
}