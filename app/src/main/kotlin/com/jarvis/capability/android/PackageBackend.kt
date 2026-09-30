package com.jarvis.capability.android

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.jarvis.core.capability.*

class PackageBackend(private val context: Context) : CapabilityBackend {
    private val packageManager get() = context.packageManager
    override val backendId = "android.package"
    override val capabilities = setOf(
        Capability("package.find", "Find an installed Android application", requiresPackageVisibility = true),
        Capability("package.list_launchable", "List launchable applications", requiresPackageVisibility = true),
        Capability("package.is_installed", "Check whether an application is installed", requiresPackageVisibility = true)
    )

    override suspend fun execute(r: CapabilityRequest): CapabilityResult = when (r.capabilityId) {
        "package.find" -> find(r)
        "package.list_launchable" -> list(r)
        "package.is_installed" -> installed(r)
        else -> CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.UNKNOWN_CAPABILITY, "Unsupported package capability.")
    }

    private fun find(r: CapabilityRequest): CapabilityResult {
        val q = r.arguments["query"]?.trim()?.takeIf { it.isNotEmpty() } ?: return invalid(r)
        val apps = launchable().mapNotNull { app ->
            val label = app.loadLabel(packageManager)?.toString() ?: return@mapNotNull null
            if (label.contains(q, true) || app.packageName.contains(q, true)) label to app.packageName else null
        }.sortedWith(compareBy<Pair<String,String>> { it.first.lowercase() }.thenBy { it.second }).take(MAX_RESULTS)
        return success(r, apps, "Found " + apps.size + " matching application(s).")
    }

    private fun list(r: CapabilityRequest): CapabilityResult {
        val apps = launchable().mapNotNull { app ->
            val label = app.loadLabel(packageManager)?.toString() ?: return@mapNotNull null
            label to app.packageName
        }.sortedWith(compareBy<Pair<String,String>> { it.first.lowercase() }.thenBy { it.second }).take(MAX_RESULTS)
        return success(r, apps, "Found " + apps.size + " launchable application(s).")
    }

    private fun installed(r: CapabilityRequest): CapabilityResult {
        val pkg = r.arguments["package"]?.trim()?.takeIf { it.isNotEmpty() } ?: return invalid(r)
        val found = runCatching {
            packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        }.isSuccess
        return CapabilityResult.Success(
            r.requestId,
            if (found) "The application is installed." else "The application is not installed.",
            mapOf("package" to pkg, "installed" to found.toString())
        )
    }

    private fun success(r: CapabilityRequest, apps: List<Pair<String,String>>, message: String) =
        CapabilityResult.Success(r.requestId, message, buildMap {
            put("count", apps.size.toString())
            apps.forEachIndexed { i, pair ->
                put("app." + i + ".label", pair.first)
                put("app." + i + ".package", pair.second)
            }
        })

    private fun launchable(): List<ApplicationInfo> =
        packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.ResolveInfoFlags.of(0)
        ).map { it.activityInfo.applicationInfo }.distinctBy { it.packageName }

    private fun invalid(r: CapabilityRequest) =
        CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.INVALID_ARGUMENTS, "Package argument is missing.")

    private companion object { const val MAX_RESULTS = 50 }
}
