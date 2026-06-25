package org.myfastingapp.app

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.myfastingapp.app.domain.WeightEntry
import org.myfastingapp.app.domain.WeightTrendCalculator

class WeightTrendCalculatorTest {
    @Test
    fun predictsTargetWhenWeightsTrendTowardGoal() {
        val day = 24L * 60L * 60L * 1_000L
        val weights = listOf(
            weight(84.0, 0L),
            weight(83.0, day),
            weight(82.0, 2L * day),
        )

        val trend = WeightTrendCalculator.calculate(weights, targetWeightKg = 80.0)

        assertTrue(trend.changeKg < 0.0)
        assertTrue(trend.slopeKgPerDay < 0.0)
        assertNotNull(trend.predictedTargetEpochMillis)
    }

    @Test
    fun skipsPredictionWhenTrendMovesAwayFromGoal() {
        val day = 24L * 60L * 60L * 1_000L
        val weights = listOf(
            weight(82.0, 0L),
            weight(83.0, day),
            weight(84.0, 2L * day),
        )

        val trend = WeightTrendCalculator.calculate(weights, targetWeightKg = 80.0)

        assertTrue(trend.slopeKgPerDay > 0.0)
        assertTrue(trend.predictedTargetEpochMillis == null)
    }

    private fun weight(weightKg: Double, recordedEpochMillis: Long): WeightEntry {
        return WeightEntry(
            id = recordedEpochMillis,
            weightKg = weightKg,
            recordedEpochMillis = recordedEpochMillis + 1_000L,
            createdEpochMillis = recordedEpochMillis + 1_000L,
        )
    }
}
