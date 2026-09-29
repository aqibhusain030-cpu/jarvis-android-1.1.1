package com.jarvis.tools

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Central just-in-time runtime permission check for tool capabilities. */
@Singleton
class PermissionGate @Inject constructor(@ApplicationContext private val ctx: Context) {
    fun missing(spec: ToolSpec): List<String> = spec.permissions.filter { permission ->
        ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED
    }
}
