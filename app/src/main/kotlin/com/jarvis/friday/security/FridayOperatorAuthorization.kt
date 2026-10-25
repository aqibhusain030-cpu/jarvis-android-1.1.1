package com.jarvis.friday.security

import com.jarvis.core.capability.OperatorAuthorization
import com.jarvis.security.SecurityUnlockManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FridayOperatorAuthorization @Inject constructor(
    private val unlock: SecurityUnlockManager
) : OperatorAuthorization {
    override fun consume(): Boolean = unlock.consume()
}
