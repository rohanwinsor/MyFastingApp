package org.myfastingapp.app.domain

data class FastSession(
    val id: Long,
    val planId: String,
    val planName: String,
    val targetSeconds: Long,
    val startEpochMillis: Long,
    val endEpochMillis: Long?,
    val createdEpochMillis: Long,
    val updatedEpochMillis: Long,
) {
    val isActive: Boolean = endEpochMillis == null
    val targetMinutes: Int = (targetSeconds / 60L).toInt()
    val targetMillis: Long = targetSeconds.coerceAtLeast(1L) * 1_000L

    fun durationMillis(nowMillis: Long): Long {
        val effectiveEnd = endEpochMillis ?: nowMillis
        return (effectiveEnd - startEpochMillis).coerceAtLeast(0L)
    }
}
