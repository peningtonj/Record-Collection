// Create a new file: FilterPreferences.kt
package io.github.peningtonj.recordcollection.viewmodel

import io.github.peningtonj.recordcollection.db.domain.filter.AlbumFilter

object FilterPreferences {
    // Simple in-memory storage for now
    // In a real app, you'd use SharedPreferences on Android or UserDefaults on iOS
    private var savedFilter: AlbumFilter = AlbumFilter()

    fun saveFilter(filter: AlbumFilter) {
        savedFilter = filter
    }

    fun loadFilter(): AlbumFilter {
        return savedFilter
    }

    fun clearFilter() {
        savedFilter = AlbumFilter()
    }
}