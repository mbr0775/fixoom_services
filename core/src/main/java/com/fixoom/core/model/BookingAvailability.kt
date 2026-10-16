package com.fixoom.core.model

import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusyTime(
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
) {
    fun overlaps(start: Instant, minutes: Int): Boolean =
        start.isBefore(Instant.parse(endsAt)) &&
            start.plusSeconds(minutes * 60L).isAfter(Instant.parse(startsAt))
}

fun nextUnreservedTime(start: Instant, minutes: Int, times: List<BusyTime>): Instant {
    var next = start
    times
        .sortedBy { Instant.parse(it.startsAt) }
        .forEach {
            if (it.overlaps(next, minutes)) next = Instant.parse(it.endsAt)
        }
    return next
}
