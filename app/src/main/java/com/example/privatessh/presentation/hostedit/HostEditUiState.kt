package com.example.privatessh.presentation.hostedit

import com.example.privatessh.domain.model.AuthType
import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.domain.model.NetworkTargetType

/**
 * UI State for the host edit screen.
 */
data class HostEditUiState(
    val isNewHost: Boolean = true,
    val hostProfile: HostProfile? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val generalError: String? = null,
    val didSave: Boolean = false,
    val didDelete: Boolean = false,
    val hasStoredPrivateKey: Boolean = false
) {
    /**
     * Returns true if the form is currently being saved.
     */
    val isBusy: Boolean
        get() = isSaving || isDeleting

    /**
     * Returns true if the form has validation errors.
     */
    val hasErrors: Boolean
        get() = validationErrors.isNotEmpty() || generalError != null

    /**
     * Gets the current form values or defaults if editing existing host.
     */
    fun getFieldValueOrDefault(field: String): String {
        return when (field) {
            FIELD_NAME -> hostProfile?.name ?: ""
            FIELD_HOST -> hostProfile?.host ?: ""
            FIELD_PORT -> hostProfile?.port?.toString() ?: "22"
            FIELD_USER -> hostProfile?.user ?: ""
            else -> ""
        }
    }

    /**
     * Gets the current auth type.
     */
    val authType: AuthType
        get() = hostProfile?.authType ?: AuthType.PASSWORD

    /**
     * Gets the current network target type.
     */
    val targetType: NetworkTargetType
        get() = hostProfile?.targetType ?: NetworkTargetType.DIRECT

    /**
     * Gets the error message for a specific field.
     */
    fun getFieldError(field: String): String? = validationErrors[field]

    companion object {
        const val FIELD_NAME = "name"
        const val FIELD_HOST = "host"
        const val FIELD_PORT = "port"
        const val FIELD_USER = "user"
    }
}
