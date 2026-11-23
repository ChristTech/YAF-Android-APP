package com.example

import com.example.core.security.PasswordHasher
import com.example.core.security.Permission
import com.example.core.security.RoleEvaluator
import com.example.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityTest {

    @Test
    fun `password hasher generates unique salts`() {
        val salt1 = PasswordHasher.generateSalt()
        val salt2 = PasswordHasher.generateSalt()
        assertNotNullOrEmpty(salt1)
        assertNotNullOrEmpty(salt2)
        assertNotEquals(salt1, salt2)
    }

    @Test
    fun `password hash and verify succeeds with correct password`() {
        val password = "FaithSecure#2026"
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)

        val isValid = PasswordHasher.verifyPassword(password, salt, hash)
        assertTrue("Verification must succeed with matching credentials", isValid)
    }

    @Test
    fun `password verify rejects incorrect password`() {
        val password = "FaithSecure#2026"
        val wrongPassword = "WrongPassword#2026"
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)

        val isValid = PasswordHasher.verifyPassword(wrongPassword, salt, hash)
        assertFalse("Verification must fail with invalid password", isValid)
    }

    @Test
    fun `role evaluator enforces correct permissions for administrator`() {
        val adminPermissions = RoleEvaluator.getPermissions(UserRole.ADMINISTRATOR)
        assertTrue(adminPermissions.contains(Permission.MANAGE_USERS))
        assertTrue(adminPermissions.contains(Permission.APPROVE_REGISTRATIONS))
        assertTrue(adminPermissions.contains(Permission.VIEW_AUDIT_LOGS))
        assertTrue(adminPermissions.contains(Permission.VIEW_ALL_FINANCES))
    }

    @Test
    fun `role evaluator restricts youth member permissions appropriately`() {
        val memberRole = UserRole.YOUTH_MEMBER
        assertTrue(RoleEvaluator.hasPermission(memberRole, Permission.VIEW_PROGRAMMES))
        assertTrue(RoleEvaluator.hasPermission(memberRole, Permission.REGISTER_EVENTS))
        assertTrue(RoleEvaluator.hasPermission(memberRole, Permission.PLAY_GAMES))

        // Youth member should NOT have administrative capabilities
        assertFalse(RoleEvaluator.hasPermission(memberRole, Permission.MANAGE_USERS))
        assertFalse(RoleEvaluator.hasPermission(memberRole, Permission.APPROVE_REGISTRATIONS))
        assertFalse(RoleEvaluator.hasPermission(memberRole, Permission.VIEW_AUDIT_LOGS))
        assertFalse(RoleEvaluator.hasPermission(memberRole, Permission.PUBLISH_ANNOUNCEMENTS))
    }

    @Test
    fun `guest role has strictly minimal public permissions`() {
        val guestRole = UserRole.GUEST
        assertTrue(RoleEvaluator.hasPermission(guestRole, Permission.VIEW_PROGRAMMES))
        assertFalse(RoleEvaluator.hasPermission(guestRole, Permission.RECORD_FINANCIAL_GIVING))
        assertFalse(RoleEvaluator.hasPermission(guestRole, Permission.MANAGE_EVENTS))
    }

    private fun assertNotNullOrEmpty(value: String?) {
        assertTrue(!value.isNullOrBlank())
    }
}
