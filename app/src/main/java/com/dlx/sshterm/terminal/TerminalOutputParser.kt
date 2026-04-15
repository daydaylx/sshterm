package com.dlx.sshterm.terminal

/**
 * Incremental parser for terminal byte streams.
 * It preserves parser state across chunk boundaries.
 */
class TerminalOutputParser {

    private enum class ParserState {
        GROUND,
        ESCAPE,
        CSI,
        OSC,
        CHARSET_DESIGNATE  // consumes the single charset designator byte after ESC ( or ESC )
    }

    private var parserState = ParserState.GROUND
    private val textBuffer = StringBuilder()
    private val csiBuffer = StringBuilder()
    private val utf8Buffer = mutableListOf<Byte>()
    private var oscEscSeen = false

    fun feed(bytes: ByteArray): List<TerminalToken> {
        val tokens = mutableListOf<TerminalToken>()
        for (byte in bytes) {
            processByte(byte, tokens)
        }
        flushText(tokens)
        return tokens
    }

    fun reset() {
        parserState = ParserState.GROUND
        textBuffer.clear()
        csiBuffer.clear()
        utf8Buffer.clear()
        oscEscSeen = false
    }


    private fun processByte(byte: Byte, tokens: MutableList<TerminalToken>) {
        when (parserState) {
            ParserState.GROUND -> processGroundByte(byte, tokens)
            ParserState.ESCAPE -> processEscapeByte(byte, tokens)
            ParserState.CSI -> processCsiByte(byte, tokens)
            ParserState.OSC -> processOscByte(byte)
            ParserState.CHARSET_DESIGNATE -> parserState = ParserState.GROUND  // consume designator byte, ignore
        }
    }

    private fun processGroundByte(byte: Byte, tokens: MutableList<TerminalToken>) {
        val value = byte.toInt() and 0xFF

        if (utf8Buffer.isNotEmpty() || value >= 0x80) {
            appendUtf8Byte(byte)
            return
        }

        when (value) {
            0x1B -> {
                flushText(tokens)
                parserState = ParserState.ESCAPE
            }

            0x0D -> {
                flushText(tokens)
                tokens += TerminalToken.Control.CARRIAGE_RETURN
            }

            0x0A -> {
                flushText(tokens)
                tokens += TerminalToken.Control.LINE_FEED
            }

            0x08 -> {
                flushText(tokens)
                tokens += TerminalToken.Control.BACKSPACE
            }

            0x09 -> {
                flushText(tokens)
                tokens += TerminalToken.Control.TAB
            }

            0x07 -> {
                flushText(tokens)
                tokens += TerminalToken.Control.BELL
            }

            in 0x20..0x7E -> textBuffer.append(value.toChar())
            else -> Unit
        }
    }

    private fun processEscapeByte(byte: Byte, tokens: MutableList<TerminalToken>) {
        val char = (byte.toInt() and 0xFF).toChar()
        parserState = when (char) {
            '[' -> {
                csiBuffer.clear()
                ParserState.CSI
            }

            ']' -> {
                oscEscSeen = false
                ParserState.OSC
            }

            '7' -> {
                tokens += TerminalToken.Escape.SAVE_CURSOR
                ParserState.GROUND
            }

            '8' -> {
                tokens += TerminalToken.Escape.RESTORE_CURSOR
                ParserState.GROUND
            }

            'c' -> {
                tokens += TerminalToken.Escape.RESET
                ParserState.GROUND
            }

            'D' -> {
                tokens += TerminalToken.Escape.INDEX
                ParserState.GROUND
            }

            'E' -> {
                tokens += TerminalToken.Escape.NEXT_LINE
                ParserState.GROUND
            }

            'M' -> {
                tokens += TerminalToken.Escape.REVERSE_INDEX
                ParserState.GROUND
            }

            '(', ')' -> ParserState.CHARSET_DESIGNATE
            else -> ParserState.GROUND
        }
    }

    private fun processCsiByte(byte: Byte, tokens: MutableList<TerminalToken>) {
        val char = (byte.toInt() and 0xFF).toChar()
        csiBuffer.append(char)

        if (char in '@'..'~') {
            tokens += parseCsiSequence(csiBuffer.toString())
            csiBuffer.clear()
            parserState = ParserState.GROUND
        }
    }

    private fun processOscByte(byte: Byte) {
        val value = byte.toInt() and 0xFF
        when {
            oscEscSeen && value == '\\'.code -> {
                oscEscSeen = false
                parserState = ParserState.GROUND
            }

            value == 0x07 -> parserState = ParserState.GROUND
            value == 0x1B -> oscEscSeen = true
            else -> oscEscSeen = false
        }
    }

    private fun appendUtf8Byte(byte: Byte) {
        utf8Buffer += byte
        val expectedLength = expectedUtf8Length(utf8Buffer.first())
        if (expectedLength <= 0) {
            utf8Buffer.clear()
            return
        }

        if (utf8Buffer.size == expectedLength) {
            textBuffer.append(utf8Buffer.toByteArray().toString(Charsets.UTF_8))
            utf8Buffer.clear()
        }
    }

    private fun flushText(tokens: MutableList<TerminalToken>) {
        if (textBuffer.isNotEmpty()) {
            tokens += TerminalToken.Text(textBuffer.toString())
            textBuffer.clear()
        }
    }

    private fun parseCsiSequence(sequence: String): TerminalToken.CsiSequence {
        val finalChar = sequence.last()
        var body = sequence.dropLast(1)
        var privateMarker: Char? = null

        if (body.firstOrNull() in listOf('?', '>', '!')) {
            privateMarker = body.first()
            body = body.drop(1)
        }

        val params = if (body.isBlank()) {
            emptyList()
        } else {
            body.split(';').map { part -> part.toIntOrNull() }
        }

        return TerminalToken.CsiSequence(
            privateMarker = privateMarker,
            params = params,
            finalChar = finalChar
        )
    }

    private fun expectedUtf8Length(firstByte: Byte): Int {
        val value = firstByte.toInt() and 0xFF
        return when {
            value and 0b1000_0000 == 0 -> 1
            value and 0b1110_0000 == 0b1100_0000 -> 2
            value and 0b1111_0000 == 0b1110_0000 -> 3
            value and 0b1111_1000 == 0b1111_0000 -> 4
            else -> -1
        }
    }
}
