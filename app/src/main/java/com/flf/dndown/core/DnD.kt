package com.flf.dndown.core

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat

fun Context.setDndMode(enable: Boolean) {
    if (!hasDndPermission()) return
    notificationManager.setInterruptionFilter(targetInterruptionFilter(enable))
}

@NotificationManagerCompat.InterruptionFilter
fun targetInterruptionFilter(isFaceDown: Boolean): Int {
    return if (isFaceDown) {
        NotificationManager.INTERRUPTION_FILTER_PRIORITY
    } else {
        NotificationManager.INTERRUPTION_FILTER_ALL
    }
}

@NotificationManagerCompat.InterruptionFilter
fun Context.currentInterruptionFilter(): Int {
    return notificationManager.currentInterruptionFilter
}
