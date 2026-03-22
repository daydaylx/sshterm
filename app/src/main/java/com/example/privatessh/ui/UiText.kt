package com.example.privatessh.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

sealed interface UiText {
    fun resolve(context: Context): String

    data class Dynamic(val value: String) : UiText {
        override fun resolve(context: Context): String = value
    }

    data class Res(
        @StringRes val resId: Int,
        val formatArgs: List<Any> = emptyList()
    ) : UiText {
        override fun resolve(context: Context): String =
            context.getString(resId, *formatArgs.toTypedArray())
    }

    companion object {
        fun dynamic(value: String): UiText = Dynamic(value)

        fun res(@StringRes resId: Int, vararg formatArgs: Any): UiText =
            Res(resId, formatArgs.toList())
    }
}

@Composable
fun UiText.asString(): String = resolve(LocalContext.current)
