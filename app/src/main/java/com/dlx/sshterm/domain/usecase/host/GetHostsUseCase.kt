package com.dlx.sshterm.domain.usecase.host

import com.dlx.sshterm.domain.model.HostProfile
import com.dlx.sshterm.domain.repository.HostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing all hosts.
 */
class GetHostsUseCase @Inject constructor(
    private val hostRepository: HostRepository
) {
    operator fun invoke(): Flow<List<HostProfile>> =
        hostRepository.observeHosts()
}
