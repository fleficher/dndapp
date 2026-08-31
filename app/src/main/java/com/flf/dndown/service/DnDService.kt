package com.flf.dndown.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.flf.dndown.core.FaceDownDetector
import com.flf.dndown.core.createNotification
import com.flf.dndown.core.createNotificationChannel
import com.flf.dndown.core.currentInterruptionFilter
import com.flf.dndown.core.setDndMode
import com.flf.dndown.core.targetInterruptionFilter
import com.flf.dndown.core.vibrate

class DnDService : Service() {

    private lateinit var faceDownDetector: FaceDownDetector

    override fun onCreate() {
        super.onCreate()
        DnDServiceState.setRunning(true)
        createNotificationChannel(this)
        startServiceInForeground()

        faceDownDetector = FaceDownDetector(this) { isFaceDown ->
            updateDnDState(isFaceDown)
        }.also {
            it.start()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startServiceInForeground() {
        val notification = createNotification(this)

        val serviceInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            @Suppress("DEPRECATION")
            ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
        }
        ServiceCompat.startForeground(
            this,
            1,
            notification,
            serviceInfo,
        )

    }

    private fun updateDnDState(isFaceDown: Boolean) {
        val targetFilter = targetInterruptionFilter(isFaceDown)
        if (currentInterruptionFilter() != targetFilter) {
            setDndMode(enable = isFaceDown)
            if (isFaceDown) {
                vibrate()
            }
        }
    }

    override fun onDestroy() {
        DnDServiceState.setRunning(false)
        faceDownDetector.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}