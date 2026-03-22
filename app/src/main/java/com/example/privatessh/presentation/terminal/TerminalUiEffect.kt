package com.example.privatessh.presentation.terminal

/**
 * One-time UI effects for terminal screen.
 */
sealed class TerminalUiEffect {
    /**
     * Show connection error message.
     */
    data class ShowConnectionError(val message: String) : TerminalUiEffect()

    /**
     * Show toast message.
     */
    data class ShowToast(val message: String) : TerminalUiEffect()

    /**
     * Vibrate device on special key press.
     */
    data object Vibrate : TerminalUiEffect()

    /**
     * Navigate back to host list.
     */
    data object NavigateBack : TerminalUiEffect()

    /**
     * Request terminal resize.
     */
    data class RequestTerminalResize(val columns: Int, val rows: Int) : TerminalUiEffect()

    /**
     * Show disconnect confirmation dialog.
     */
    data object ShowDisconnectDialog : TerminalUiEffect()

    /**
     * Clear terminal output.
     */
    data object ClearTerminal : TerminalUiEffect()
}
