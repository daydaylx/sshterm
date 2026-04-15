package com.dlx.sshterm.ui.terminal

import android.content.Context
import android.graphics.Rect
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.dlx.sshterm.terminal.input.SpecialKey

@Stable
class TerminalImeBridgeController internal constructor() {
    internal var requestFocusHandler: (() -> Unit)? = null

    fun requestFocus() {
        requestFocusHandler?.invoke()
    }
}

@Composable
fun rememberTerminalImeBridgeController(): TerminalImeBridgeController {
    return remember { TerminalImeBridgeController() }
}

@Composable
fun TerminalImeBridge(
    controller: TerminalImeBridgeController,
    onTextInput: (String) -> Unit,
    onKeyEvent: (KeyEvent) -> Boolean,
    onSpecialKey: (SpecialKey) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TerminalInputView(context).apply {
                textInputHandler = onTextInput
                keyEventHandler = onKeyEvent
                specialKeyHandler = onSpecialKey
            }
        },
        update = { view ->
            view.textInputHandler = onTextInput
            view.keyEventHandler = onKeyEvent
            view.specialKeyHandler = onSpecialKey
            controller.requestFocusHandler = view::focusAndShowKeyboard
        }
    )
}

private class TerminalInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var textInputHandler: (String) -> Unit = {}
    var keyEventHandler: (KeyEvent) -> Boolean = { false }
    var specialKeyHandler: (SpecialKey) -> Unit = {}

    private var composingText = ""

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        alpha = 0f
        setWillNotDraw(true)
        overScrollMode = OVER_SCROLL_NEVER
        importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or
            EditorInfo.IME_FLAG_NO_FULLSCREEN or
            EditorInfo.IME_ACTION_NONE
        outAttrs.initialSelStart = 0
        outAttrs.initialSelEnd = 0

        return object : BaseInputConnection(this, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                applyCommittedText(text?.toString().orEmpty())
                return true
            }

            override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                applyCompositionUpdate(text?.toString().orEmpty())
                return true
            }

            override fun finishComposingText(): Boolean {
                composingText = ""
                return true
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength > 0) {
                    repeat(beforeLength) {
                        if (composingText.isNotEmpty()) {
                            composingText = composingText.dropLast(1)
                        }
                        specialKeyHandler(SpecialKey.BACKSPACE)
                    }
                }
                if (afterLength > 0) {
                    repeat(afterLength) {
                        specialKeyHandler(SpecialKey.DELETE)
                    }
                }
                return true
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL && composingText.isNotEmpty()) {
                    composingText = composingText.dropLast(1)
                }
                return keyEventHandler(event)
            }

            override fun performEditorAction(actionCode: Int): Boolean {
                if (actionCode != EditorInfo.IME_ACTION_NONE) {
                    composingText = ""
                    specialKeyHandler(SpecialKey.ENTER)
                    return true
                }
                return false
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return keyEventHandler(event) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return keyEventHandler(event) || super.onKeyUp(keyCode, event)
    }

    override fun onFocusChanged(
        gainFocus: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (gainFocus) {
            post {
                inputMethodManager()?.restartInput(this)
            }
        }
    }

    fun focusAndShowKeyboard() {
        if (!hasFocus()) {
            requestFocus()
        }
        post {
            inputMethodManager()?.restartInput(this)
            inputMethodManager()?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun applyCommittedText(text: String) {
        if (text.isEmpty()) {
            composingText = ""
            return
        }

        if (composingText.isNotEmpty()) {
            val commonPrefixLength = composingText.commonPrefixWith(text).length
            repeat(composingText.length - commonPrefixLength) {
                specialKeyHandler(SpecialKey.BACKSPACE)
            }
            dispatchText(text.substring(commonPrefixLength))
            composingText = ""
            return
        }

        dispatchText(text)
    }

    private fun applyCompositionUpdate(text: String) {
        if (text == composingText) {
            return
        }

        val previous = composingText
        val commonPrefixLength = previous.commonPrefixWith(text).length
        repeat(previous.length - commonPrefixLength) {
            specialKeyHandler(SpecialKey.BACKSPACE)
        }
        dispatchText(text.substring(commonPrefixLength))
        composingText = text
    }

    private fun dispatchText(text: String) {
        if (text.isNotEmpty()) {
            textInputHandler(text)
        }
    }

    private fun inputMethodManager(): InputMethodManager? {
        return context.getSystemService(InputMethodManager::class.java)
    }
}
