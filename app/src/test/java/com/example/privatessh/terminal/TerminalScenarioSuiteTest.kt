package com.example.privatessh.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalScenarioSuiteTest {

    @Test
    fun parser_handles_chunk_split_escape_sequence_and_utf8() {
        val parser = TerminalOutputParser()

        val first = parser.feed(byteArrayOf(0x1B, '['.code.toByte(), '3'.code.toByte(), '1'.code.toByte()))
        val second = parser.feed(byteArrayOf('m'.code.toByte(), 'R'.code.toByte(), 0xC3.toByte()))
        val third = parser.feed(byteArrayOf(0xA4.toByte()))

        assertTrue(first.isEmpty())
        assertTrue(second.any { it is TerminalToken.CsiSequence && it.finalChar == 'm' })
        assertTrue(second.any { it is TerminalToken.Text && it.value == "R" })
        assertTrue(third.any { it is TerminalToken.Text && it.value == "ä" })
    }

    @Test
    fun shell_scenario_keeps_prompt_visible() {
        val state = TerminalSpikeRunner().run(TerminalScenarioSuite.shell())

        val renderedRows = state.renderRows.map { it.toPlainText() }

        assertTrue(renderedRows.any { it.contains("hello") })
        assertTrue(renderedRows.any { it.contains("user@host:~$") })
        assertFalse(state.isAlternateScreen)
    }

    @Test
    fun less_scenario_uses_alternate_screen_and_hides_cursor() {
        val state = TerminalSpikeRunner().run(TerminalScenarioSuite.less())

        assertTrue(state.isAlternateScreen)
        assertFalse(state.isCursorVisible)
        assertEquals("file.txt", state.renderRows[0].toPlainText().trim())
    }

    @Test
    fun less_exit_restores_primary_screen() {
        val state = TerminalSpikeRunner().run(TerminalScenarioSuite.lessExit())

        assertFalse(state.isAlternateScreen)
        val renderedText = state.renderRows.joinToString("\n") { it.toPlainText() }
        assertTrue(renderedText.contains("user@host:~$ less file.txt"))
    }

    @Test
    fun nano_scenario_updates_named_rows() {
        val state = TerminalSpikeRunner().run(TerminalScenarioSuite.nano())

        assertTrue(state.isAlternateScreen)
        assertEquals("GNU nano 7.2", state.renderRows[0].toPlainText().trim())
        assertEquals("buffer.txt", state.renderRows[2].toPlainText().trim())
    }

    @Test
    fun tmux_scenario_renders_status_line() {
        val state = TerminalSpikeRunner().run(TerminalScenarioSuite.tmux())

        val lastScreenRow = state.renderRows.last().toPlainText()
        assertTrue(lastScreenRow.contains("0:bash*"))
        assertTrue(lastScreenRow.contains("12:34"))
    }

    @Test
    fun palette256_scenario_applies_foreground_and_background_palette_indices() {
        val state = TerminalSpikeRunner().run(TerminalScenarioSuite.palette256())

        val coloredRow = state.renderRows.first()
        assertEquals(TerminalColor.Indexed(196), coloredRow.cells[6].attribute.foregroundColor)
        assertEquals(TerminalColor.Indexed(33), coloredRow.cells[10].attribute.backgroundColor)
    }

    @Test
    fun application_cursor_mode_scenario_updates_renderer_state() {
        val state = TerminalSpikeRunner().run(TerminalScenarioSuite.applicationCursorMode())

        assertTrue(state.isApplicationCursorKeys)
    }

    private fun TerminalRenderRow.toPlainText(): String =
        cells.joinToString(separator = "") { it.char.toString() }
}
