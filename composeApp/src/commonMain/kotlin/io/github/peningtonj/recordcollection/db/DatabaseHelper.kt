package io.github.peningtonj.recordcollection.db

class DatabaseHelper(databaseDriver: DatabaseDriver) {
    private val driver = databaseDriver.createDriver()
    val database = RecordCollectionDatabase(driver)
}