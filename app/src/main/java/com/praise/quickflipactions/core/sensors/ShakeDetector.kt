package com.praise.quickflipactions.core.sensors

import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import kotlin.math.sqrt

/**
 * Detects long, strong shake gestures using accelerometer data.
 */
class ShakeDetector {

    companion object {
        private const val TAG = "ShakeDetector"
        private const val SHAKE_THRESHOLD_G = 2.0f
        private const val LONG_SHAKE_DURATION_MS = 5_000L
        private const val SHAKE_RESET_TIMEOUT_MS = 800L
    }

    private var onLongShakeListener: (() -> Unit)? = null

    private var shaking = false
    private var firstShakeTimestamp: Long = 0L
    private var lastShakeTimestamp: Long = 0L
    private var longShakeTriggered = false

    fun setOnLongShakeListener(listener: () -> Unit) {
        this.onLongShakeListener = listener
    }

    /**
     * Call this with each accelerometer update.
     */
    fun onAccelerometerData(values: FloatArray) {
        if (values.size < 3) return

        val x = values[0]
        val y = values[1]
        val z = values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
        val now = SystemClock.elapsedRealtime()

        val isShakingNow = gForce > SHAKE_THRESHOLD_G

        if (isShakingNow) {
            if (!shaking) {
                // Start of a new shake burst
                shaking = true
                firstShakeTimestamp = now
                longShakeTriggered = false
                Log.d(TAG, "Shake started")
            }
            lastShakeTimestamp = now

            val duration = now - firstShakeTimestamp
            if (!longShakeTriggered && duration >= LONG_SHAKE_DURATION_MS) {
                longShakeTriggered = true
                Log.d(TAG, "Long shake detected (duration=${duration}ms)")
                onLongShakeListener?.invoke()
            }
        } else {
            // Not currently shaking; check if we should reset
            if (shaking && now - lastShakeTimestamp > SHAKE_RESET_TIMEOUT_MS) {
                Log.d(TAG, "Shake ended / reset")
                shaking = false
                firstShakeTimestamp = 0L
                lastShakeTimestamp = 0L
                longShakeTriggered = false
            }
        }
    }
}