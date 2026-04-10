package com.praise.quickflipactions.core.safety

import android.content.Context
import com.praise.quickflipactions.core.models.SosConfig
import com.praise.quickflipactions.core.models.SosContact

/**
 * Simple SharedPreferences-based storage for SOS configuration
 */
class SosPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getConfig(): SosConfig {
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        val contactsString = prefs.getString(KEY_CONTACTS, "") ?: ""
        val message = prefs.getString(KEY_MESSAGE, SosConfig.DEFAULT_MESSAGE) ?: SosConfig.DEFAULT_MESSAGE
        val contacts = parseContacts(contactsString)
        return SosConfig(
            enabled = enabled,
            contacts = contacts,
            customMessage = message
        )
    }

    fun saveConfig(config: SosConfig) {
        val contactsString = serializeContacts(config.contacts)
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_CONTACTS, contactsString)
            .putString(KEY_MESSAGE, config.customMessage)
            .apply()
    }

    private fun parseContacts(raw: String): List<SosContact> {
        if (raw.isBlank()) return emptyList()
        return raw.split(ENTRY_SEPARATOR)
            .mapNotNull { entry ->
                val parts = entry.split(FIELD_SEPARATOR)
                if (parts.size == 2) {
                    val name = parts[0]
                    val phone = parts[1]
                    if (phone.isNotBlank()) SosContact(name, phone) else null
                } else {
                    null
                }
            }
    }

    private fun serializeContacts(contacts: List<SosContact>): String {
        return contacts.joinToString(ENTRY_SEPARATOR) { contact ->
            "${contact.name}$FIELD_SEPARATOR${contact.phoneNumber}"
        }
    }

    companion object {
        private const val PREFS_NAME = "sos_prefs"
        private const val KEY_ENABLED = "sos_enabled"
        private const val KEY_CONTACTS = "sos_contacts"
        private const val KEY_MESSAGE = "sos_message"

        private const val ENTRY_SEPARATOR = ";"
        private const val FIELD_SEPARATOR = "|"
    }
}