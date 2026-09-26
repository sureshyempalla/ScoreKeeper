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

/**
 * Short range like "Oct 20" for a single-day event, or "Oct 20 – Oct 23"
 * (dropping the repeated month/year when they match, e.g. "Oct 20 – 23") for
 * a multi-day one. [endMillis] null or equal to [startMillis]'s day is
 * treated as single-day.
 */
fun formatEventDateRangeShort(startMillis: Long, endMillis: Long?): String {
    val start = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    if (endMillis == null) return "${monthName(start)} ${start.dayOfMonth}"
    val end = Instant.fromEpochMilliseconds(endMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    if (end == start) return "${monthName(start)} ${start.dayOfMonth}"
    return if (end.year == start.year && end.month == start.month) {
        "${monthName(start)} ${start.dayOfMonth} – ${end.dayOfMonth}"
    } else {
        "${monthName(start)} ${start.dayOfMonth} – ${monthName(end)} ${end.dayOfMonth}"
    }
}

/** Long range for the Create Event form: "Tuesday, Oct 20, 2026" or "Oct 20 – Oct 23, 2026". */
fun formatEventDateRangeLong(startMillis: Long, endMillis: Long?): String {
    val start = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    if (endMillis == null) return formatEventDateLong(startMillis)
    val end = Instant.fromEpochMilliseconds(endMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    if (end == start) return formatEventDateLong(startMillis)
    return if (end.year == start.year) {
        "${monthName(start)} ${start.dayOfMonth} – ${monthName(end)} ${end.dayOfMonth}, ${end.year}"
    } else {
        "${monthName(start)} ${start.dayOfMonth}, ${start.year} – ${monthName(end)} ${end.dayOfMonth}, ${end.year}"
    }
}

private fun monthName(date: LocalDate): String =
    date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
