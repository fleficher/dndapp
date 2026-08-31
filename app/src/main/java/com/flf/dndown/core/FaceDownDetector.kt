package com.flf.dndown.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

private const val BATCH_MAX_REPORT_LATENCY_MS = 1_000

class FaceDownDetector(
    context: Context,
    private val onStateChanged: (isFaceDown: Boolean) -> Unit
) : SensorEventListener {

    private val sensorManager = context.sensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private var isNear = false
    private var isDark = false
    private var isFaceDown = false

    private val stateFlow = MutableStateFlow(false)
    private var detectorScope: CoroutineScope? = null
    private var collectionJob: Job? = null
    private var sensorThread: HandlerThread? = null

    @OptIn(FlowPreview::class)
    fun start() {
        stop()

        detectorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        collectionJob = detectorScope?.launch {
            stateFlow
                .debounce(3.seconds)
                .collect { throttledState ->
                    onStateChanged(throttledState)
                }
        }

        val handlerThread = HandlerThread("FaceDownSensors").apply { start() }
        sensorThread = handlerThread
        val handler = Handler(handlerThread.looper)

        proximitySensor = sensorManager.getSensorList(Sensor.TYPE_PROXIMITY)
            .find { it.isWakeUpSensor }
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        register(proximitySensor, maxReportLatencyMs = 0, handler)
        register(accelerometer, BATCH_MAX_REPORT_LATENCY_MS, handler)
        register(lightSensor, BATCH_MAX_REPORT_LATENCY_MS, handler)
    }

    private fun register(sensor: Sensor?, maxReportLatencyMs: Int, handler: Handler) {
        sensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL,
                maxReportLatencyMs * 1_000,
                handler
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        sensorThread?.quitSafely()
        sensorThread = null
        detectorScope?.cancel()
        detectorScope = null
        collectionJob = null
        isNear = false
        isDark = false
        isFaceDown = false
        stateFlow.value = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                val maxRange = event.sensor.maximumRange
                isNear = distance < maxRange / 2
                evaluateOrientation()
            }

            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                // If face down on a table, lux should be very low (0.0 or near 0)
                isDark = lux <= 1.0f
                evaluateOrientation()
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                isFaceDown = z < -7.5 && abs(x) < 3.5 && abs(y) < 3.5
                evaluateOrientation()
            }
        }
    }

    private fun evaluateOrientation() {
        val activeFaceDown = isFaceDown && (isDark || isNear)
        stateFlow.value = activeFaceDown
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
