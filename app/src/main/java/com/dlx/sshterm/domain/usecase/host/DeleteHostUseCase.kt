package com.dlx.sshterm.domain.usecase.host

import com.dlx.sshterm.domain.repository.HostRepository
import javax.inject.Inject

/**
 * Use case for deleting a host by ID.
 */
class DeleteHostUseCase @Inject constructor(
    private val hostRepository: HostRepository
) {
    suspend operator fun invoke(id: String) {
        hostRepository.delete(id)
    }
}
