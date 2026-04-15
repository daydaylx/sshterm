package com.dlx.sshterm.terminal

/**
 * A single terminal cell with one displayed character and its attributes.
 */
data class TerminalCell(
    val char: Char = ' ',
    val attribute: TerminalTextAttribute = TerminalTextAttribute.DEFAULT
) {
    companion object {
        fun blank(): TerminalCell = TerminalCell()
    }
}

/**
 * A renderable terminal row.
 */
data class TerminalRenderRow(
    val cells: List<TerminalCell>
) {
    companion object {
        fun blank(columns: Int): TerminalRenderRow =
            TerminalRenderRow(List(columns) { TerminalCell.blank() })
    }
}
