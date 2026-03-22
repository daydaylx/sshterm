package com.example.privatessh.domain.usecase.host

import com.example.privatessh.domain.repository.HostRepository
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
