package com.dlx.sshterm.terminal

data class TerminalScenario(
    val name: String,
    val chunks: List<ByteArray>
)

object TerminalScenarioSuite {

    fun shell(): TerminalScenario = TerminalScenario(
        name = "shell",
        chunks = listOf(
            "echo hello\r\nhello\r\nuser@host:~$ ".toByteArray()
        )
    )

    fun less(): TerminalScenario = TerminalScenario(
        name = "less",
        chunks = listOf(
            "user@host:~$ less file.txt".toByteArray(),
            "\u001B[?1049h\u001B[2J\u001B[Hfile.txt\r\nline 1\r\nline 2".toByteArray(),
            "\u001B[?25l".toByteArray()
        )
    )

    fun lessExit(): TerminalScenario = TerminalScenario(
        name = "less-exit",
        chunks = listOf(
            "user@host:~$ less file.txt".toByteArray(),
            "\u001B[?1049h\u001B[2J\u001B[Hfile.txt\r\nline 1\r\nline 2".toByteArray(),
            "\u001B[?1049l\u001B[?25h".toByteArray()
        )
    )

    fun nano(): TerminalScenario = TerminalScenario(
        name = "nano",
        chunks = listOf(
            "\u001B[?1049h\u001B[2J\u001B[HGNU nano 7.2".toByteArray(),
            "\u001B[3;1Hbuffer.txt".toByteArray(),
            "\u001B[s\u001B[5;5Htext\u001B[u".toByteArray()
        )
    )

    fun tmux(): TerminalScenario = TerminalScenario(
        name = "tmux",
        chunks = listOf(
            "\u001B[2J\u001B[H".toByteArray(),
            "top pane\r\nbottom pane".toByteArray(),
            "\u001B[24;1H\u001B[7m0:bash*                                 host    12:34\u001B[27m".toByteArray()
        )
    )

    fun palette256(): TerminalScenario = TerminalScenario(
        name = "palette-256",
        chunks = listOf(
            "plain ".toByteArray(),
            "\u001B[38;5;196mred ".toByteArray(),
            "\u001B[48;5;33mcyan-bg".toByteArray(),
            "\u001B[0m done".toByteArray()
        )
    )

    fun applicationCursorMode(): TerminalScenario = TerminalScenario(
        name = "application-cursor-mode",
        chunks = listOf(
            "\u001B[?1h".toByteArray(),
            "tmux attach".toByteArray()
        )
    )
}
