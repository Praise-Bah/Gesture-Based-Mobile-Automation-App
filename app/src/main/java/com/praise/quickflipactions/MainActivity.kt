package com.praise.quickflipactions

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.praise.quickflipactions.core.actions.ActionType
import com.praise.quickflipactions.service.GestureService
import com.praise.quickflipactions.ui.SosSettingsActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var actionGroup: RadioGroup
    private lateinit var flashlightRadio: RadioButton
    private lateinit var silentRadio: RadioButton
    private lateinit var sosSettingsButton: Button
    
    private var isServiceRunning = false
    private var selectedActionType: ActionType = ActionType.TOGGLE_FLASHLIGHT
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        updateUI()
    }
    
    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        actionGroup = findViewById(R.id.actionGroup)
        flashlightRadio = findViewById(R.id.actionFlashlight)
        silentRadio = findViewById(R.id.actionSilent)
        sosSettingsButton = findViewById(R.id.buttonSosSettings)

        // Set default selection
        flashlightRadio.isChecked = true
        selectedActionType = ActionType.TOGGLE_FLASHLIGHT
    }
    
    private fun setupClickListeners() {
        actionGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedActionType = when (checkedId) {
                R.id.actionSilent -> ActionType.TOGGLE_SILENT_MODE
                else -> ActionType.TOGGLE_FLASHLIGHT
            }
            updateUI()
        }

        startButton.setOnClickListener {
            startGestureDetection()
        }
        
        stopButton.setOnClickListener {
            stopGestureDetection()
        }

        sosSettingsButton.setOnClickListener {
            val intent = Intent(this, SosSettingsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun startGestureDetection() {
        val intent = Intent(this, GestureService::class.java).apply {
            action = GestureService.ACTION_START_GESTURE_DETECTION
            putExtra(GestureService.EXTRA_ACTION_TYPE, selectedActionType.name)
        }
        startForegroundService(intent)
        
        isServiceRunning = true
        updateUI()
    }
    
    private fun stopGestureDetection() {
        val intent = Intent(this, GestureService::class.java).apply {
            action = GestureService.ACTION_STOP_GESTURE_DETECTION
        }
        startService(intent)
        
        isServiceRunning = false
        updateUI()
    }
    
    private fun updateUI() {
        val actionLabel = when (selectedActionType) {
            ActionType.TOGGLE_SILENT_MODE -> "Silent mode"
            ActionType.TOGGLE_FLASHLIGHT -> "Flashlight"
            ActionType.LAUNCH_APP -> "Launch app"
            else -> selectedActionType.name
        }

        if (isServiceRunning) {
            statusText.text = "🟢 Gesture Detection ACTIVE\n\nSelected action: $actionLabel\n\n• Face-up → Face-down: START action\n• Face-down → Face-up: STOP action and DISABLE detection"
            startButton.isEnabled = false
            stopButton.isEnabled = true
        } else {
            statusText.text = "🔴 Gesture Detection INACTIVE\n\nSelected action: $actionLabel\n\nTap 'Start' to arm this action. Then you can lock your phone, flip face-down to START, and flip face-up to STOP and turn detection off."
            startButton.isEnabled = true
            stopButton.isEnabled = false
        }
    }
}