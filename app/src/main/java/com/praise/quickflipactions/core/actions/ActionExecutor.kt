package com.praise.quickflipactions.core.actions

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.util.Log
import com.praise.quickflipactions.core.models.GestureActionMapping

/**
 * Executes actions based on gesture mappings
 */
class ActionExecutor(private val context: Context) {
    
    companion object {
        private const val TAG = "ActionExecutor"
    }
    
    private var audioManager: AudioManager? = null
    private var cameraManager: CameraManager? = null
    private var flashlightOn = false
    
    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    
    /**
     * Execute an action based on gesture mapping
     */
    fun executeAction(mapping: GestureActionMapping) {
        if (!mapping.isEnabled) {
            Log.d(TAG, "Mapping is disabled: ${mapping.displayName}")
            return
        }
        
        Log.d(TAG, "Executing action: ${mapping.action} for gesture: ${mapping.gesture}")
        
        try {
            when (mapping.action) {
                ActionType.TOGGLE_SILENT_MODE -> toggleSilentMode()
                ActionType.TOGGLE_FLASHLIGHT -> toggleFlashlight()
                ActionType.LAUNCH_APP -> launchApp(mapping.actionParameters["packageName"])
                ActionType.VOICE_MEMO -> startVoiceMemo()
                ActionType.START_TASK_TIMER -> startTaskTimer()
                ActionType.TAKE_PICTURE -> takePicture()
                ActionType.TOGGLE_VIDEO_RECORDING -> toggleVideoRecording()
                ActionType.SHARE_LIVE_LOCATION -> shareLiveLocation()
                ActionType.SEND_SOS_MESSAGE -> sendSOSMessage()
                ActionType.SAVE_CURRENT_LOCATION -> saveCurrentLocation()
                else -> {
                    Log.w(TAG, "Unknown action type: ${mapping.action}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action: ${mapping.action}", e)
        }
    }
    
    private fun toggleSilentMode() {
        audioManager?.let { am ->
            when (am.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> {
                    am.ringerMode = AudioManager.RINGER_MODE_SILENT
                    Log.d(TAG, "Switched to silent mode")
                }
                AudioManager.RINGER_MODE_SILENT,
                AudioManager.RINGER_MODE_VIBRATE -> {
                    am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    Log.d(TAG, "Switched to normal mode")
                }
                else -> {
                    Log.d(TAG, "Ringer mode unchanged (unsupported mode): ${am.ringerMode}")
                }
            }
        }
    }
    
    private fun toggleFlashlight() {
        try {
            cameraManager?.let { cm ->
                val cameraIdList = cm.cameraIdList
                if (cameraIdList.isNotEmpty()) {
                    val cameraId = cameraIdList[0] // Usually back camera
                    cm.setTorchMode(cameraId, !flashlightOn)
                    flashlightOn = !flashlightOn
                    Log.d(TAG, "Flashlight ${if (flashlightOn) "ON" else "OFF"}")
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error toggling flashlight", e)
        }
    }
    
    private fun launchApp(packageName: String?) {
        if (packageName.isNullOrEmpty()) {
            Log.w(TAG, "No package name provided for LAUNCH_APP")
            return
        }
        
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                Log.d(TAG, "Launched app: $packageName")
            } else {
                Log.w(TAG, "App not found: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app: $packageName", e)
        }
    }
    
    // Placeholder implementations for future features
    private fun startVoiceMemo() {
        Log.d(TAG, "Voice memo - TODO: Implement in future phase")
        // TODO: Implement in Section 7
    }
    
    private fun startTaskTimer() {
        Log.d(TAG, "Task timer - TODO: Implement in future phase")
        // TODO: Implement in Section 7
    }
    
    private fun takePicture() {
        Log.d(TAG, "Take picture - TODO: Implement in future phase")
        // TODO: Implement in Section 7
    }
    
    private fun toggleVideoRecording() {
        Log.d(TAG, "Video recording - TODO: Implement in future phase")
        // TODO: Implement in Section 7
    }
    
    private fun shareLiveLocation() {
        Log.d(TAG, "Share live location - TODO: Implement in future phase")
        // TODO: Implement in Section 7
    }
    
    private fun sendSOSMessage() {
        Log.d(TAG, "SOS message - TODO: Implement in future phase")
        // TODO: Implement in Section 7
    }
    
    private fun saveCurrentLocation() {
        Log.d(TAG, "Save location - TODO: Implement in future phase")
        // TODO: Implement in Section 7
    }
}
