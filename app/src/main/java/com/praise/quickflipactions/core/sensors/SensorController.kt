package com.praise.quickflipactions.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * Manages sensor reading and provides normalized sensor data
 */
class SensorController(private val context: Context) : SensorEventListener {
    
    companion object {
        private const val TAG = "SensorController"
    }
    
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    
    // Current sensor values
    private var accelerometerValues = FloatArray(3)
    private var gyroscopeValues = FloatArray(3)
    
    // Callback for sensor data updates
    private var onSensorDataChanged: ((accelerometer: FloatArray, gyroscope: FloatArray) -> Unit)? = null
    
    /**
     * Initialize sensor manager and sensors
     */
    fun initialize(): Boolean {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        
        if (accelerometer == null) {
            Log.e(TAG, "Accelerometer not available")
            return false
        }
        
        if (gyroscope == null) {
            Log.w(TAG, "Gyroscope not available - gesture detection may be less accurate")
        }
        
        return true
    }
    
    /**
     * Start listening to sensor events
     */
    fun startListening() {
        sensorManager?.let { sm ->
            accelerometer?.let { accel ->
                sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
                Log.d(TAG, "Started listening to accelerometer")
            }
            
            gyroscope?.let { gyro ->
                sm.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
                Log.d(TAG, "Started listening to gyroscope")
            }
        }
    }
    
    /**
     * Stop listening to sensor events
     */
    fun stopListening() {
        sensorManager?.unregisterListener(this)
        Log.d(TAG, "Stopped listening to sensors")
    }
    
    /**
     * Set callback for sensor data updates
     */
    fun setSensorDataCallback(callback: (accelerometer: FloatArray, gyroscope: FloatArray) -> Unit) {
        this.onSensorDataChanged = callback
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerValues, 0, 3)
            }
            Sensor.TYPE_GYROSCOPE -> {
                System.arraycopy(event.values, 0, gyroscopeValues, 0, 3)
            }
        }
        
        // Notify callback with current sensor data
        onSensorDataChanged?.invoke(accelerometerValues.clone(), gyroscopeValues.clone())
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: ${sensor?.name} -> $accuracy")
    }
    
    /**
     * Get current accelerometer values
     */
    fun getCurrentAccelerometerValues(): FloatArray = accelerometerValues.clone()
    
    /**
     * Get current gyroscope values
     */
    fun getCurrentGyroscopeValues(): FloatArray = gyroscopeValues.clone()
}
