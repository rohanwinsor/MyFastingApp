package org.myfastingapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.myfastingapp.app.domain.TimerMath

class TimerMathTest {
    @Test
    fun progressReportsBeyondTarget() {
        val progress = TimerMath.progress(
            startEpochMillis = 1_000L,
            targetSeconds = 60 * 60L,
            nowEpochMillis = 1_000L + 75 * 60_000L,
        )

        assertEquals(75 * 60_000L, progress.elapsedMillis)
        assertEquals(0L, progress.remainingMillis)
        assertEquals(125, progress.progressPercent)
        assertTrue(progress.targetReached)
    }

    @Test
    fun progressReportsRemainingBeforeTarget() {
        val progress = TimerMath.progress(
            startEpochMillis = 10_000L,
            targetSeconds = 120 * 60L,
            nowEpochMillis = 10_000L + 30 * 60_000L,
        )

        assertEquals(25, progress.progressPercent)
        assertEquals(90 * 60_000L, progress.remainingMillis)
        assertFalse(progress.targetReached)
    }
}
