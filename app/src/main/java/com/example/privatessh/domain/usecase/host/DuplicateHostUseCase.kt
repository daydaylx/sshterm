package com.example.privatessh.domain.usecase.host

import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.domain.repository.HostRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for duplicating a host.
 */
class DuplicateHostUseCase @Inject constructor(
    private val hostRepository: HostRepository
) {
    suspend operator fun invoke(hostId: String): HostProfile? {
        val original = hostRepository.getHost(hostId) ?: return null

        val duplicate = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            lastConnectedAt = null,
            connectOnLaunch = false
        )

        hostRepository.save(duplicate)
        return duplicate
    }
}
