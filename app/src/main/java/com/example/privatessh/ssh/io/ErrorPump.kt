package com.example.privatessh.ssh.io

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

/**
 * Pumps error stream from an SSH channel.
 */
class ErrorPump @Inject constructor() {

    /**
     * Starts pumping error output from the channel.
     * Returns a Job that can be cancelled to stop the pump.
     */
    fun start(
        scope: CoroutineScope,
        errorStream: InputStream,
        onError: (ByteArray) -> Unit
    ): Job {
        return scope.launch(Dispatchers.IO + SupervisorJob()) {
            val buffer = ByteArray(4096)

            try {
                while (isActive) {
                    val read = errorStream.read(buffer)
                    if (read < 0) break

                    val data = buffer.copyOf(read)
                    onError(data)
                }
            } catch (e: Exception) {
                // Ignore error stream errors
            }
        }
    }
}
