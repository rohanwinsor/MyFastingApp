package org.myfastingapp.app.domain

data class FastingPhase(
    val title: String,
    val windowLabel: String,
    val body: String,
    val colorArgb: Int,
)

object FastingPhases {
    fun forElapsed(elapsedMillis: Long): FastingPhase {
        val elapsedHours = elapsedMillis / 3_600_000.0
        return when {
            elapsedHours < 4.0 -> FastingPhase(
                title = "Food energy",
                windowLabel = "0-4h",
                body = "Using your last meal.",
                colorArgb = 0xFF2D9CDB.toInt(),
            )
            elapsedHours < 12.0 -> FastingPhase(
                title = "Stored glucose",
                windowLabel = "4-12h",
                body = "Tapping stored fuel.",
                colorArgb = 0xFF3230B8.toInt(),
            )
            elapsedHours < 18.0 -> FastingPhase(
                title = "Fat switch",
                windowLabel = "12-18h",
                body = "Fat use is rising.",
                colorArgb = 0xFF7B3FC8.toInt(),
            )
            elapsedHours < 24.0 -> FastingPhase(
                title = "Fat burning",
                windowLabel = "18-24h",
                body = "More energy from fat.",
                colorArgb = 0xFFE58B2A.toInt(),
            )
            else -> FastingPhase(
                title = "Extended fast",
                windowLabel = "24h+",
                body = "Past your target.",
                colorArgb = 0xFFC44536.toInt(),
            )
        }
    }
}
