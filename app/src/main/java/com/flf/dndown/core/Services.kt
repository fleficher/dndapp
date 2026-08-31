package com.flf.dndown.core

import android.app.NotificationManager
import android.content.Context
import android.hardware.SensorManager
import android.os.VibratorManager
import androidx.core.content.getSystemService

val Context.notificationManager : NotificationManager
    get() = requireNotNull(getSystemService<NotificationManager>())

val Context.sensorManager : SensorManager
    get() = requireNotNull(getSystemService<SensorManager>())

val Context.vibratorManager : VibratorManager
    get() = requireNotNull(getSystemService<VibratorManager>())

