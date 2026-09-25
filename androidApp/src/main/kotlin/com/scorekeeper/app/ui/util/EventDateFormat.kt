package com.scorekeeper.app.ui.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Short "Oct 20" style date, used on event cards and headers. */
fun formatEventDateShort(millis: Long): String {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${monthName(date)} ${date.dayOfMonth}"
}

/** Long "Tuesday, Oct 20, 2026" style date, used on the Create Event form. */
fun formatEventDateLong(millis: Long): String {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    val weekday = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$weekday, ${monthName(date)} ${date.dayOfMonth}, ${date.year}"
}

private fun monthName(date: LocalDate): String =
    date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
