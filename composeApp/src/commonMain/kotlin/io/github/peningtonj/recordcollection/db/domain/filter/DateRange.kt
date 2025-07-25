package io.github.peningtonj.recordcollection.db.domain.filter

import kotlinx.datetime.LocalDate

data class DateRange(
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val name: String? = null
)

fun DateRange.toLabel(): String {
    return when {
        name != null -> name
        start != null && end != null -> "${start.year} - ${end.year}"
        start != null && end == null -> "After ${start.year}"
        start == null && end != null -> "Before ${end.year}"
        else -> "All Time"
    }
}
