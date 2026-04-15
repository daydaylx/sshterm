package com.dlx.sshterm.ui.terminal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlx.sshterm.terminal.TerminalCellPosition
import com.dlx.sshterm.terminal.TerminalColor
import com.dlx.sshterm.terminal.TerminalRenderRow
import com.dlx.sshterm.terminal.TerminalRendererState
import com.dlx.sshterm.terminal.TerminalSelection
import com.dlx.sshterm.ui.theme.TerminalBackground
import com.dlx.sshterm.ui.theme.TerminalForeground
import com.dlx.sshterm.ui.theme.TerminalSelection as TerminalSelectionColor
import kotlin.math.floor

@Composable
fun TerminalViewport(
    rendererState: TerminalRendererState,
    selection: TerminalSelection?,
    fontSizeSp: Float = 14f,
    onResize: (columns: Int, rows: Int) -> Unit,
    onTapTerminal: () -> Unit,
    onSelectionStart: (TerminalCellPosition) -> Unit,
    onSelectionDrag: (TerminalCellPosition) -> Unit,
    onSelectionClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    // Zoom state
    var currentFontSizeSp by remember(fontSizeSp) { mutableFloatStateOf(fontSizeSp) }
    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        currentFontSizeSp = (currentFontSizeSp * zoomChange).coerceIn(8f, 32f)
    }

    // Scroll state (in rows)
    var scrollOffsetRows by remember { mutableFloatStateOf(0f) }
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .transformable(state = transformableState)
    ) {
        val horizontalPaddingDp = 8.dp
        val verticalPaddingDp = 8.dp
        val horizontalPaddingPx = with(density) { horizontalPaddingDp.toPx() }
        val verticalPaddingPx = with(density) { verticalPaddingDp.toPx() }
        
        val monoStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = currentFontSizeSp.sp,
            color = TerminalForeground
        )
        
        // Measure character size
        val charLayout = remember(currentFontSizeSp) {
            textMeasurer.measure(AnnotatedString("W"), style = monoStyle)
        }
        val charWidthPx = charLayout.size.width.toFloat()
        val charHeightPx = charLayout.size.height.toFloat()
        
        val usableWidth = constraints.maxWidth.toFloat() - (horizontalPaddingPx * 2f)
        val usableHeight = constraints.maxHeight.toFloat() - (verticalPaddingPx * 2f)
        
        val columns = maxOf(20, (usableWidth / charWidthPx).toInt())
        val rows = maxOf(6, (usableHeight / charHeightPx).toInt())

        LaunchedEffect(columns, rows) {
            onResize(columns, rows)
        }

        // Auto-scroll to bottom when content changes if not selecting
        LaunchedEffect(rendererState.renderRows.size, rendererState.cursorRow, selection) {
            if (selection == null) {
                val lastIndex = rendererState.renderRows.size.toFloat()
                val visibleTop = lastIndex - rows.toFloat()
                scrollOffsetRows = maxOf(0f, visibleTop)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (selection != null) onSelectionClear()
                            onTapTerminal()
                        }
                    )
                }
                .pointerInput(rendererState, charWidthPx, charHeightPx, horizontalPaddingPx, verticalPaddingPx) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            mapOffsetToCell(
                                offset = offset,
                                rendererState = rendererState,
                                charWidthPx = charWidthPx,
                                charHeightPx = charHeightPx,
                                scrollOffsetRows = scrollOffsetRows,
                                horizontalPaddingPx = horizontalPaddingPx,
                                verticalPaddingPx = verticalPaddingPx
                            )?.let(onSelectionStart)
                        },
                        onDrag = { change, _ ->
                            mapOffsetToCell(
                                offset = change.position,
                                rendererState = rendererState,
                                charWidthPx = charWidthPx,
                                charHeightPx = charHeightPx,
                                scrollOffsetRows = scrollOffsetRows,
                                horizontalPaddingPx = horizontalPaddingPx,
                                verticalPaddingPx = verticalPaddingPx
                            )?.let(onSelectionDrag)
                        }
                    )
                }
                .pointerInput(rendererState.renderRows.size, rows) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val dragAmount = event.changes.first().scrollDelta.y
                            if (dragAmount != 0f) {
                                val newOffset = (scrollOffsetRows + dragAmount).coerceIn(
                                    0f, 
                                    maxOf(0f, rendererState.renderRows.size.toFloat() - rows.toFloat())
                                )
                                scrollOffsetRows = newOffset
                                event.changes.first().consume()
                            }
                        }
                    }
                }
        ) {
            val startRow = floor(scrollOffsetRows).toInt().coerceIn(0, rendererState.renderRows.size)
            val endRow = (startRow + rows + 1).coerceAtMost(rendererState.renderRows.size)
            
            for (rowIndex in startRow until endRow) {
                val row = rendererState.renderRows[rowIndex]
                val yOffset = (rowIndex.toFloat() - scrollOffsetRows) * charHeightPx + verticalPaddingPx
                
                val annotatedString = buildRowString(
                    row = row,
                    rowIndex = rowIndex,
                    selection = selection,
                    highlightCursor = rendererState.isCursorVisible &&
                        selection == null &&
                        rowIndex == rendererState.cursorRow,
                    cursorCol = rendererState.cursorCol
                )
                
                drawText(
                    textMeasurer = textMeasurer,
                    text = annotatedString,
                    style = monoStyle,
                    topLeft = Offset(horizontalPaddingPx, yOffset)
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
        val defaultForeground = TerminalForeground
        val defaultBackground = TerminalBackground
        var foreground = cell.attribute.foregroundColor.toComposeColor(defaultForeground)
        var background = cell.attribute.backgroundColor.toComposeColor(defaultBackground)

        if (cell.attribute.reverse) {
            val originalForeground = foreground
            foreground = background
            background = originalForeground
        }

        if (selection?.contains(rowIndex, index) == true) {
            foreground = foreground.takeUnless { it == Color.Unspecified } ?: defaultForeground
            background = TerminalSelectionColor
        } else if (highlightCursor && index == cursorCol) {
            val originalForeground = foreground
            foreground = background.takeUnless { it == TerminalBackground } ?: TerminalBackground
            background = originalForeground.takeUnless { it == TerminalBackground } ?: TerminalForeground
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
    charHeightPx: Float,
    scrollOffsetRows: Float,
    horizontalPaddingPx: Float,
    verticalPaddingPx: Float
): TerminalCellPosition? {
    if (rendererState.renderRows.isEmpty()) return null

    val adjustedX = (offset.x - horizontalPaddingPx).coerceAtLeast(0f)
    val adjustedY = (offset.y - verticalPaddingPx).coerceAtLeast(0f)
    
    val absoluteRow = floor(adjustedY / charHeightPx + scrollOffsetRows).toInt()
    val absoluteColumn = floor(adjustedX / charWidthPx).toInt()

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
    if (level == 0) 0 else 55 + (level * 40)

private fun rgbColor(red: Int, green: Int, blue: Int): Color =
    Color(
        red = red / 255f,
        green = green / 255f,
        blue = blue / 255f,
        alpha = 1f
    )

