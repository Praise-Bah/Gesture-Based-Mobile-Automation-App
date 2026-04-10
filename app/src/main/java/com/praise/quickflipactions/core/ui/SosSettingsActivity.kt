package com.praise.quickflipactions.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.praise.quickflipactions.R
import com.praise.quickflipactions.core.models.SosConfig
import com.praise.quickflipactions.core.models.SosContact
import com.praise.quickflipactions.core.safety.SosPreferences

class SosSettingsActivity : AppCompatActivity() {

    private lateinit var sosPreferences: SosPreferences

    private lateinit var enableSwitch: Switch
    private lateinit var contactsText: TextView
    private lateinit var messageEditText: EditText
    private lateinit var selectContactsButton: Button
    private lateinit var saveButton: Button

    private var currentConfig: SosConfig = SosConfig()
    private val mutableContacts = mutableListOf<SosContact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_settings)

        sosPreferences = SosPreferences(this)

        enableSwitch = findViewById(R.id.switchEnableSos)
        contactsText = findViewById(R.id.textContacts)
        messageEditText = findViewById(R.id.editMessage)
        selectContactsButton = findViewById(R.id.buttonSelectContacts)
        saveButton = findViewById(R.id.buttonSaveSos)

        loadConfig()
        setupListeners()
    }

    private fun loadConfig() {
        currentConfig = sosPreferences.getConfig()
        enableSwitch.isChecked = currentConfig.enabled
        mutableContacts.clear()
        mutableContacts.addAll(currentConfig.contacts)
        messageEditText.setText(currentConfig.customMessage)
        updateContactsText()
    }

    private fun setupListeners() {
        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Ensure we have all required permissions before really enabling SOS
                if (!ensureSmsAndLocationPermissions()) {
                    // Permissions not yet granted; temporarily uncheck until result comes back
                    enableSwitch.isChecked = false
                    return@setOnCheckedChangeListener
                }
            }
            currentConfig = currentConfig.copy(enabled = isChecked)
        }

        selectContactsButton.setOnClickListener {
            ensureContactsPermissionAndPick()
        }

        saveButton.setOnClickListener {
            val updated = currentConfig.copy(
                contacts = mutableContacts.toList(),
                customMessage = messageEditText.text.toString().trim()
            )
            sosPreferences.saveConfig(updated)
            Toast.makeText(this, "SOS settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun updateContactsText() {
        if (mutableContacts.isEmpty()) {
            contactsText.text = "No SOS contacts selected (min 2, max 5)."
        } else {
            val summary = mutableContacts.joinToString("\n") { "${it.name} - ${it.phoneNumber}" }
            contactsText.text = summary
        }
    }

    private fun ensureContactsPermissionAndPick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_READ_CONTACTS)
        } else {
            launchContactPicker()
        }
    }

    private fun ensureSmsAndLocationPermissions(): Boolean {
        val needsSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
        val needsLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED

        if (!needsSms && !needsLocation) {
            return true
        }

        val permissionsToRequest = mutableListOf<String>()
        if (needsSms) permissionsToRequest.add(Manifest.permission.SEND_SMS)
        if (needsLocation) permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_SMS_AND_LOCATION)
        }
        return false
    }

    private fun launchContactPicker() {
        if (mutableContacts.size >= 5) {
            Toast.makeText(this, "Maximum 5 SOS contacts reached", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_CONTACT)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchContactPicker()
            } else {
                Toast.makeText(this, "Contacts permission is required to pick SOS contacts", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQUEST_SMS_AND_LOCATION) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                // Now we can safely enable SOS
                enableSwitch.isChecked = true
                currentConfig = currentConfig.copy(enabled = true)
            } else {
                Toast.makeText(this, "SMS and location permissions are required for SOS alerts", Toast.LENGTH_LONG).show()
                enableSwitch.isChecked = false
                currentConfig = currentConfig.copy(enabled = false)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_CONTACT && resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val name = it.getString(0) ?: "Unknown"
                    val phone = it.getString(1) ?: ""
                    if (phone.isNotBlank()) {
                        addContact(SosContact(name, phone))
                    }
                }
            }
        }
    }

    private fun addContact(contact: SosContact) {
        if (mutableContacts.any { it.phoneNumber == contact.phoneNumber }) {
            Toast.makeText(this, "Contact already added", Toast.LENGTH_SHORT).show()
            return
        }
        if (mutableContacts.size >= 5) {
            Toast.makeText(this, "Maximum 5 SOS contacts reached", Toast.LENGTH_SHORT).show()
            return
        }
        mutableContacts.add(contact)
        updateContactsText()
    }

    companion object {
        private const val REQUEST_READ_CONTACTS = 1001
        private const val REQUEST_PICK_CONTACT = 1002
        private const val REQUEST_SMS_AND_LOCATION = 1003
    }
}