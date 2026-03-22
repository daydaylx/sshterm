package com.example.privatessh.presentation.hostedit

import com.example.privatessh.domain.model.AuthType
import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.domain.model.NetworkTargetType
import com.example.privatessh.presentation.hostedit.HostEditUiState.Companion.FIELD_HOST
import com.example.privatessh.presentation.hostedit.HostEditUiState.Companion.FIELD_NAME
import com.example.privatessh.presentation.hostedit.HostEditUiState.Companion.FIELD_PORT
import com.example.privatessh.presentation.hostedit.HostEditUiState.Companion.FIELD_USER
import javax.inject.Inject

/**
 * Validator for host profile input.
 */
class HostEditValidator @Inject constructor() {

    /**
     * Validates a host profile.
     * Returns a map of field names to error messages.
     */
    fun validate(profile: HostProfile): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        // Name validation
        if (profile.name.isBlank()) {
            errors[FIELD_NAME] = "Name is required"
        }

        // Host validation
        if (profile.host.isBlank()) {
            errors[FIELD_HOST] = "Hostname or IP is required"
        } else {
            // Basic hostname/IP validation
            val hostPattern = Regex("^[a-zA-Z0-9.-]+$|^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
            if (!hostPattern.matches(profile.host)) {
                errors[FIELD_HOST] = "Invalid hostname or IP address"
            }
        }

        // Port validation
        if (profile.port !in 1..65535) {
            errors[FIELD_PORT] = "Port must be between 1 and 65535"
        }

        // User validation
        if (profile.user.isBlank()) {
            errors[FIELD_USER] = "Username is required"
        }

        return errors
    }

    /**
     * Validates individual field values.
     */
    fun validateField(field: String, value: String): String? {
        return when (field) {
            FIELD_NAME -> {
                if (value.isBlank()) "Name is required"
                else if (value.length < 2) "Name must be at least 2 characters"
                else null
            }
            FIELD_HOST -> {
                if (value.isBlank()) "Hostname or IP is required"
                else {
                    val hostPattern = Regex("^[a-zA-Z0-9.-]+$|^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
                    if (!hostPattern.matches(value)) "Invalid hostname or IP address"
                    else null
                }
            }
            FIELD_PORT -> {
                value.toIntOrNull()?.let { port ->
                    if (port !in 1..65535) "Port must be between 1 and 65535"
                    else null
                } ?: "Invalid port number"
            }
            FIELD_USER -> {
                if (value.isBlank()) "Username is required"
                else null
            }
            else -> null
        }
    }

    /**
     * Creates a HostProfile from form field values.
     */
    fun createProfileFromFields(
        name: String,
        host: String,
        port: String,
        user: String,
        authType: AuthType,
        targetType: NetworkTargetType,
        existingProfile: HostProfile? = null
    ): HostProfile {
        return existingProfile?.copy(
            name = name.trim(),
            host = host.trim(),
            port = port.toIntOrNull() ?: 22,
            user = user.trim(),
            authType = authType,
            targetType = targetType
        ) ?: HostProfile(
            name = name.trim(),
            host = host.trim(),
            port = port.toIntOrNull() ?: 22,
            user = user.trim(),
            authType = authType,
            targetType = targetType
        )
    }
}
