package com.example.privatessh.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TerminalViewport(
    terminalOutput: String,
    fontSizeSp: Float = 14f,
    onResize: (columns: Int, rows: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        val columns = maxOf(20, (maxWidth.value / (fontSizeSp * 0.65f)).toInt())
        val rows = maxOf(6, (maxHeight.value / (fontSizeSp * 1.45f)).toInt())

        LaunchedEffect(columns, rows) {
            onResize(columns, rows)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(parseTerminalOutput(terminalOutput)) { line ->
                Text(
                    text = line,
                    color = Color(0xFFE6E6E6),
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSizeSp.sp
                )
            }
        }
    }
}

private fun parseTerminalOutput(output: String): List<String> {
    if (output.isEmpty()) return listOf("")

    val cleaned = buildString(output.length) {
        var index = 0
        while (index < output.length) {
            val char = output[index]
            if (char == '\u001B') {
                index++
                if (index < output.length && output[index] == '[') {
                    index++
                    while (index < output.length && output[index] !in 'A'..'z') {
                        index++
                    }
                }
            } else {
                append(char)
            }
            index++
        }
    }

    val lines = mutableListOf(StringBuilder())
    for (char in cleaned) {
        when (char) {
            '\r' -> lines[lines.lastIndex].clear()
            '\n' -> lines += StringBuilder()
            '\b' -> {
                val line = lines[lines.lastIndex]
                if (line.isNotEmpty()) {
                    line.deleteCharAt(line.lastIndex)
                }
            }
            else -> lines[lines.lastIndex].append(char)
        }
    }

    return lines.map(StringBuilder::toString)
}
