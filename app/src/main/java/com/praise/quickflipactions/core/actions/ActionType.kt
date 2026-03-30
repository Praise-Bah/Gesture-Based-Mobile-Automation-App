package com.praise.quickflipactions.core.actions

/**
 * Defines the types of actions that can be triggered by gestures
 */
enum class ActionType {
    // System Actions
    /**
     * Toggle between silent mode and normal mode
     */
    TOGGLE_SILENT_MODE,
    
    /**
     * Toggle flashlight on/off
     */
    TOGGLE_FLASHLIGHT,
    
    /**
     * Launch a specific app by package name
     */
    LAUNCH_APP,
    
    // Productivity Actions
    /**
     * Start voice memo recording
     */
    VOICE_MEMO,
    
    /**
     * Start task timer/stopwatch
     */
    START_TASK_TIMER,
    
    // Media Actions
    /**
     * Take a photo using camera
     */
    TAKE_PICTURE,
    
    /**
     * Start/stop video recording
     */
    TOGGLE_VIDEO_RECORDING,
    
    // Location & Safety Actions
    /**
     * Share current live location
     */
    SHARE_LIVE_LOCATION,
    
    /**
     * Send SOS message with location
     */
    SEND_SOS_MESSAGE,
    
    /**
     * Save current location for later reference
     */
    SAVE_CURRENT_LOCATION
}
