package com.example.privatessh.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privatessh.terminal.TerminalCellPosition
import com.example.privatessh.terminal.TerminalColor
import com.example.privatessh.terminal.TerminalRenderRow
import com.example.privatessh.terminal.TerminalRendererState
import com.example.privatessh.terminal.TerminalSelection
import kotlin.math.floor

@Composable
fun TerminalViewport(
    rendererState: TerminalRendererState,
    selection: TerminalSelection?,
    fontSizeSp: Float = 14f,
    onResize: (columns: Int, rows: Int) -> Unit,
    onSelectionStart: (TerminalCellPosition) -> Unit,
    onSelectionDrag: (TerminalCellPosition) -> Unit,
    onSelectionClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        val columns = maxOf(20, (maxWidth.value / (fontSizeSp * 0.65f)).toInt())
        val rows = maxOf(6, (maxHeight.value / (fontSizeSp * 1.45f)).toInt())
        val charWidthPx = with(density) { fontSizeSp.sp.toPx() * 0.65f }
        val lineHeightPx = with(density) { fontSizeSp.sp.toPx() * 1.45f }

        LaunchedEffect(columns, rows) {
            onResize(columns, rows)
        }

        LaunchedEffect(rendererState.renderRows.size, rendererState.cursorRow, selection) {
            if (selection == null && rendererState.renderRows.isNotEmpty()) {
                listState.animateScrollToItem(rendererState.renderRows.lastIndex)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selection) {
                    detectTapGestures(
                        onTap = {
                            if (selection != null) {
                                onSelectionClear()
                            }
                        }
                    )
                }
                .pointerInput(rendererState, selection, charWidthPx, lineHeightPx) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            mapOffsetToCell(
                                offset = offset,
                                rendererState = rendererState,
                                charWidthPx = charWidthPx,
                                lineHeightPx = lineHeightPx,
                                firstVisibleIndex = listState.firstVisibleItemIndex,
                                firstVisibleScrollOffset = listState.firstVisibleItemScrollOffset
                            )?.let(onSelectionStart)
                        },
                        onDrag = { change, _ ->
                            mapOffsetToCell(
                                offset = change.position,
                                rendererState = rendererState,
                                charWidthPx = charWidthPx,
                                lineHeightPx = lineHeightPx,
                                firstVisibleIndex = listState.firstVisibleItemIndex,
                                firstVisibleScrollOffset = listState.firstVisibleItemScrollOffset
                            )?.let(onSelectionDrag)
                        }
                    )
                }
        ) {
            items(rendererState.renderRows.size) { rowIndex ->
                val row = rendererState.renderRows[rowIndex]
                Text(
                    text = buildRowString(
                        row = row,
                        rowIndex = rowIndex,
                        selection = selection,
                        highlightCursor = rendererState.isCursorVisible &&
                            selection == null &&
                            rowIndex == rendererState.cursorRow,
                        cursorCol = rendererState.cursorCol
                    ),
                    color = Color(0xFFE6E6E6),
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSizeSp.sp,
                    softWrap = false,
                    maxLines = 1
                )
            }
        }
    }
}

private fun buildRowString(
    row: TerminalRenderRow,
    rowIndex: Int,
    selection: TerminalSelection?,
    highlightCursor: Boolean,
    cursorCol: Int
) = buildAnnotatedString {
    row.cells.forEachIndexed { index, cell ->
        val defaultForeground = Color(0xFFE6E6E6)
        val defaultBackground = Color.Black
        var foreground = cell.attribute.foregroundColor.toComposeColor(defaultForeground)
        var background = cell.attribute.backgroundColor.toComposeColor(defaultBackground)

        if (cell.attribute.reverse) {
            val originalForeground = foreground
            foreground = background
            background = originalForeground
        }

        if (selection?.contains(rowIndex, index) == true) {
            val selectedBackground = Color(0xFF28527A)
            foreground = foreground.takeUnless { it == Color.Unspecified } ?: defaultForeground
            background = selectedBackground
        } else if (highlightCursor && index == cursorCol) {
            val originalForeground = foreground
            foreground = background.takeUnless { it == Color.Black } ?: Color.Black
            background = originalForeground.takeUnless { it == Color.Black } ?: Color(0xFFE6E6E6)
        }

        withStyle(
            SpanStyle(
                color = foreground,
                background = background,
                fontWeight = if (cell.attribute.bold) FontWeight.Bold else null,
                fontStyle = if (cell.attribute.italic) FontStyle.Italic else null,
                textDecoration = when {
                    cell.attribute.underline && cell.attribute.strikethrough ->
                        TextDecoration.Underline + TextDecoration.LineThrough

                    cell.attribute.underline -> TextDecoration.Underline
                    cell.attribute.strikethrough -> TextDecoration.LineThrough
                    else -> null
                }
            )
        ) {
            append(cell.char)
        }
    }
}

private fun mapOffsetToCell(
    offset: Offset,
    rendererState: TerminalRendererState,
    charWidthPx: Float,
    lineHeightPx: Float,
    firstVisibleIndex: Int,
    firstVisibleScrollOffset: Int
): TerminalCellPosition? {
    if (rendererState.renderRows.isEmpty()) {
        return null
    }

    val absoluteRow = firstVisibleIndex + floor((offset.y + firstVisibleScrollOffset) / lineHeightPx).toInt()
    val absoluteColumn = floor(offset.x / charWidthPx).toInt()

    return TerminalCellPosition(
        row = absoluteRow.coerceIn(0, rendererState.renderRows.lastIndex),
        column = absoluteColumn.coerceIn(0, rendererState.columns - 1)
    )
}

private fun TerminalColor.toComposeColor(defaultColor: Color): Color =
    when (this) {
        TerminalColor.Default -> defaultColor
        is TerminalColor.Indexed -> xtermColor(value)
    }

private fun xtermColor(index: Int): Color =
    when (index) {
        0 -> Color(0xFF000000)
        1 -> Color(0xFFCD3131)
        2 -> Color(0xFF0DBC79)
        3 -> Color(0xFFE5E510)
        4 -> Color(0xFF2472C8)
        5 -> Color(0xFFBC3FBC)
        6 -> Color(0xFF11A8CD)
        7 -> Color(0xFFE5E5E5)
        8 -> Color(0xFF666666)
        9 -> Color(0xFFF14C4C)
        10 -> Color(0xFF23D18B)
        11 -> Color(0xFFF5F543)
        12 -> Color(0xFF3B8EEA)
        13 -> Color(0xFFD670D6)
        14 -> Color(0xFF29B8DB)
        15 -> Color(0xFFFFFFFF)
        in 16..231 -> {
            val adjusted = index - 16
            val red = adjusted / 36
            val green = (adjusted % 36) / 6
            val blue = adjusted % 6
            rgbColor(
                channel(red),
                channel(green),
                channel(blue)
            )
        }
        in 232..255 -> {
            val shade = 8 + ((index - 232) * 10)
            rgbColor(shade, shade, shade)
        }
        else -> Color.Unspecified
    }

private fun channel(level: Int): Int =
    if (level == 0) {
        0
    } else {
        55 + (level * 40)
    }

private fun rgbColor(red: Int, green: Int, blue: Int): Color =
    Color(
        red = red / 255f,
        green = green / 255f,
        blue = blue / 255f,
        alpha = 1f
    )
