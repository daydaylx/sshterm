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

    /**
     * Sets the output stream for writing input.
     */
    fun setOutputStream(stream: OutputStream?) {
        outputStream = stream
    }

    /**
     * Writes input to the SSH channel.
     */
    suspend fun write(data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(data)
            outputStream?.flush()
        } catch (e: Exception) {
            // TODO: Handle write errors
        }
    }

    /**
     * Sends a special key sequence (like Ctrl+C).
     */
    suspend fun sendSpecialKey(sequence: ByteArray) {
        write(sequence)
    }

    /**
     * Writes a string to the input.
     */
    suspend fun writeString(text: String) {
        write(text.toByteArray())
    }

    /**
     * Clears the output stream.
     */
    fun clear() {
        outputStream = null
    }
}
