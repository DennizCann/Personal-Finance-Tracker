package com.denizcan.personalfinancetracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.denizcan.personalfinancetracker.R
import java.time.LocalDateTime

class SmartNotificationManager(private val context: Context) {
    companion object {
        const val CHANNEL_ID_EXPENSES = "expenses_channel"
        const val CHANNEL_ID_BUDGET = "budget_channel"
        const val CHANNEL_ID_GOALS = "goals_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_EXPENSES,
                    "Harcama Bildirimleri",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    CHANNEL_ID_BUDGET,
                    "Bütçe Bildirimleri",
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    CHANNEL_ID_GOALS,
                    "Hedef Bildirimleri",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            channels.forEach { channel ->
                channel.description = "Finansal takip bildirimleri"
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun sendExpenseWarning(currentExpense: Double, limit: Double) {
        val percentage = (currentExpense / limit) * 100
        when {
            percentage >= 90 -> sendNotification(
                "Harcama Limiti Uyarısı",
                "Aylık harcama limitinizin %90'ına ulaştınız!",
                CHANNEL_ID_EXPENSES,
                1
            )
            percentage >= 75 -> sendNotification(
                "Harcama Bildirimi",
                "Aylık harcama limitinizin %75'ini kullandınız.",
                CHANNEL_ID_EXPENSES,
                2
            )
        }
    }

    fun sendBudgetReminder() {
        val now = LocalDateTime.now()
        if (now.dayOfMonth == 25) {
            sendNotification(
                "Bütçe Planlaması",
                "Gelecek ay için bütçenizi planlamayı unutmayın!",
                CHANNEL_ID_BUDGET,
                3
            )
        }
    }

    fun sendGoalProgress(goalName: String, progress: Int) {
        if (progress in listOf(50, 75, 90, 100)) {
            sendNotification(
                "Hedef İlerlemesi",
                "$goalName hedefinizde %$progress ilerleme sağladınız!",
                CHANNEL_ID_GOALS,
                4
            )
        }
    }

    private fun sendNotification(
        title: String,
        content: String,
        channelId: String,
        notificationId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
} 