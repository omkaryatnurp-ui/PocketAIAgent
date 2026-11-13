package com.pocketai.agent.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pocketai.agent.MainActivity
import com.pocketai.agent.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_SERVICE_ID = "pocket_ai_service_channel"
        const val CHANNEL_ALERTS_ID = "pocket_ai_alerts_channel"
        const val SERVICE_NOTIFICATION_ID = 1001

        const val ACTION_PAUSE = "com.pocketai.agent.ACTION_PAUSE"
        const val ACTION_RESUME = "com.pocketai.agent.ACTION_RESUME"
        const val ACTION_CANCEL = "com.pocketai.agent.ACTION_CANCEL"
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                context.getString(R.string.channel_service),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_service_desc)
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                context.getString(R.string.channel_task_updates),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_task_updates_desc)
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }

    fun buildForegroundNotification(
        taskTitle: String,
        currentStage: String,
        progressPercent: Int,
        isPaused: Boolean
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause / Resume Action
        val toggleActionIntent = Intent(context, TaskExecutionService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val togglePendingIntent = PendingIntent.getService(
            context,
            1,
            toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel Action
        val cancelActionIntent = Intent(context, TaskExecutionService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            context,
            2,
            cancelActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SERVICE_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pocket AI: $taskTitle")
            .setContentText(if (isPaused) "Paused - $currentStage" else "$currentStage ($progressPercent%)")
            .setContentIntent(openPendingIntent)
            .setProgress(100, progressPercent, progressPercent == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                0,
                if (isPaused) "Resume" else "Pause",
                togglePendingIntent
            )
            .addAction(
                0,
                "Cancel",
                cancelPendingIntent
            )

        return builder.build()
    }

    fun showTaskAlert(title: String, message: String, notificationId: Int = (System.currentTimeMillis() % 10000).toInt()) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (ignored: SecurityException) {
            // Notifications permission not yet granted by user
        }
    }
}
