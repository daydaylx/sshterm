package com.example.privatessh.ssh.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writer for sending input to an SSH channel.
 */
@Singleton
class InputWriter @Inject constructor() {

    private var outputStream: OutputStream? = null
    private var onWriteError: ((Exception) -> Unit)? = null

    fun setOutputStream(stream: OutputStream?, onError: ((Exception) -> Unit)? = null) {
        outputStream = stream
        onWriteError = onError
    }

    suspend fun write(data: ByteArray) = withContext(Dispatchers.IO) {
        val stream = outputStream ?: return@withContext
        try {
            stream.write(data)
            stream.flush()
        } catch (e: Exception) {
            onWriteError?.invoke(e)
        }
    }

    suspend fun sendSpecialKey(sequence: ByteArray) {
        write(sequence)
    }

    suspend fun writeString(text: String) {
        write(text.toByteArray())
    }

    fun clear() {
        outputStream = null
        onWriteError = null
    }
}
