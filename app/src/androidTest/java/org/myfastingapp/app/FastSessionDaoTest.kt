package org.myfastingapp.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.myfastingapp.app.backup.BackupCodec
import org.myfastingapp.app.data.FastRepository
import org.myfastingapp.app.data.FastSessionEntity
import org.myfastingapp.app.data.MyFastingAppDatabase
import org.myfastingapp.app.data.SettingsStore
import org.myfastingapp.app.domain.FastPlans

@RunWith(AndroidJUnit4::class)
class FastSessionDaoTest {
    private lateinit var database: MyFastingAppDatabase
    private lateinit var repository: FastRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MyFastingAppDatabase::class.java,
        ).build()
        repository = FastRepository(
            database.fastSessionDao(),
            database.weightEntryDao(),
            SettingsStore(context),
            BackupCodec(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun tracksOnlyActiveSession() = runTest {
        val dao = database.fastSessionDao()
        val id = dao.insert(entity(end = null))

        assertEquals(id, dao.getActive()?.id)
        assertEquals(1, dao.activeCount())

        dao.update(dao.getActive()!!.copy(endEpochMillis = 2_000L))

        assertNull(dao.getActive())
        assertEquals(0, dao.activeCount())
    }

    @Test
    fun detectsOverlappingCompletedSessions() = runTest {
        val dao = database.fastSessionDao()
        dao.insert(entity(start = 1_000L, end = 10_000L))

        assertTrue(dao.overlappingCount(5_000L, 12_000L) > 0)
        assertEquals(0, dao.overlappingCount(10_000L, 12_000L))
    }

    @Test
    fun editingActiveFastWithSameTargetKeepsPlan() = runTest {
        val plan = FastPlans.resolve(FastPlans.DEFAULT_ID, FastPlans.DEFAULT_CUSTOM_MINUTES)
        val now = 1_000_000_000L
        val id = repository.startFast(plan, now)
        val shiftedStart = now - 2L * 60L * 60L * 1_000L
        val shiftedEnd = shiftedStart + plan.fastingMinutes * 60_000L

        repository.editFast(id, shiftedStart, shiftedEnd, now)

        val active = database.fastSessionDao().getActive()!!
        assertEquals(plan.id, active.planId)
        assertEquals(plan.name, active.planName)
        assertEquals(plan.fastingMinutes * 60L, active.targetSeconds)
        assertEquals(shiftedStart, active.startEpochMillis)
        assertNull(active.endEpochMillis)
    }

    @Test
    fun editingActiveFastWithDifferentTargetBecomesCustom() = runTest {
        val plan = FastPlans.resolve(FastPlans.DEFAULT_ID, FastPlans.DEFAULT_CUSTOM_MINUTES)
        val now = 1_000_000_000L
        val id = repository.startFast(plan, now)
        val shiftedStart = now - 2L * 60L * 60L * 1_000L
        val shiftedEnd = shiftedStart + 17L * 60L * 60L * 1_000L

        repository.editFast(id, shiftedStart, shiftedEnd, now)

        val active = database.fastSessionDao().getActive()!!
        assertEquals(FastPlans.CUSTOM_ID, active.planId)
        assertEquals("Custom", active.planName)
        assertEquals(17L * 60L * 60L, active.targetSeconds)
        assertNull(active.endEpochMillis)
    }

    @Test
    fun repairActiveBuiltInPlanRestoresCanonicalTarget() = runTest {
        val dao = database.fastSessionDao()
        val now = 1_000_000_000L
        dao.insert(
            entity(
                start = now - 10L * 60L * 60L * 1_000L,
                end = null,
            ).copy(
                planId = FastPlans.DEFAULT_ID,
                planName = "16:8",
                targetSeconds = 15L * 60L * 60L,
            ),
        )

        repository.repairActivePlanTarget(now)

        val active = dao.getActive()!!
        assertEquals(FastPlans.DEFAULT_ID, active.planId)
        assertEquals("16:8", active.planName)
        assertEquals(16L * 60L * 60L, active.targetSeconds)
    }

    @Test
    fun editingMismatchedBuiltInActiveFastWithCanonicalTargetKeepsPlan() = runTest {
        val dao = database.fastSessionDao()
        val now = 1_000_000_000L
        val start = now - 4L * 60L * 60L * 1_000L
        val id = dao.insert(
            entity(start = start, end = null).copy(
                planId = FastPlans.DEFAULT_ID,
                planName = "16:8",
                targetSeconds = 15L * 60L * 60L,
            ),
        )
        val shiftedStart = now - 3L * 60L * 60L * 1_000L
        val shiftedEnd = shiftedStart + 16L * 60L * 60L * 1_000L

        repository.editFast(id, shiftedStart, shiftedEnd, now)

        val active = dao.getActive()!!
        assertEquals(FastPlans.DEFAULT_ID, active.planId)
        assertEquals("16:8", active.planName)
        assertEquals(16L * 60L * 60L, active.targetSeconds)
        assertEquals(shiftedStart, active.startEpochMillis)
        assertNull(active.endEpochMillis)
    }

    @Test
    fun editingFastCannotMoveStartIntoFuture() = runTest {
        val plan = FastPlans.resolve(FastPlans.DEFAULT_ID, FastPlans.DEFAULT_CUSTOM_MINUTES)
        val now = 1_000_000_000L
        val id = repository.startFast(plan, now)
        val futureStart = now + 60_000L
        val futureEnd = futureStart + plan.fastingMinutes * 60_000L

        try {
            repository.editFast(id, futureStart, futureEnd, now)
        } catch (expected: IllegalArgumentException) {
            assertEquals("Start time cannot be in the future.", expected.message)
            return@runTest
        }

        throw AssertionError("Expected future start edit to be rejected.")
    }

    private fun entity(start: Long = 1_000L, end: Long? = 4_000L): FastSessionEntity {
        return FastSessionEntity(
            planId = "16_8",
            planName = "16:8",
            targetSeconds = 960L * 60L,
            startEpochMillis = start,
            endEpochMillis = end,
            createdEpochMillis = start,
            updatedEpochMillis = end ?: start,
        )
    }
}
