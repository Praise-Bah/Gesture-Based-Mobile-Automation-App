package com.praise.quickflipactions.core.sensors

/**
 * Defines the types of gestures that can be detected by the app
 */
enum class GestureType {
    /**
     * Phone flipped from face-up to face-down position
     */
    FACE_UP_TO_DOWN,
    
    /**
     * Phone flipped from face-down to face-up position
     */
    FACE_DOWN_TO_UP,
    
    /**
     * Double tap on screen to deactivate gesture detection (optional MVP feature)
     */
    DOUBLE_TAP
}
