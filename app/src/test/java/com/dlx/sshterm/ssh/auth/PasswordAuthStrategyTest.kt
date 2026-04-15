package com.dlx.sshterm.ssh.auth

import com.dlx.sshterm.diagnostics.SessionDiagnosticsStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PasswordAuthStrategyTest {

    private lateinit var strategy: PasswordAuthStrategy

    @Before
    fun setUp() {
        strategy = PasswordAuthStrategy(SessionDiagnosticsStore())
    }

    @Test
    fun hasPassword_returnsFalseInitially() {
        assertFalse(strategy.hasPassword("host1"))
    }

    @Test
    fun setPassword_allowsHasPasswordToReturnTrue() {
        strategy.setPassword("host1", "secret")
        assertTrue(strategy.hasPassword("host1"))
    }

    @Test
    fun hasPassword_returnsFalseForUnknownHost() {
        strategy.setPassword("host1", "secret")
        assertFalse(strategy.hasPassword("host2"))
    }

    @Test
    fun clearPassword_removesPasswordFromCache() {
        strategy.setPassword("host1", "secret")
        strategy.clearPassword("host1")
        assertFalse(strategy.hasPassword("host1"))
    }

    @Test
    fun clearPassword_isIdempotentWhenNoPasswordExists() {
        strategy.clearPassword("host1") // must not throw
        assertFalse(strategy.hasPassword("host1"))
    }

    @Test
    fun setPassword_withBlankPassword_doesNotStoreEntry() {
        strategy.setPassword("host1", "   ")
        assertFalse(strategy.hasPassword("host1"))
    }

    @Test
    fun setPassword_withEmptyString_doesNotStoreEntry() {
        strategy.setPassword("host1", "")
        assertFalse(strategy.hasPassword("host1"))
    }

    @Test
    fun setPassword_overwritesPreviousPassword() {
        strategy.setPassword("host1", "old")
        strategy.setPassword("host1", "new")
        assertTrue(strategy.hasPassword("host1"))
    }

    @Test
    fun multipleHosts_areIndependent() {
        strategy.setPassword("host1", "pass1")
        strategy.setPassword("host2", "pass2")

        assertTrue(strategy.hasPassword("host1"))
        assertTrue(strategy.hasPassword("host2"))

        strategy.clearPassword("host1")
        assertFalse(strategy.hasPassword("host1"))
        assertTrue(strategy.hasPassword("host2"))
    }

    @Test
    fun clearPassword_thenSetAgain_restoresPassword() {
        strategy.setPassword("host1", "first")
        strategy.clearPassword("host1")
        strategy.setPassword("host1", "second")
        assertTrue(strategy.hasPassword("host1"))
    }
}
