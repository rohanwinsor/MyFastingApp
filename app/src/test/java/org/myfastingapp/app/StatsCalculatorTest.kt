package org.myfastingapp.app

import org.junit.Assert.assertEquals
import org.junit.Test
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.StatsCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatsCalculatorTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun calculatesTotalsAndStreakAcrossLocalDates() {
        val sessions = listOf(
            completed(day = 18, hours = 16, id = 1),
            completed(day = 19, hours = 17, id = 2),
            completed(day = 20, hours = 18, id = 3),
        )

        val stats = StatsCalculator.calculate(
            sessions = sessions,
            zoneId = zone,
            today = LocalDate.of(2026, 6, 21),
        )

        assertEquals(3, stats.completedCount)
        assertEquals(51 * 60L * 60L, stats.totalSeconds)
        assertEquals(17 * 60L * 60L, stats.averageSeconds)
        assertEquals(18 * 60L * 60L, stats.longestSeconds)
        assertEquals(3, stats.currentStreakDays)
        assertEquals(100, stats.targetHitRatePercent)
    }

    @Test
    fun streakBreaksOnMissingLocalDay() {
        val sessions = listOf(
            completed(day = 17, hours = 16, id = 1),
            completed(day = 20, hours = 16, id = 2),
        )

        val stats = StatsCalculator.calculate(
            sessions = sessions,
            zoneId = zone,
            today = LocalDate.of(2026, 6, 21),
        )

        assertEquals(1, stats.currentStreakDays)
    }

    private fun completed(day: Int, hours: Int, id: Long): FastSession {
        val start = LocalDateTime.of(2026, 6, day, 4, 0).atZone(zone).toInstant().toEpochMilli()
        val end = start + hours * 60 * 60_000L
        return FastSession(
            id = id,
            planId = "16_8",
            planName = "16:8",
            targetSeconds = 16 * 60L * 60L,
            startEpochMillis = start,
            endEpochMillis = end,
            createdEpochMillis = start,
            updatedEpochMillis = end,
        )
    }
}
