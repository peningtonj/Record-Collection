
package io.github.peningtonj.recordcollection.di.module

import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase

interface DatabaseModule {
    fun provideDatabase(): RecordCollectionDatabase
    fun close()
}