package com.praise.quickflipactions.core.models

import com.praise.quickflipactions.core.actions.ActionType
import com.praise.quickflipactions.core.sensors.GestureType

/**
 * Represents a mapping between a gesture and an action
 */
data class GestureActionMapping(
    /**
     * Unique identifier for this mapping
     */
    val id: String,
    
    /**
     * The gesture that triggers this action
     */
    val gesture: GestureType,
    
    /**
     * The action to be executed when the gesture is detected
     */
    val action: ActionType,
    
    /**
     * Optional parameters for the action (e.g., target app package name for LAUNCH_APP)
     */
    val actionParameters: Map<String, String> = emptyMap(),
    
    /**
     * Whether this mapping is currently enabled
     */
    val isEnabled: Boolean = true,
    
    /**
     * Display name for this mapping (for UI purposes)
     */
    val displayName: String = "${gesture.name} → ${action.name}"
)
