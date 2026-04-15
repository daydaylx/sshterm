package com.dlx.sshterm.ssh

import com.dlx.sshterm.domain.model.SessionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellBootstrapPlannerTest {

    @Test
    fun buildPlan_without_tmux_auto_attach_returns_plain_shell() {
        val plan = ShellBootstrapPlanner.buildPlan(
            SessionPolicy(tmuxAutoAttach = false, tmuxSessionName = "ops")
        )

        assertNull(plan.command)
        assertEquals(SessionShellMode.SHELL, plan.initialStatus.mode)
    }

    @Test
    fun buildPlan_with_blank_session_name_uses_main_default() {
        val plan = ShellBootstrapPlanner.buildPlan(
            SessionPolicy(tmuxAutoAttach = true, tmuxSessionName = "   ")
        )

        requireNotNull(plan.command)
        assertTrue(plan.command.contains("tmux attach -t 'main'"))
        assertTrue(plan.command.contains(ShellBootstrapPlanner.TMUX_FALLBACK_MARKER))
        assertEquals(SessionShellMode.TMUX_REQUESTED, plan.initialStatus.mode)
    }

    @Test
    fun buildPlan_quotes_tmux_session_name_for_shell_execution() {
        val plan = ShellBootstrapPlanner.buildPlan(
            SessionPolicy(tmuxAutoAttach = true, tmuxSessionName = "ops'prod")
        )

        requireNotNull(plan.command)
        assertTrue(plan.command.contains("ops'\"'\"'prod"))
    }

    @Test
    fun buildPlan_with_null_session_name_uses_main_default() {
        val plan = ShellBootstrapPlanner.buildPlan(
            SessionPolicy(tmuxAutoAttach = true, tmuxSessionName = null)
        )

        requireNotNull(plan.command)
        assertTrue(plan.command.contains("tmux attach -t 'main'"))
    }

    @Test
    fun buildPlan_with_empty_session_name_uses_main_default() {
        val plan = ShellBootstrapPlanner.buildPlan(
            SessionPolicy(tmuxAutoAttach = true, tmuxSessionName = "")
        )

        requireNotNull(plan.command)
        assertTrue(plan.command.contains("tmux attach -t 'main'"))
    }

    @Test
    fun buildPlan_with_valid_session_name_uses_it() {
        val plan = ShellBootstrapPlanner.buildPlan(
            SessionPolicy(tmuxAutoAttach = true, tmuxSessionName = "dev")
        )

        requireNotNull(plan.command)
        assertTrue(plan.command.contains("tmux attach -t 'dev'"))
        assertTrue(plan.command.contains("tmux new -s 'dev'"))
    }

    @Test
    fun buildPlan_command_ends_with_newline() {
        val plan = ShellBootstrapPlanner.buildPlan(
            SessionPolicy(tmuxAutoAttach = true, tmuxSessionName = "main")
        )

        requireNotNull(plan.command)
        assertTrue(plan.command.endsWith("\n"))
    }

    @Test
    fun buildPlan_command_contains_fallback_marker_in_both_branches() {
        val plan = ShellBootstrapPlanner.buildPlan(
            SessionPolicy(tmuxAutoAttach = true, tmuxSessionName = "main")
        )

        requireNotNull(plan.command)
        val occurrences = plan.command.split(ShellBootstrapPlanner.TMUX_FALLBACK_MARKER).size - 1
        assertEquals(2, occurrences)
    }

    // ── normalizeTmuxSessionName ──────────────────────────────────────────────

    @Test
    fun normalizeTmuxSessionName_returnsDefault_for_null() {
        assertEquals(
            ShellBootstrapPlanner.DEFAULT_TMUX_SESSION_NAME,
            ShellBootstrapPlanner.normalizeTmuxSessionName(null)
        )
    }

    @Test
    fun normalizeTmuxSessionName_returnsDefault_for_empty() {
        assertEquals(
            ShellBootstrapPlanner.DEFAULT_TMUX_SESSION_NAME,
            ShellBootstrapPlanner.normalizeTmuxSessionName("")
        )
    }

    @Test
    fun normalizeTmuxSessionName_returnsDefault_for_whitespaceOnly() {
        assertEquals(
            ShellBootstrapPlanner.DEFAULT_TMUX_SESSION_NAME,
            ShellBootstrapPlanner.normalizeTmuxSessionName("   ")
        )
    }

    @Test
    fun normalizeTmuxSessionName_trimsWhitespaceFromValidName() {
        assertEquals("ops", ShellBootstrapPlanner.normalizeTmuxSessionName("  ops  "))
    }

    @Test
    fun normalizeTmuxSessionName_returnsNameUnchanged_whenAlreadyTrimmed() {
        assertEquals("prod-session", ShellBootstrapPlanner.normalizeTmuxSessionName("prod-session"))
    }
}
