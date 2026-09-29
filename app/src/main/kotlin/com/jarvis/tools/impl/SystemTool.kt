package com.jarvis.tools.impl

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "system_info",
        descriptionHi = "RAM, स्टोरेज आदि की जानकारी",
        descriptionEn = "Report RAM, storage, and CPU info",
        parameters = emptyMap()
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeGB = stat.availableBytes / 1_000_000_000.0
        val totalGB = stat.totalBytes / 1_000_000_000.0
        val threads = Runtime.getRuntime().availableProcessors()
        return ToolResult.Ok(
            "cpu=$threads cores, ram=${mem.availMem / 1_000_000}MB/${mem.totalMem / 1_000_000}MB, " +
            "storage=${"%.1f".format(freeGB)}/${"%.1f".format(totalGB)}GB free")
    }
}
