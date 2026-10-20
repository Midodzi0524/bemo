package com.example.bemo.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.bemo.model.AlertThreshold
import com.example.bemo.model.BudgetAlert

object NotificationHelper {
    const val CHANNEL_ID = "bemo_budget_alerts"
    const val CHANNEL_NAME = "Budget & Expense Alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val descriptionText = "Alerts when spending reaches 80% or 100% of category budget limits"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun postBudgetNotification(context: Context, alert: BudgetAlert) {
        if (!hasNotificationPermission(context)) {
            return
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_navigate_subscreen", "NOTIFICATIONS")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = when (alert.threshold) {
            AlertThreshold.LIMIT_100 -> android.R.drawable.ic_dialog_alert
            AlertThreshold.WARNING_80 -> android.R.drawable.stat_notify_more
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(alert.message)
                    .setBigContentTitle(alert.title)
                    .setSummaryText(if (alert.threshold == AlertThreshold.LIMIT_100) "100% Budget Reached" else "80% Warning")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFFD85A30.toInt())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(alert.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Handled when permission is missing
        }
    }
}
