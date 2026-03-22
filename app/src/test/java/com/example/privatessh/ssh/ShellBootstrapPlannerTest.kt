package com.example.privatessh.ssh

import com.example.privatessh.domain.model.SessionPolicy
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
}
