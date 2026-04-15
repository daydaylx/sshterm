package com.dlx.sshterm.terminal

import com.dlx.sshterm.terminal.input.KeyMapper
import com.dlx.sshterm.terminal.input.SpecialKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalModeAndInputTest {

    @Test
    fun decckm_switches_arrow_keys_to_ss3_sequences() {
        val keyMapper = KeyMapper()

        assertArrayEquals(
            byteArrayOf(0x1B.toByte(), '['.code.toByte(), 'A'.code.toByte()),
            keyMapper.mapSpecialKey(SpecialKey.ARROW_UP, applicationCursorKeys = false)
        )
        assertArrayEquals(
            byteArrayOf(0x1B.toByte(), 'O'.code.toByte(), 'A'.code.toByte()),
            keyMapper.mapSpecialKey(SpecialKey.ARROW_UP, applicationCursorKeys = true)
        )
        assertArrayEquals(
            byteArrayOf(0x1B.toByte(), 'O'.code.toByte(), 'H'.code.toByte()),
            keyMapper.mapSpecialKey(SpecialKey.HOME, applicationCursorKeys = true)
        )
    }

    @Test
    fun key_events_map_backspace_delete_and_enter_sequences() {
        val keyMapper = KeyMapper()

        assertArrayEquals(
            byteArrayOf(0x7F.toByte()),
            keyMapper.mapSpecialKey(keyMapper.getSpecialKey(android.view.KeyEvent.KEYCODE_DEL)!!)
        )
        assertArrayEquals(
            byteArrayOf(0x1B.toByte(), '['.code.toByte(), '3'.code.toByte(), '~'.code.toByte()),
            keyMapper.mapSpecialKey(keyMapper.getSpecialKey(android.view.KeyEvent.KEYCODE_FORWARD_DEL)!!)
        )
        assertArrayEquals(
            byteArrayOf(0x0D.toByte()),
            keyMapper.mapSpecialKey(keyMapper.getSpecialKey(android.view.KeyEvent.KEYCODE_ENTER)!!)
        )
    }

    @Test
    fun decawm_false_overwrites_last_column_instead_of_wrapping() {
        val state = TerminalEmulator(columns = 4, rows = 2)
            .apply { feed("\u001B[?7lABCDE".toByteArray()) }
            .snapshot()

        assertFalse(state.isAutoWrapEnabled)
        assertEquals("ABCE", state.renderRows[0].toPlainText().take(4))
        assertEquals("", state.renderRows[1].toPlainText().trim())
    }

    @Test
    fun fullscreen_sequences_scroll_and_move_cursor_within_region() {
        val state = TerminalEmulator(columns = 6, rows = 4)
            .apply {
                feed("1\r\n2\r\n3\r\n4".toByteArray())
                feed("\u001B[2;4r".toByteArray())
                feed("\u001B[2;1H\u001B[S".toByteArray())
                feed("\u001B[3;1H\u001B[T".toByteArray())
                feed("\u001B[4dZ".toByteArray())
            }
            .snapshot()

        assertEquals("1", state.renderRows[0].toPlainText().trim())
        assertEquals("", state.renderRows[1].toPlainText().trim())
        assertEquals("3", state.renderRows[2].toPlainText().trim())
        assertTrue(state.renderRows[3].toPlainText().trim().startsWith("Z"))
    }

    @Test
    fun escape_index_next_line_and_reverse_index_are_interpreted() {
        val state = TerminalEmulator(columns = 5, rows = 3)
            .apply {
                feed("A\u001BD".toByteArray())
                feed("B\u001BE".toByteArray())
                feed("C".toByteArray())
                feed("\u001B[1;3r\u001B[1;1H\u001BM".toByteArray())
            }
            .snapshot()

        assertEquals("A", state.renderRows[1].toPlainText().trim())
        assertEquals("B", state.renderRows[2].toPlainText().trim())
    }

    private fun TerminalRenderRow.toPlainText(): String =
        cells.joinToString(separator = "") { it.char.toString() }
}
