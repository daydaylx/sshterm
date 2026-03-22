package com.example.privatessh.terminal

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Controller for handling terminal PTY resize events.
 */
@ViewModelScoped
class TerminalResizeController @Inject constructor(
    private val resizeTerminalUseCase: com.example.privatessh.domain.usecase.session.ResizeTerminalUseCase
) {

    private val _terminalSize = MutableStateFlow(TerminalSize(80, 24))
    val terminalSize: StateFlow<TerminalSize> = _terminalSize.asStateFlow()

    private var resizeScope: CoroutineScope? = null
    private var resizeJob: kotlinx.coroutines.Job? = null

    /**
     * Terminal dimensions.
     */
    data class TerminalSize(
        val columns: Int,
        val rows: Int
    )

    /**
     * Starts listening for resize events.
     */
    fun startListening(scope: CoroutineScope) {
        resizeScope = scope
    }

    /**
     * Stops listening for resize events.
     */
    fun stopListening() {
        resizeJob?.cancel()
        resizeScope = null
    }

    /**
     * Requests a terminal resize to the specified dimensions.
     */
    fun requestResize(columns: Int, rows: Int) {
        _terminalSize.value = TerminalSize(columns, rows)

        resizeScope?.launch {
            resizeTerminalUseCase(columns, rows)
        }
    }

    /**
     * Handles a screen rotation event.
     */
    fun onScreenRotation(isLandscape: Boolean, screenWidthDp: Int, screenHeightDp: Int) {
        // Estimate character dimensions (typically 8x16 pixels per character)
        val charWidthDp = 5
        val charHeightDp = 12

        val columns = maxOf(40, screenWidthDp / charWidthDp)
        val rows = maxOf(10, screenHeightDp / charHeightDp)

        requestResize(columns, rows)
    }

    /**
     * Resets to default terminal size.
     */
    fun resetToDefault() {
        requestResize(80, 24)
    }

    /**
     * Returns the current terminal dimensions.
     */
    fun getCurrentSize(): TerminalSize {
        return _terminalSize.value
    }
}
