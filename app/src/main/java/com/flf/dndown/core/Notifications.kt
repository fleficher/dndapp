package com.flf.dndown.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.flf.dndown.R

private const val CHANNEL_ID = "dnd_service_channel"

fun createNotification(context: Context): Notification {
    return NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(context.getString(R.string.service_notification_title))
        .setContentText(context.getString(R.string.service_notification_text))
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()
}

fun createNotificationChannel(context: Context) {
    val serviceChannel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.service_notification_channel_name),
        NotificationManager.IMPORTANCE_LOW
    )

    val notificationManager = context.notificationManager
    notificationManager.createNotificationChannel(serviceChannel)
}
