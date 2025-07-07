package io.github.peningtonj.recordcollection.di.module.impl

import DatabaseHelper
import io.github.peningtonj.recordcollection.db.DatabaseDriver
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.di.module.DatabaseModule

class ProductionDatabaseModule(
    private val driver: DatabaseDriver
) : DatabaseModule {
    private var database: RecordCollectionDatabase? = null
    
    override fun provideDatabase(): RecordCollectionDatabase {
        return database ?: DatabaseHelper(driver)
            .database
            .also { database = it }
    }
    
    override fun close() {
        database = null
    }
}