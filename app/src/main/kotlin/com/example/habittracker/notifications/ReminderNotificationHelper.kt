package com.example.habittracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.habittracker.R
import com.example.habittracker.MainActivity

object ReminderNotificationHelper {
    const val CHANNEL_ID = "habit_reminders"
    private const val CHANNEL_NAME = "Habit Reminders"
    private const val GROUP_KEY = "habit_reminder_group"
    private const val SUMMARY_NOTIFICATION_ID = 100_001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
            Log.d(TAG, "Created notification channel $CHANNEL_ID")
        }
    }

    fun showHabitReminder(context: Context, habitId: Long, habitName: String) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            habitId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDoneIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_MARK_DONE
            putExtra(ReminderActionReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderActionReceiver.EXTRA_HABIT_NAME, habitName)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 31L + 7L).toInt(),
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze5PendingIntent = createSnoozePendingIntent(
            context = context,
            habitId = habitId,
            habitName = habitName,
            minutes = 5,
            requestCodeSalt = 17L
        )
        val snooze15PendingIntent = createSnoozePendingIntent(
            context = context,
            habitId = habitId,
            habitName = habitName,
            minutes = 15,
            requestCodeSalt = 19L
        )
        val snooze30PendingIntent = createSnoozePendingIntent(
            context = context,
            habitId = habitId,
            habitName = habitName,
            minutes = 30,
            requestCodeSalt = 23L
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_text, habitName))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setGroup(GROUP_KEY)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.reminder_notification_text, habitName))
            )
            .addAction(
                0,
                context.getString(R.string.reminder_action_mark_done),
                markDonePendingIntent
            )
            .addAction(
                0,
                context.getString(R.string.reminder_action_snooze_5m),
                snooze5PendingIntent
            )
            .addAction(
                0,
                context.getString(R.string.reminder_action_snooze_15m),
                snooze15PendingIntent
            )
            .addAction(
                0,
                context.getString(R.string.reminder_action_snooze_30m),
                snooze30PendingIntent
            )
            .build()

        val manager = NotificationManagerCompat.from(context)
        manager.notify(notificationIdForHabit(habitId), notification)
        postOrUpdateGroupSummary(context)

        Log.d(TAG, "Notification posted for habitId=$habitId name=$habitName")
    }

    fun dismissHabitReminder(context: Context, habitId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationIdForHabit(habitId))
        postOrUpdateGroupSummary(context)
    }

    fun postOrUpdateGroupSummary(context: Context) {
        createNotificationChannel(context)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val childCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.activeNotifications.count { statusBarNotification ->
                val id = statusBarNotification.id
                val notification = statusBarNotification.notification
                id != SUMMARY_NOTIFICATION_ID && notification.group == GROUP_KEY
            }
        } else {
            1
        }

        if (childCount <= 0) {
            NotificationManagerCompat.from(context).cancel(SUMMARY_NOTIFICATION_ID)
            return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            SUMMARY_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(context.getString(R.string.reminder_group_title))
            .setContentText(context.getString(R.string.reminder_group_text, childCount))
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText(context.getString(R.string.reminder_group_summary, childCount))
            )
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, summary)
    }

    private fun notificationIdForHabit(habitId: Long): Int = (habitId % Int.MAX_VALUE).toInt()

    private fun createSnoozePendingIntent(
        context: Context,
        habitId: Long,
        habitName: String,
        minutes: Int,
        requestCodeSalt: Long
    ): PendingIntent {
        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE
            putExtra(ReminderActionReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderActionReceiver.EXTRA_HABIT_NAME, habitName)
            putExtra(ReminderActionReceiver.EXTRA_SNOOZE_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(
            context,
            (habitId * 31L + requestCodeSalt).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val TAG = "ReminderNotification"
}
