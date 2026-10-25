package com.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLauncherTool @Inject constructor(
    @ApplicationContext private val ctx: Context
) : Tool {

    override val spec = ToolSpec(
        name = "open_app",
        descriptionHi = "ऐप खोलें",
        descriptionEn = "Open a launchable installed app by package or name",
        parameters = mapOf(
            "target" to ParamSpec("string", true, "Package name or app label")
        )
    )

    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val target = (args["target"] as? String)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return ToolResult.Err("target required", "bad_arg")

        val launcherIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = queryLaunchers(launcherIntent)

        val exactPackage = matches.firstOrNull { it.activityInfo.packageName.equals(target, true) }
        val exactLabel = matches.firstOrNull {
            it.loadLabel(ctx.packageManager).toString().equals(target, true)
        }
        val partialLabel = matches.firstOrNull {
            it.loadLabel(ctx.packageManager).toString().contains(target, true)
        }

        val chosen = exactPackage ?: exactLabel ?: partialLabel
            ?: return ToolResult.Err("App not found: $target", "not_found")

        val launchIntent = chosen.activityInfo.packageName.let {
            ctx.packageManager.getLaunchIntentForPackage(it)
        } ?: Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(chosen.activityInfo.packageName)

        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (ctx.packageManager.resolveActivity(launchIntent, 0) == null) {
                ToolResult.Err(
                    "No launcher activity is available for ${chosen.activityInfo.packageName}.",
                    "no_launcher"
                )
            } else {
                ctx.startActivity(launchIntent)
                ToolResult.Ok("opened ${chosen.activityInfo.packageName}")
            }
        } catch (t: Throwable) {
            ToolResult.Err(t.message ?: "Unable to open app", "launch_error")
        }
    }

    private fun queryLaunchers(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            ctx.packageManager.queryIntentActivities(intent, 0)
        }
}
