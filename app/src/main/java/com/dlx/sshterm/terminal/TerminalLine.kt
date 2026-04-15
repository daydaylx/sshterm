package com.dlx.sshterm.terminal

/**
 * Represents a single line of terminal text with attributes.
 */
data class TerminalLine(
    val segments: List<TextSegment> = emptyList(),
    val lineBreak: Boolean = true
) {
    /**
     * Returns the plain text without attributes.
     */
    fun toPlainText(): String {
        return segments.joinToString("") { it.text }
    }

    /**
     * Returns the length of the line in characters.
     */
    fun length(): Int {
        return segments.sumOf { it.text.length }
    }

    /**
     * Creates a new line with text truncated to the specified width.
     */
    fun truncate(width: Int): TerminalLine {
        var currentLength = 0
        val truncatedSegments = mutableListOf<TextSegment>()

        for (segment in segments) {
            if (currentLength >= width) break

            val remainingWidth = width - currentLength
            if (segment.text.length <= remainingWidth) {
                truncatedSegments.add(segment)
                currentLength += segment.text.length
            } else {
                truncatedSegments.add(segment.copy(text = segment.text.take(remainingWidth)))
                currentLength = width
            }
        }

        return copy(segments = truncatedSegments)
    }

    /**
     * Creates a new line with text padded to the specified width.
     */
    fun padTo(width: Int): TerminalLine {
        val currentLength = length()
        if (currentLength >= width) return this

        val padding = " ".repeat(width - currentLength)
        return copy(
            segments = segments + listOf(TextSegment(padding, TerminalTextAttribute.DEFAULT))
        )
    }

    companion object {
        /**
         * Creates a plain line from a string.
         */
        fun fromPlainText(text: String): TerminalLine {
            return TerminalLine(
                segments = listOf(TextSegment(text, TerminalTextAttribute.DEFAULT))
            )
        }

        /**
         * Creates an empty line.
         */
        fun empty(): TerminalLine {
            return TerminalLine()
        }
    }
}
