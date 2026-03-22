package com.example.privatessh.terminal

/**
 * Documents which terminal features are implemented in the current engine slice.
 */
object TerminalFeatureMatrix {

    enum class SupportLevel {
        SUPPORTED,
        IGNORED,
        DEFERRED
    }

    val features: Map<String, SupportLevel> = linkedMapOf(
        "printable_utf8" to SupportLevel.SUPPORTED,
        "cr_lf_bs_tab" to SupportLevel.SUPPORTED,
        "cursor_movement" to SupportLevel.SUPPORTED,
        "erase_display_line" to SupportLevel.SUPPORTED,
        "scroll_region" to SupportLevel.SUPPORTED,
        "scroll_up_down" to SupportLevel.SUPPORTED,
        "index_nextline_reverseindex" to SupportLevel.SUPPORTED,
        "alternate_screen" to SupportLevel.SUPPORTED,
        "cursor_visibility" to SupportLevel.SUPPORTED,
        "basic_sgr_colors" to SupportLevel.SUPPORTED,
        "extended_256_colors" to SupportLevel.SUPPORTED,
        "application_cursor_keys" to SupportLevel.SUPPORTED,
        "autowrap_mode" to SupportLevel.SUPPORTED,
        "osc_window_title" to SupportLevel.IGNORED,
        "vim_fullscreen" to SupportLevel.DEFERRED,
        "htop_fullscreen" to SupportLevel.DEFERRED
    )
}
