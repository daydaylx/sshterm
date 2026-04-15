package com.dlx.sshterm.terminal

class TerminalSpikeRunner(
    columns: Int = 80,
    rows: Int = 24
) {
    private val emulator = TerminalEmulator(columns = columns, rows = rows)

    fun run(scenario: TerminalScenario): TerminalRendererState {
        scenario.chunks.forEach { chunk ->
            emulator.feed(chunk)
        }
        return emulator.snapshot()
    }
}
