package com.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLauncherTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "open_app",
        descriptionHi = "ऐप खोलें",
        descriptionEn = "Open an installed app by package or name",
        parameters = mapOf("target" to ParamSpec("string", true, "Package name or app label"))
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val target = args["target"] as? String ?: return ToolResult.Err("target required", "bad_arg")
        val pm = ctx.packageManager
        val launch = pm.getLaunchIntentForPackage(target)
        if (launch != null) {
            ctx.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return ToolResult.Ok("opened $target")
        }
        val match = pm.getInstalledApplications(0).firstOrNull { app ->
            pm.getApplicationLabel(app).toString().equals(target, ignoreCase = true)
        } ?: return ToolResult.Err("App not found: $target", "not_found")
        val i = pm.getLaunchIntentForPackage(match.packageName)
            ?: return ToolResult.Err("No launcher for ${match.packageName}", "no_launcher")
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return ToolResult.Ok("opened ${match.packageName}")
    }
}
