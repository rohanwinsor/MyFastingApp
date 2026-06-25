package org.myfastingapp.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.myfastingapp.app.MainActivity
import org.myfastingapp.app.MyFastingAppApplication
import org.myfastingapp.app.R
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.FastingPhases
import org.myfastingapp.app.domain.TimerMath
import org.myfastingapp.app.domain.UserSettings

class MyFastingAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        renderAsync(context, manager, appWidgetIds) {
            pending.finish()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_START_DEFAULT, ACTION_END_FAST -> {
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as MyFastingAppApplication
                        val repository = app.container.repository
                        val settings = app.container.settingsStore.settings.first()
                        when (intent.action) {
                            ACTION_START_DEFAULT -> {
                                if (repository.activeSession.first() == null) {
                                    repository.startFast(settings.defaultPlan)
                                }
                            }
                            ACTION_END_FAST -> repository.endActiveFast()
                        }
                        repository.repairActivePlanTarget()
                        val active = repository.activeSession.first()
                        app.container.reminderScheduler.schedule(active, settings)
                        updateWidgets(context)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun renderAsync(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray,
        onDone: () -> Unit = {},
    ) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as MyFastingAppApplication
                app.container.repository.repairActivePlanTarget()
                val active = app.container.repository.activeSession.first()
                val settings = app.container.settingsStore.settings.first()
                appWidgetIds.forEach { id ->
                    manager.updateAppWidget(id, buildViews(context, active, settings))
                }
            } finally {
                onDone()
            }
        }
    }

    private fun buildViews(context: Context, active: FastSession?, settings: UserSettings): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_myfastingapp)
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_start, actionIntent(context, ACTION_START_DEFAULT, 1001))
        views.setOnClickPendingIntent(R.id.widget_end, actionIntent(context, ACTION_END_FAST, 1002))

        if (active == null) {
            views.setViewVisibility(R.id.widget_idle_panel, View.VISIBLE)
            views.setViewVisibility(R.id.widget_active_panel, View.GONE)
            views.setTextViewText(
                R.id.widget_idle_detail,
                "Default ${settings.defaultPlan.name} - ${TimerMath.formatMinutes(settings.defaultPlan.fastingMinutes)}",
            )
            return views
        }

        val now = System.currentTimeMillis()
        val progress = TimerMath.progress(active.startEpochMillis, active.targetSeconds, now)
        val elapsedRealtimeBase = SystemClock.elapsedRealtime() - progress.elapsedMillis
        val phase = FastingPhases.forElapsed(progress.elapsedMillis)

        views.setViewVisibility(R.id.widget_idle_panel, View.GONE)
        views.setViewVisibility(R.id.widget_active_panel, View.VISIBLE)
        views.setImageViewBitmap(R.id.widget_ring, WidgetProgressRenderer.render(context, progress.progressFraction, phase.colorArgb))
        views.setTextViewText(R.id.widget_phase, phase.title)
        views.setTextViewText(R.id.widget_status, TimerMath.formatProgressPercent(progress.progressFraction))
        views.setChronometer(R.id.widget_time, elapsedRealtimeBase, null, true)
        views.setChronometerCountDown(R.id.widget_time, false)
        views.setTextViewText(
            R.id.widget_remaining,
            if (progress.targetReached) {
                "Extended by ${TimerMath.formatDuration(progress.elapsedMillis - progress.targetMillis)}"
            } else {
                "${TimerMath.formatDuration(progress.remainingMillis)} remaining"
            },
        )
        views.setTextViewText(
            R.id.widget_detail,
            "${phase.body} ${active.planName} target - ${TimerMath.formatMinutes((active.targetSeconds / 60L).toInt())}",
        )
        return views
    }

    fun buildViewsForTest(context: Context, active: FastSession?, settings: UserSettings): RemoteViews {
        return buildViews(context, active, settings)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MyFastingAppWidgetProvider::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_START_DEFAULT = "org.myfastingapp.app.widget.START_DEFAULT"
        const val ACTION_END_FAST = "org.myfastingapp.app.widget.END_FAST"

        fun updateWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MyFastingAppWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                MyFastingAppWidgetProvider().renderAsync(context, manager, ids)
            }
        }
    }
}
