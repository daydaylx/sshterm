package com.dlx.sshterm.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HostProfileTest {

    private fun validProfile() = HostProfile(
        name = "My Server",
        host = "192.168.1.1",
        port = 22,
        user = "admin",
        authType = AuthType.PASSWORD
    )

    // ── isValid ───────────────────────────────────────────────────────────────

    @Test
    fun isValid_returnsTrueForCompleteProfile() {
        assertTrue(validProfile().isValid())
    }

    @Test
    fun isValid_returnsFalseForBlankName() {
        assertFalse(validProfile().copy(name = "").isValid())
        assertFalse(validProfile().copy(name = "   ").isValid())
    }

    @Test
    fun isValid_returnsFalseForBlankHost() {
        assertFalse(validProfile().copy(host = "").isValid())
        assertFalse(validProfile().copy(host = " ").isValid())
    }

    @Test
    fun isValid_returnsFalseForBlankUser() {
        assertFalse(validProfile().copy(user = "").isValid())
        assertFalse(validProfile().copy(user = "  ").isValid())
    }

    @Test
    fun isValid_returnsFalseForPortZero() {
        assertFalse(validProfile().copy(port = 0).isValid())
    }

    @Test
    fun isValid_returnsFalseForNegativePort() {
        assertFalse(validProfile().copy(port = -1).isValid())
    }

    @Test
    fun isValid_returnsFalseForPortAboveMax() {
        assertFalse(validProfile().copy(port = 65536).isValid())
        assertFalse(validProfile().copy(port = Int.MAX_VALUE).isValid())
    }

    @Test
    fun isValid_returnsTrueForPortAtMinBoundary() {
        assertTrue(validProfile().copy(port = HostProfile.MIN_PORT).isValid())
    }

    @Test
    fun isValid_returnsTrueForPortAtMaxBoundary() {
        assertTrue(validProfile().copy(port = HostProfile.MAX_PORT).isValid())
    }

    // ── getHostWithPort ───────────────────────────────────────────────────────

    @Test
    fun getHostWithPort_returnsCorrectFormat() {
        assertEquals("example.com:2222", validProfile().copy(host = "example.com", port = 2222).getHostWithPort())
    }

    @Test
    fun getHostWithPort_withDefaultPort() {
        assertEquals("192.168.1.1:22", validProfile().getHostWithPort())
    }

    @Test
    fun getHostWithPort_withIpv6Address() {
        assertEquals("::1:22", validProfile().copy(host = "::1").getHostWithPort())
    }

    // ── getDisplayName ────────────────────────────────────────────────────────

    @Test
    fun getDisplayName_returnsUserAtHost() {
        assertEquals("root@prod.server.io", validProfile().copy(user = "root", host = "prod.server.io").getDisplayName())
    }

    @Test
    fun getDisplayName_withDefaultValues() {
        assertEquals("admin@192.168.1.1", validProfile().getDisplayName())
    }

    // ── isTailscale ───────────────────────────────────────────────────────────

    @Test
    fun isTailscale_returnsTrueForTailscaleTarget() {
        assertTrue(validProfile().copy(targetType = NetworkTargetType.TAILSCALE).isTailscale())
    }

    @Test
    fun isTailscale_returnsFalseForDirectTarget() {
        assertFalse(validProfile().copy(targetType = NetworkTargetType.DIRECT).isTailscale())
    }

    @Test
    fun isTailscale_returnsFalseByDefault() {
        assertFalse(validProfile().isTailscale())
    }

    // ── generateId ────────────────────────────────────────────────────────────

    @Test
    fun generateId_producesUniqueIds() {
        val id1 = HostProfile.generateId()
        val id2 = HostProfile.generateId()
        assertNotEquals(id1, id2)
    }

    @Test
    fun generateId_producesNonBlankId() {
        assertTrue(HostProfile.generateId().isNotBlank())
    }
}
