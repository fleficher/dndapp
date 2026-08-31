package com.flf.dndown.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import kotlin.time.Duration.Companion.seconds


fun Context.vibrate() {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        vibratorManager.defaultVibrator
    } else {
        getSystemService<Vibrator>() ?: return
    }

    vibrator.vibrate(VibrationEffect.createOneShot(1.seconds.inWholeMilliseconds, 200))
}
