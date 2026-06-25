package org.myfastingapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.myfastingapp.app.backup.BackupCodec
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.UserSettings
import org.myfastingapp.app.domain.WeightEntry

class BackupCodecTest {
    private val codec = BackupCodec()

    @Test
    fun jsonRoundTripPreservesSettingsAndSessions() {
        val settings = UserSettings(remindersEnabled = true, reminderLeadMinutes = 20)
        val sessions = listOf(session(planName = "16:8"))
        val weights = listOf(weight())

        val json = codec.encode(settings, sessions, weights, exportedAtEpochMillis = 100L)
        val decoded = codec.decode(json)

        assertEquals(settings, decoded.settings)
        assertEquals(1, decoded.sessions.size)
        assertEquals("16:8", decoded.sessions.first().planName)
        assertEquals(1, decoded.weights.size)
        assertEquals(82.0, decoded.weights.first().weightKg, 0.001)
    }

    @Test
    fun csvEscapesPlanNames() {
        val csv = codec.encodeCsv(listOf(session(planName = "Custom, long")))

        assertTrue(csv.contains("\"Custom, long\""))
        assertTrue(csv.lines().first().contains("duration_seconds"))
    }

    @Test
    fun rejectsOlderBackupSchemasDuringDevelopment() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.decode("""{"schemaVersion":2,"exportedAtEpochMillis":100}""")
        }
    }

    @Test
    fun stressBackupRoundTripHandlesLargeLocalHistory() {
        val day = 24L * 60L * 60L * 1_000L
        val sessions = (0 until 400).map { index ->
            val end = 10_000_000_000L + index * day
            session(
                planName = "Stress ${index % 5}",
                id = index.toLong() + 1L,
                start = end - (13L + index % 10) * 60L * 60L * 1_000L,
                end = end,
            )
        }
        val weights = (0 until 365).map { index ->
            weight(
                id = index.toLong() + 1L,
                weightKg = 92.0 - index * 0.02,
                recordedEpochMillis = 9_000_000_000L + index * day,
            )
        }

        val json = codec.encode(UserSettings(targetWeightKg = 78.0), sessions, weights, exportedAtEpochMillis = 123L)
        val decoded = codec.decode(json)

        assertEquals(400, decoded.sessions.size)
        assertEquals(365, decoded.weights.size)
        assertEquals(78.0, decoded.settings.targetWeightKg!!, 0.001)
        assertTrue(json.length > 100_000)
    }

    private fun session(
        planName: String,
        id: Long = 7,
        start: Long = 1_000L,
        end: Long = 1_000L + 960 * 60_000L,
    ): FastSession {
        return FastSession(
            id = id,
            planId = "custom",
            planName = planName,
            targetSeconds = 960L * 60L,
            startEpochMillis = start,
            endEpochMillis = end,
            createdEpochMillis = start,
            updatedEpochMillis = end,
        )
    }

    private fun weight(
        id: Long = 3,
        weightKg: Double = 82.0,
        recordedEpochMillis: Long = 3_000L,
    ): WeightEntry {
        return WeightEntry(
            id = id,
            weightKg = weightKg,
            recordedEpochMillis = recordedEpochMillis,
            createdEpochMillis = recordedEpochMillis,
        )
    }
}
