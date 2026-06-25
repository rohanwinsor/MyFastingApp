package org.myfastingapp.app.domain

import kotlin.math.roundToLong

data class WeightTrend(
    val latestWeightKg: Double? = null,
    val firstWeightKg: Double? = null,
    val changeKg: Double = 0.0,
    val slopeKgPerDay: Double = 0.0,
    val predictedTargetEpochMillis: Long? = null,
    val daysToTarget: Long? = null,
) {
    val hasEnoughData: Boolean = latestWeightKg != null && firstWeightKg != null
}

object WeightTrendCalculator {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L

    fun calculate(weights: List<WeightEntry>, targetWeightKg: Double?): WeightTrend {
        val points = weights
            .sortedBy { it.recordedEpochMillis }
            .distinctBy { it.recordedEpochMillis }
        if (points.isEmpty()) return WeightTrend()

        val first = points.first()
        val latest = points.last()
        val slope = slopeKgPerDay(points)
        val targetDate = targetWeightKg
            ?.let { target -> predictedTargetDate(points, latest, target, slope) }
        val daysToTarget = targetDate
            ?.let { ((it - latest.recordedEpochMillis).toDouble() / DAY_MILLIS.toDouble()).roundToLong().coerceAtLeast(0L) }

        return WeightTrend(
            latestWeightKg = latest.weightKg,
            firstWeightKg = first.weightKg,
            changeKg = latest.weightKg - first.weightKg,
            slopeKgPerDay = slope,
            predictedTargetEpochMillis = targetDate,
            daysToTarget = daysToTarget,
        )
    }

    private fun slopeKgPerDay(points: List<WeightEntry>): Double {
        if (points.size < 2) return 0.0
        val firstMillis = points.first().recordedEpochMillis
        val xs = points.map { (it.recordedEpochMillis - firstMillis).toDouble() / DAY_MILLIS.toDouble() }
        val ys = points.map { it.weightKg }
        val meanX = xs.average()
        val meanY = ys.average()
        val numerator = xs.zip(ys).sumOf { (x, y) -> (x - meanX) * (y - meanY) }
        val denominator = xs.sumOf { x -> (x - meanX) * (x - meanX) }
        if (denominator == 0.0) return 0.0
        return numerator / denominator
    }

    private fun predictedTargetDate(
        points: List<WeightEntry>,
        latest: WeightEntry,
        targetWeightKg: Double,
        slopeKgPerDay: Double,
    ): Long? {
        if (points.size < 2 || slopeKgPerDay == 0.0) return null
        val latestWeight = latest.weightKg
        val movingTowardLowerTarget = targetWeightKg < latestWeight && slopeKgPerDay < 0.0
        val movingTowardHigherTarget = targetWeightKg > latestWeight && slopeKgPerDay > 0.0
        if (!movingTowardLowerTarget && !movingTowardHigherTarget) return null

        val days = (targetWeightKg - latestWeight) / slopeKgPerDay
        if (!days.isFinite() || days < 0.0) return null
        return latest.recordedEpochMillis + (days * DAY_MILLIS).roundToLong()
    }
}
