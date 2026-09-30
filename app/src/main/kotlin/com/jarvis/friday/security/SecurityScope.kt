package com.jarvis.friday.security

import java.time.Instant
import java.util.UUID

data class SecurityScope(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val target: String,
    val authorizedByUser: Boolean,
    val expiresAt: Instant
) {
    fun active(now: Instant = Instant.now()): Boolean =
        authorizedByUser && now.isBefore(expiresAt)
}

class SecurityScopeRegistry {
    private val scopes = LinkedHashMap<String, SecurityScope>()

    @Synchronized
    fun enroll(scope: SecurityScope): Boolean {
        if (!scope.authorizedByUser || !scope.active()) return false
        scopes[scope.id] = scope
        return true
    }

    @Synchronized
    fun isAuthorized(target: String, now: Instant = Instant.now()): Boolean =
        scopes.values.any { it.target == target && it.active(now) }

    @Synchronized
    fun activeScopes(now: Instant = Instant.now()): List<SecurityScope> =
        scopes.values.filter { it.active(now) }.toList()
}
