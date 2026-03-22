package com.example.privatessh.domain.usecase.host

import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.domain.repository.HostRepository
import javax.inject.Inject

/**
 * Use case for getting a host by ID.
 */
class GetHostByIdUseCase @Inject constructor(
    private val hostRepository: HostRepository
) {
    suspend operator fun invoke(id: String): HostProfile? =
        hostRepository.getHost(id)
}
