package com.praise.quickflipactions.core.safety

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.praise.quickflipactions.core.models.SosConfig

/**
 * Handles sending SOS SMS messages to configured contacts.
 */
class SosManager(private val context: Context) {

    companion object {
        private const val TAG = "SosManager"
        private const val FOLLOWUP_COUNT = 5
        private const val INTERVAL_MS = 5_000L
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pendingRunnables = mutableListOf<Runnable>()
    @Volatile
    private var isActive: Boolean = false

    fun isActive(): Boolean = isActive

    fun startSos(config: SosConfig) {
        if (!config.enabled || !config.hasValidContacts()) {
            Log.d(TAG, "SOS config not enabled or not enough contacts, ignoring startSos")
            return
        }

        if (!hasSmsPermission()) {
            Log.w(TAG, "SEND_SMS permission not granted, cannot send SOS messages")
            return
        }

        // Reset any existing sequence
        cancel()
        isActive = true

        val locationLink = getLocationLink()
        val baseMessage = if (config.customMessage.isNotBlank()) config.customMessage else SosConfig.DEFAULT_MESSAGE

        // First SMS immediately, includes location link if available
        schedule(0L) {
            sendInitialMessages(config, baseMessage, locationLink)
        }

        // Follow-up messages every INTERVAL_MS, without mandatory location link
        for (i in 1..FOLLOWUP_COUNT) {
            schedule(INTERVAL_MS * i) {
                sendFollowupMessages(config, baseMessage)
            }
        }
    }

    fun cancel() {
        isActive = false
        pendingRunnables.forEach { handler.removeCallbacks(it) }
        pendingRunnables.clear()
        Log.d(TAG, "SOS sequence cancelled")
    }

    private fun schedule(delayMs: Long, block: () -> Unit) {
        val runnable = Runnable {
            if (!isActive) return@Runnable
            block()
        }
        pendingRunnables.add(runnable)
        handler.postDelayed(runnable, delayMs)
    }

    private fun sendInitialMessages(config: SosConfig, baseMessage: String, locationLink: String?) {
        val text = if (locationLink != null) {
            // First SMS is focused on location so contacts can tap the map link immediately.
            "Location: $locationLink"
        } else {
            // Fallback to base message if we cannot obtain location.
            baseMessage
        }
        sendSmsToAll(config, text)
    }

    private fun sendFollowupMessages(config: SosConfig, baseMessage: String) {
        sendSmsToAll(config, baseMessage)
    }

    private fun sendSmsToAll(config: SosConfig, text: String) {
        try {
            val smsManager = SmsManager.getDefault()
            config.contacts.forEach { contact ->
                if (contact.phoneNumber.isNotBlank()) {
                    Log.d(TAG, "Sending SOS SMS to ${contact.phoneNumber}")
                    smsManager.sendTextMessage(contact.phoneNumber, null, text, null, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SOS SMS", e)
        }
    }

    private fun hasSmsPermission(): Boolean {
        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
        return status == PermissionChecker.PERMISSION_GRANTED
    }

    private fun getLocationLink(): String? {
        val fineStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineStatus != PermissionChecker.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted, cannot include GPS link in SOS SMS")
            return null
        }

        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || (loc.accuracy < bestLocation!!.accuracy)) {
                    bestLocation = loc
                }
            }
            if (bestLocation != null) {
                val lat = bestLocation.latitude
                val lon = bestLocation.longitude
                "https://maps.google.com/?q=$lat,$lon"
            } else {
                null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting location for SOS", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location for SOS", e)
            null
        }
    }
}