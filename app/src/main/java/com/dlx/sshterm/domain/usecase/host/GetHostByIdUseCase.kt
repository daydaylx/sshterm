package com.dlx.sshterm.domain.usecase.host

import com.dlx.sshterm.domain.model.HostProfile
import com.dlx.sshterm.domain.repository.HostRepository
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
