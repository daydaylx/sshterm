package com.example.privatessh.ssh.io

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

/**
 * Pumps error stream from an SSH channel.
 */
class ErrorPump @Inject constructor() {

    fun start(
        scope: CoroutineScope,
        errorStream: InputStream,
        onClosed: (String?) -> Unit = {},
        onError: (ByteArray) -> Unit
    ): Job {
        return scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            try {
                while (isActive) {
                    val read = errorStream.read(buffer)
                    if (read < 0) {
                        onClosed(null)
                        break
                    }
                    onError(buffer.copyOf(read))
                }
            } catch (e: Exception) {
                if (isActive) onClosed(e.message)
            }
        }
    }
}
