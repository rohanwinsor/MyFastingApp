package org.myfastingapp.app.domain

import kotlin.math.roundToInt

data class FastProgress(
    val elapsedMillis: Long,
    val targetMillis: Long,
    val remainingMillis: Long,
    val progressFraction: Float,
    val progressPercent: Int,
    val targetReached: Boolean,
)

object TimerMath {
    fun progress(startEpochMillis: Long, targetSeconds: Long, nowEpochMillis: Long): FastProgress {
        val elapsed = (nowEpochMillis - startEpochMillis).coerceAtLeast(0L)
        val target = targetSeconds.coerceAtLeast(1L) * 1_000L
        val remaining = (target - elapsed).coerceAtLeast(0L)
        val fraction = (elapsed.toDouble() / target.toDouble()).toFloat().coerceAtLeast(0f)
        val percent = (fraction * 100f)
            .roundToInt()
            .coerceAtLeast(0)
        return FastProgress(
            elapsedMillis = elapsed,
            targetMillis = target,
            remainingMillis = remaining,
            progressFraction = fraction,
            progressPercent = percent,
            targetReached = elapsed >= target,
        )
    }

    fun formatDuration(millis: Long): String {
        val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "%02d:%02d".format(hours, minutes)
    }

    fun formatDurationWithSeconds(millis: Long): String {
        val totalSeconds = (millis / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
    }

    fun formatProgressPercent(progressFraction: Float): String {
        val percent = (progressFraction.coerceAtLeast(0f) * 100f)
        return when {
            percent == 0f -> "0%"
            percent < 10f -> "%.1f%%".format(percent)
            else -> "${percent.roundToInt()}%"
        }
    }
}
