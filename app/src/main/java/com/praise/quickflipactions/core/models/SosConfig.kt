package com.praise.quickflipactions.core.models

/**
 * Configuration for the SOS shake alert feature
 */
data class SosConfig(
    val enabled: Boolean = false,
    val contacts: List<SosContact> = emptyList(),
    val customMessage: String = DEFAULT_MESSAGE
) {
    fun hasValidContacts(): Boolean = contacts.size >= 2

    companion object {
        const val DEFAULT_MESSAGE: String = "I am in trouble. Please help me."
    }
}