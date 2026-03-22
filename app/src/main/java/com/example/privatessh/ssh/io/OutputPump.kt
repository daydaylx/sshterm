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
 * Pumps output from an SSH channel to a buffer.
 */
class OutputPump @Inject constructor() {

    /**
     * Starts pumping output from the channel to the buffer.
     * Returns a Job that can be cancelled to stop the pump.
     */
    fun start(
        scope: CoroutineScope,
        inputStream: InputStream,
        onOutput: (ByteArray) -> Unit,
        onClosed: (String?) -> Unit = {}
    ): Job {
        return scope.launch(Dispatchers.IO + SupervisorJob()) {
            val buffer = ByteArray(8192)

            try {
                while (isActive) {
                    val read = inputStream.read(buffer)
                    if (read < 0) {
                        onClosed("SSH channel closed.")
                        break
                    }

                    val data = buffer.copyOf(read)
                    onOutput(data)
                }
            } catch (e: Exception) {
                if (isActive) {
                    onClosed(e.message ?: "SSH channel closed unexpectedly.")
                }
            }
        }
    }
}
