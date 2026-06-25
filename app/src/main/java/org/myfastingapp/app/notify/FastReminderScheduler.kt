package org.myfastingapp.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.UserSettings

class FastReminderScheduler(private val context: Context) {
    private val alarmManager: AlarmManager?
        get() = context.getSystemService()

    private val notificationController = FastNotificationController(context)

    fun schedule(session: FastSession?, settings: UserSettings, nowEpochMillis: Long = System.currentTimeMillis()) {
        cancelScheduledAlarms()
        notificationController.showOngoing(session, settings, nowEpochMillis)
        if (session == null) return

        scheduleStatusUpdates(session, nowEpochMillis)
        MILESTONES.forEach { milestone ->
            val triggerAt = session.startEpochMillis + ((session.targetSeconds * 1_000L * milestone) / 100L)
            if (triggerAt > nowEpochMillis) {
                scheduleAlarm(
                    action = ReminderReceiver.ACTION_FAST_MILESTONE,
                    requestCode = REQUEST_MILESTONE_BASE + milestone,
                    triggerAt = triggerAt,
                    sessionId = session.id,
                    milestone = milestone,
                )
            }
        }
        if (settings.remindersEnabled) {
            scheduleTargetReminder(session, settings, nowEpochMillis)
        }
    }

    fun cancel() {
        cancelScheduledAlarms()
    }

    private fun scheduleTargetReminder(session: FastSession, settings: UserSettings, nowEpochMillis: Long) {
        val targetEnd = session.startEpochMillis + session.targetSeconds * 1_000L
        val triggerAt = targetEnd - settings.reminderLeadMinutes * 60_000L
        if (triggerAt <= nowEpochMillis) return
        scheduleAlarm(
            action = ReminderReceiver.ACTION_FAST_REMINDER,
            requestCode = REQUEST_TARGET_REMINDER,
            triggerAt = triggerAt,
            sessionId = session.id,
        )
    }

    private fun scheduleStatusUpdates(session: FastSession, nowEpochMillis: Long) {
        scheduleAlarm(
            action = ReminderReceiver.ACTION_FAST_NOTIFICATION_UPDATE,
            requestCode = REQUEST_STATUS_REFRESH,
            triggerAt = nowEpochMillis + STATUS_REFRESH_INTERVAL_MILLIS,
            sessionId = session.id,
        )

        val targetEnd = session.startEpochMillis + session.targetSeconds * 1_000L
        if (targetEnd > nowEpochMillis) {
            scheduleAlarm(
                action = ReminderReceiver.ACTION_FAST_NOTIFICATION_UPDATE,
                requestCode = REQUEST_TARGET_UPDATE,
                triggerAt = targetEnd,
                sessionId = session.id,
            )
        }

        PHASE_HOUR_MARKS.forEach { hour ->
            val triggerAt = session.startEpochMillis + hour * 3_600_000L
            if (triggerAt > nowEpochMillis) {
                scheduleAlarm(
                    action = ReminderReceiver.ACTION_FAST_NOTIFICATION_UPDATE,
                    requestCode = REQUEST_PHASE_UPDATE_BASE + hour,
                    triggerAt = triggerAt,
                    sessionId = session.id,
                )
            }
        }
    }

    private fun scheduleAlarm(
        action: String,
        requestCode: Int,
        triggerAt: Long,
        sessionId: Long,
        milestone: Int? = null,
    ) {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderReceiver.EXTRA_SESSION_ID, sessionId)
        if (milestone != null) {
            intent.putExtra(ReminderReceiver.EXTRA_MILESTONE_PERCENT, milestone)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager?.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private fun cancelScheduledAlarms() {
        cancelRequest(REQUEST_TARGET_REMINDER, ReminderReceiver.ACTION_FAST_REMINDER)
        cancelRequest(REQUEST_TARGET_UPDATE, ReminderReceiver.ACTION_FAST_NOTIFICATION_UPDATE)
        cancelRequest(REQUEST_STATUS_REFRESH, ReminderReceiver.ACTION_FAST_NOTIFICATION_UPDATE)
        MILESTONES.forEach { cancelRequest(REQUEST_MILESTONE_BASE + it, ReminderReceiver.ACTION_FAST_MILESTONE) }
        PHASE_HOUR_MARKS.forEach { cancelRequest(REQUEST_PHASE_UPDATE_BASE + it, ReminderReceiver.ACTION_FAST_NOTIFICATION_UPDATE) }
    }

    private fun cancelRequest(requestCode: Int, action: String) {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private companion object {
        val MILESTONES = listOf(25, 50, 75, 90, 95, 100)
        val PHASE_HOUR_MARKS = listOf(4, 12, 18, 24)
        const val REQUEST_TARGET_REMINDER = 3101
        const val REQUEST_TARGET_UPDATE = 3200
        const val REQUEST_STATUS_REFRESH = 3201
        const val REQUEST_MILESTONE_BASE = 3300
        const val REQUEST_PHASE_UPDATE_BASE = 3400
        const val STATUS_REFRESH_INTERVAL_MILLIS = 10 * 60_000L
    }
}
