package org.myfastingapp.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.myfastingapp.app.MyFastingAppApplication
import org.myfastingapp.app.widget.MyFastingAppWidgetProvider

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FAST_REMINDER,
            ACTION_FAST_MILESTONE,
            ACTION_FAST_NOTIFICATION_UPDATE,
            ACTION_NOTIFICATION_START,
            ACTION_NOTIFICATION_END,
            -> handleAsync(context, intent)
        }
    }

    private fun handleAsync(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as MyFastingAppApplication
                val repository = app.container.repository
                val settingsStore = app.container.settingsStore
                val notificationController = FastNotificationController(context)

                when (intent.action) {
                    ACTION_NOTIFICATION_START -> {
                        val settings = settingsStore.settings.first()
                        if (repository.activeSession.first() == null) {
                            repository.startFast(settings.defaultPlan)
                        }
                    }
                    ACTION_NOTIFICATION_END -> repository.endActiveFast()
                    ACTION_FAST_MILESTONE -> {
                        val active = repository.activeSession.first()
                        if (active != null && active.id == intent.getLongExtra(EXTRA_SESSION_ID, -1L)) {
                            notificationController.showMilestone(active, intent.getIntExtra(EXTRA_MILESTONE_PERCENT, 0))
                        }
                    }
                    ACTION_FAST_REMINDER -> {
                        val active = repository.activeSession.first()
                        notificationController.showTargetReminder(active)
                    }
                    ACTION_FAST_NOTIFICATION_UPDATE -> Unit
                }

                repository.repairActivePlanTarget()
                val active = repository.activeSession.first()
                val settings = settingsStore.settings.first()
                app.container.reminderScheduler.schedule(active, settings)
                MyFastingAppWidgetProvider.updateWidgets(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FAST_REMINDER = "org.myfastingapp.app.notify.FAST_REMINDER"
        const val ACTION_FAST_MILESTONE = "org.myfastingapp.app.notify.FAST_MILESTONE"
        const val ACTION_FAST_NOTIFICATION_UPDATE = "org.myfastingapp.app.notify.FAST_NOTIFICATION_UPDATE"
        const val ACTION_NOTIFICATION_START = "org.myfastingapp.app.notify.START_FAST"
        const val ACTION_NOTIFICATION_END = "org.myfastingapp.app.notify.END_FAST"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_MILESTONE_PERCENT = "milestone_percent"
    }
}
