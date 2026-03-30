package com.praise.quickflipactions.core.sensors

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects gestures based on sensor data and emits gesture events
 */
class GestureDetector {
    
    companion object {
        private const val TAG = "GestureDetector"
        
        // Orientation thresholds (m/s²)
        private const val FACE_UP_THRESHOLD = 8.0f
        private const val FACE_DOWN_THRESHOLD = -8.0f
        
        // Motion thresholds
        private const val MIN_MOTION_THRESHOLD = 2.0f
        private const val MAX_FLIP_DURATION_MS = 2000L
        private const val MIN_TIME_BETWEEN_FLIPS_MS = 1000L
    }
    
    // Current state
    private var currentOrientation: Orientation = Orientation.UNKNOWN
    private var lastOrientationChangeTime = 0L
    private var lastFlipTime = 0L
    
    // Callback for gesture detection
    private var onGestureDetected: ((GestureType) -> Unit)? = null
    
    /**
     * Phone orientation states
     */
    private enum class Orientation {
        FACE_UP,
        FACE_DOWN,
        UNKNOWN
    }
    
    /**
     * Set callback for gesture detection events
     */
    fun setGestureCallback(callback: (GestureType) -> Unit) {
        this.onGestureDetected = callback
    }
    
    /**
     * Process sensor data and detect gestures
     */
    fun processSensorData(accelerometer: FloatArray, gyroscope: FloatArray) {
        val currentTime = System.currentTimeMillis()
        
        // Calculate orientation based on gravity (Z-axis)
        val zAcceleration = accelerometer[2]
        val newOrientation = when {
            zAcceleration > FACE_UP_THRESHOLD -> Orientation.FACE_UP
            zAcceleration < FACE_DOWN_THRESHOLD -> Orientation.FACE_DOWN
            else -> Orientation.UNKNOWN
        }
        
        // Check if orientation changed
        if (newOrientation != currentOrientation && newOrientation != Orientation.UNKNOWN) {
            val orientationChanged = currentOrientation != Orientation.UNKNOWN
            
            if (orientationChanged) {
                val timeSinceLastChange = currentTime - lastOrientationChangeTime
                val timeSinceLastFlip = currentTime - lastFlipTime
                
                // Check if this could be a flip gesture
                if (timeSinceLastChange < MAX_FLIP_DURATION_MS && 
                    timeSinceLastFlip > MIN_TIME_BETWEEN_FLIPS_MS) {
                    
                    // Check if there was sufficient motion (using gyroscope)
                    if (hasSignificantMotion(gyroscope)) {
                        val gestureType = determineGestureType(currentOrientation, newOrientation)
                        gestureType?.let { gesture ->
                            Log.d(TAG, "Gesture detected: $gesture")
                            onGestureDetected?.invoke(gesture)
                            lastFlipTime = currentTime
                        }
                    }
                }
            }
            
            currentOrientation = newOrientation
            lastOrientationChangeTime = currentTime
            
            Log.d(TAG, "Orientation changed to: $newOrientation (Z: $zAcceleration)")
        }
    }
    
    /**
     * Check if there's significant motion based on gyroscope data
     */
    private fun hasSignificantMotion(gyroscope: FloatArray): Boolean {
        val motionMagnitude = sqrt(
            gyroscope[0] * gyroscope[0] + 
            gyroscope[1] * gyroscope[1] + 
            gyroscope[2] * gyroscope[2]
        )
        
        return motionMagnitude > MIN_MOTION_THRESHOLD
    }
    
    /**
     * Determine the gesture type based on orientation transition
     */
    private fun determineGestureType(from: Orientation, to: Orientation): GestureType? {
        return when {
            from == Orientation.FACE_UP && to == Orientation.FACE_DOWN -> GestureType.FACE_UP_TO_DOWN
            from == Orientation.FACE_DOWN && to == Orientation.FACE_UP -> GestureType.FACE_DOWN_TO_UP
            else -> null
        }
    }
    
    /**
     * Get current orientation for debugging
     */
    fun getCurrentOrientation(): String = currentOrientation.name
}
