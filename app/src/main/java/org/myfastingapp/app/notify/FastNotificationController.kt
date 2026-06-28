package org.myfastingapp.app.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.myfastingapp.app.MainActivity
import org.myfastingapp.app.MyFastingAppApplication
import org.myfastingapp.app.R
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.FastingPhases
import org.myfastingapp.app.domain.TimerMath
import org.myfastingapp.app.domain.UserSettings

class FastNotificationController(private val context: Context) {
    @SuppressLint("MissingPermission")
    fun showOngoing(session: FastSession?, settings: UserSettings, nowEpochMillis: Long = System.currentTimeMillis()) {
        if (!canPostNotifications()) return

        val builder = NotificationCompat.Builder(context, MyFastingAppApplication.FASTING_STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_myfastingapp)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (session == null) {
            val plan = settings.defaultPlan
            builder
                .setContentTitle("MyFastingApp is ready")
                .setContentText("Start ${plan.name} - ${TimerMath.formatMinutes(plan.fastingMinutes)}")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Start ${plan.name} when your next fast begins. Everything stays on this device."))
                .setShowWhen(false)
                .addAction(R.drawable.ic_myfastingapp, "Start fast", actionIntent(ReminderReceiver.ACTION_NOTIFICATION_START, REQUEST_START))
        } else {
            val progress = TimerMath.progress(session.startEpochMillis, session.targetSeconds, nowEpochMillis)
            val phase = FastingPhases.forElapsed(progress.elapsedMillis)
            val remainingText = if (progress.targetReached) {
                "Extended by ${TimerMath.formatDuration(progress.elapsedMillis - progress.targetMillis)}"
            } else {
                "${TimerMath.formatDuration(progress.remainingMillis)} remaining"
            }
            builder
                .setContentTitle("${TimerMath.formatProgressPercent(progress.progressFraction)} - ${phase.title}")
                .setContentText("${TimerMath.formatDuration(progress.elapsedMillis)} elapsed - $remainingText")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "${session.planName}: ${TimerMath.formatDuration(progress.elapsedMillis)} elapsed, $remainingText. ${phase.body}",
                    ),
                )
                .setProgress(100, progress.progressPercent.coerceAtMost(100), false)
                .setColor(phase.colorArgb)
                .setShowWhen(false)
                .addAction(R.drawable.ic_myfastingapp, "End fast", actionIntent(ReminderReceiver.ACTION_NOTIFICATION_END, REQUEST_END))
        }

        NotificationManagerCompat.from(context).notify(ONGOING_NOTIFICATION_ID, builder.build())
    }

    @SuppressLint("MissingPermission")
    fun showMilestone(session: FastSession, percent: Int, nowEpochMillis: Long = System.currentTimeMillis()) {
        if (!canPostNotifications()) return
        val progress = TimerMath.progress(session.startEpochMillis, session.targetSeconds, nowEpochMillis)
        val phase = FastingPhases.forElapsed(progress.elapsedMillis)
        val notification = NotificationCompat.Builder(context, MyFastingAppApplication.FASTING_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_myfastingapp)
            .setContentTitle("$percent% fast progress")
            .setContentText("${phase.title}: ${TimerMath.formatDuration(progress.elapsedMillis)} elapsed.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${session.planName} is $percent% complete. ${phase.body}"))
            .setColor(phase.colorArgb)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setLocalOnly(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(MILESTONE_NOTIFICATION_BASE_ID + percent, notification)
    }

    @SuppressLint("MissingPermission")
    fun showTargetReminder(session: FastSession?, nowEpochMillis: Long = System.currentTimeMillis()) {
        if (!canPostNotifications()) return
        val text = session?.let {
            val progress = TimerMath.progress(it.startEpochMillis, it.targetSeconds, nowEpochMillis)
            "${TimerMath.formatProgressPercent(progress.progressFraction)} complete - target is almost here."
        } ?: "Your fasting target is almost here."
        val notification = NotificationCompat.Builder(context, MyFastingAppApplication.FASTING_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_myfastingapp)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setLocalOnly(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(FAST_REMINDER_NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ONGOING_NOTIFICATION_ID = 4001
        const val FAST_REMINDER_NOTIFICATION_ID = 4101
        private const val MILESTONE_NOTIFICATION_BASE_ID = 4200
        private const val REQUEST_START = 4301
        private const val REQUEST_END = 4302
    }
}
