package com.dlx.sshterm.terminal

/**
 * A segment of text with a specific attribute.
 */
data class TextSegment(
    val text: String,
    val attribute: TerminalTextAttribute
) {
    /**
     * Returns true if this segment is empty.
     */
    fun isEmpty(): Boolean {
        return text.isEmpty()
    }

    /**
     * Returns the length of the text.
     */
    fun length(): Int {
        return text.length
    }

    /**
     * Splits this segment into two parts at the specified position.
     */
    fun splitAt(position: Int): Pair<TextSegment, TextSegment?> {
        return if (position >= text.length) {
            this to null
        } else if (position <= 0) {
            TextSegment("", attribute) to this
        } else {
            TextSegment(text.take(position), attribute) to
                    TextSegment(text.drop(position), attribute)
        }
    }
}
