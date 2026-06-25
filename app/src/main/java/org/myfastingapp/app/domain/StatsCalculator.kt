package org.myfastingapp.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

data class FastStats(
    val completedCount: Int = 0,
    val totalSeconds: Long = 0,
    val averageSeconds: Long = 0,
    val longestSeconds: Long = 0,
    val currentStreakDays: Int = 0,
    val targetHitRatePercent: Int = 0,
) {
    val totalMinutes: Long = totalSeconds / 60L
    val averageMinutes: Long = averageSeconds / 60L
    val longestMinutes: Long = longestSeconds / 60L
}

object StatsCalculator {
    fun calculate(
        sessions: List<FastSession>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zoneId),
    ): FastStats {
        val completed = sessions.filter { it.endEpochMillis != null }
        if (completed.isEmpty()) return FastStats()

        val durations = completed.map { ((it.endEpochMillis!! - it.startEpochMillis) / 1_000L).coerceAtLeast(0L) }
        val total = durations.sum()
        val hits = completed.count { session ->
            ((session.endEpochMillis!! - session.startEpochMillis) / 1_000L) >= session.targetSeconds
        }

        return FastStats(
            completedCount = completed.size,
            totalSeconds = total,
            averageSeconds = (total.toDouble() / completed.size.toDouble()).roundToLong(),
            longestSeconds = durations.maxOrNull() ?: 0L,
            currentStreakDays = currentStreak(completed, zoneId, today),
            targetHitRatePercent = ((hits.toDouble() / completed.size.toDouble()) * 100).roundToLong().toInt(),
        )
    }

    private fun currentStreak(
        completed: List<FastSession>,
        zoneId: ZoneId,
        today: LocalDate,
    ): Int {
        val dates = completed
            .mapNotNull { it.endEpochMillis }
            .map { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
            .toSet()

        var cursor = if (today in dates) today else today.minusDays(1)
        var count = 0
        while (cursor in dates) {
            count += 1
            cursor = cursor.minusDays(1)
        }
        return count
    }
}
