package com.praise.quickflipactions.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.praise.quickflipactions.R
import com.praise.quickflipactions.core.actions.ActionExecutor
import com.praise.quickflipactions.core.actions.ActionType
import com.praise.quickflipactions.core.models.GestureActionMapping
import com.praise.quickflipactions.core.sensors.GestureDetector
import com.praise.quickflipactions.core.sensors.GestureType
import com.praise.quickflipactions.core.sensors.SensorController

/**
 * Foreground service that handles gesture detection and action execution
 */
class GestureService : Service() {
    
    companion object {
        private const val TAG = "GestureService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "GestureServiceChannel"
        
        const val ACTION_START_GESTURE_DETECTION = "START_GESTURE_DETECTION"
        const val ACTION_STOP_GESTURE_DETECTION = "STOP_GESTURE_DETECTION"

        const val EXTRA_ACTION_TYPE = "EXTRA_ACTION_TYPE"
        const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
    }
    
    private enum class SessionState {
        IDLE,
        ARMED,
        ACTION_ACTIVE
    }

    private var sensorController: SensorController? = null
    private var gestureDetector: GestureDetector? = null
    private var actionExecutor: ActionExecutor? = null
    
    // TODO: In future, this will be loaded from DataStore/SharedPreferences
    private val currentMappings = mutableListOf<GestureActionMapping>()

    private var sessionState: SessionState = SessionState.IDLE
    private var armedActionType: ActionType = ActionType.TOGGLE_FLASHLIGHT
    private var armedPackageName: String? = null
    
    private var isDetectionActive = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Initialize components
        sensorController = SensorController(this)
        gestureDetector = GestureDetector()
        actionExecutor = ActionExecutor(this)
        
        // Setup gesture detection callback
        gestureDetector?.setGestureCallback { gesture ->
            handleGestureDetected(gesture)
        }
        
        // Setup sensor data callback
        sensorController?.setSensorDataCallback { accelerometer, gyroscope ->
            gestureDetector?.processSensorData(accelerometer, gyroscope)
        }
        
        // Initialize sensor controller
        if (sensorController?.initialize() != true) {
            Log.e(TAG, "Failed to initialize sensor controller")
            stopSelf()
            return
        }
        
        // Load default mappings for MVP
        loadDefaultMappings()
        
        // Create notification channel
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_GESTURE_DETECTION -> {
                updateArmedActionFromIntent(intent)
                sessionState = SessionState.ARMED
                startGestureDetection()
            }
            ACTION_STOP_GESTURE_DETECTION -> {
                stopGestureDetection()
            }
            else -> startGestureDetection() // Default behavior
        }
        
        return START_STICKY // Restart if killed by system
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopGestureDetection()
        Log.d(TAG, "Service destroyed")
    }
    
    private fun startGestureDetection() {
        if (isDetectionActive) return
        
        Log.d(TAG, "Starting gesture detection")
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start sensor listening
        sensorController?.startListening()
        
        isDetectionActive = true
    }
    
    private fun stopGestureDetection() {
        if (!isDetectionActive) return
        
        Log.d(TAG, "Stopping gesture detection")
        
        // Stop sensor listening
        sensorController?.stopListening()
        
        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        isDetectionActive = false
        sessionState = SessionState.IDLE
    }
    
    private fun handleGestureDetected(gesture: GestureType) {
        Log.d(TAG, "Gesture detected: $gesture, sessionState=$sessionState, armedActionType=$armedActionType")

        when (sessionState) {
            SessionState.IDLE -> {
                // No action armed; ignore gestures
                Log.d(TAG, "Ignoring gesture in IDLE state")
            }
            SessionState.ARMED -> {
                if (gesture == GestureType.FACE_UP_TO_DOWN) {
                    Log.d(TAG, "Starting selected action for face-down gesture")
                    startSelectedAction()
                    sessionState = SessionState.ACTION_ACTIVE
                } else {
                    Log.d(TAG, "Gesture $gesture ignored while ARMED")
                }
            }
            SessionState.ACTION_ACTIVE -> {
                if (gesture == GestureType.FACE_DOWN_TO_UP) {
                    Log.d(TAG, "Stopping selected action for face-up gesture and disabling detection")
                    stopSelectedAction()
                    stopGestureDetection()
                } else {
                    Log.d(TAG, "Gesture $gesture ignored while ACTION_ACTIVE")
                }
            }
        }
    }
    
    private fun loadDefaultMappings() {
        // TODO: In future, load from DataStore/Room
        // For now, create some default mappings for testing
        currentMappings.clear()
        
        // Default mapping: Face-up to face-down → Toggle flashlight
        currentMappings.add(
            GestureActionMapping(
                id = "default_1",
                gesture = GestureType.FACE_UP_TO_DOWN,
                action = com.praise.quickflipactions.core.actions.ActionType.TOGGLE_FLASHLIGHT,
                displayName = "Face-up to Face-down → Toggle Flashlight"
            )
        )
        
        // Default mapping: Face-down to face-up → Toggle silent mode
        currentMappings.add(
            GestureActionMapping(
                id = "default_2",
                gesture = GestureType.FACE_DOWN_TO_UP,
                action = com.praise.quickflipactions.core.actions.ActionType.TOGGLE_SILENT_MODE,
                displayName = "Face-down to Face-up → Toggle Silent Mode"
            )
        )
        
        Log.d(TAG, "Loaded ${currentMappings.size} default mappings")
    }

    private fun updateArmedActionFromIntent(intent: Intent) {
        val actionName = intent.getStringExtra(EXTRA_ACTION_TYPE)
        armedActionType = try {
            if (actionName != null) ActionType.valueOf(actionName) else ActionType.TOGGLE_FLASHLIGHT
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Unknown action type from intent: $actionName, defaulting to TOGGLE_FLASHLIGHT", e)
            ActionType.TOGGLE_FLASHLIGHT
        }

        armedPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        Log.d(TAG, "Armed action updated: $armedActionType, package=$armedPackageName")
    }

    private fun buildSessionMapping(gestureType: GestureType): GestureActionMapping {
        val params = mutableMapOf<String, String>()
        if (armedActionType == ActionType.LAUNCH_APP && !armedPackageName.isNullOrEmpty()) {
            params["packageName"] = armedPackageName!!
        }
        return GestureActionMapping(
            id = "session_${armedActionType.name}",
            gesture = gestureType,
            action = armedActionType,
            actionParameters = params
        )
    }

    private fun startSelectedAction() {
        val mapping = buildSessionMapping(GestureType.FACE_UP_TO_DOWN)
        actionExecutor?.executeAction(mapping)
    }

    private fun stopSelectedAction() {
        when (armedActionType) {
            ActionType.TOGGLE_SILENT_MODE,
            ActionType.TOGGLE_FLASHLIGHT -> {
                val mapping = buildSessionMapping(GestureType.FACE_DOWN_TO_UP)
                actionExecutor?.executeAction(mapping)
            }
            else -> {
                Log.d(TAG, "No explicit stop behavior for action $armedActionType; ending session only")
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gesture Detection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for gesture detection service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Quick Flip Actions")
            .setContentText("Gesture detection is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
