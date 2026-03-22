package com.example.privatessh.domain.usecase.host

import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.domain.repository.HostRepository
import javax.inject.Inject

/**
 * Use case for saving a host (create or update).
 */
class SaveHostUseCase @Inject constructor(
    private val hostRepository: HostRepository
) {
    suspend operator fun invoke(host: HostProfile) {
        require(host.isValid()) {
            "Host profile is not valid: name=${host.name}, host=${host.host}, user=${host.user}, port=${host.port}"
        }
        hostRepository.save(host)
    }
}
