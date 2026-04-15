package com.dlx.sshterm.ssh

import com.dlx.sshterm.domain.model.SessionPolicy

enum class SessionShellMode {
    SHELL,
    TMUX_REQUESTED,
    TMUX_ATTACHED,
    TMUX_FALLBACK
}

data class SessionShellStatus(
    val mode: SessionShellMode = SessionShellMode.SHELL,
    val message: String? = null
)

data class ShellBootstrapPlan(
    val command: String?,
    val initialStatus: SessionShellStatus
)

object ShellBootstrapPlanner {
    const val DEFAULT_TMUX_SESSION_NAME = "main"
    const val TMUX_FALLBACK_MARKER = "__SSHAPP_TMUX_FALLBACK__"
    const val TMUX_CONFIRMED_MARKER = "__SSHAPP_TMUX_CONFIRMED__"

    fun buildPlan(policy: SessionPolicy): ShellBootstrapPlan {
        if (!policy.tmuxAutoAttach) {
            return ShellBootstrapPlan(
                command = null,
                initialStatus = SessionShellStatus()
            )
        }

        val sessionName = normalizeTmuxSessionName(policy.tmuxSessionName)
        val quotedSessionName = shellQuote(sessionName)
        // CONFIRMED_MARKER is printed before handing off to tmux so we can detect it in the
        // output stream. FALLBACK_MARKER is printed when tmux is not available.
        val command = buildString {
            append("if command -v tmux >/dev/null 2>&1; then ")
            append("printf '$TMUX_CONFIRMED_MARKER\\n'; ")
            append("tmux attach -t $quotedSessionName || tmux new -s $quotedSessionName || printf '$TMUX_FALLBACK_MARKER\\n'; ")
            append("else printf '$TMUX_FALLBACK_MARKER\\n'; ")
            append("fi\n")
        }

        return ShellBootstrapPlan(
            command = command,
            initialStatus = SessionShellStatus(
                mode = SessionShellMode.TMUX_REQUESTED,
                message = "tmux attach/create requested"
            )
        )
    }

    fun normalizeTmuxSessionName(rawName: String?): String =
        rawName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_TMUX_SESSION_NAME

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\"'\"'") + "'"
}
