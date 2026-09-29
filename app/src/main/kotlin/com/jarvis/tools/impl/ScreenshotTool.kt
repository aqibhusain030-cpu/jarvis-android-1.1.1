package com.jarvis.tools.impl

import android.content.Context
import com.jarvis.logging.JarvisLogger
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Screenshot tool. On non-rooted Android 12+ the only reliable path is
 * MediaProjection with a foreground service + user consent; the full
 * implementation is wired in Part 2 (ScreenshotService). For now the tool
 * reports the honest limitation.
 */
@Singleton
class ScreenshotTool @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val logger: JarvisLogger
) : Tool {
    override val spec = ToolSpec(
        name = "screenshot",
        descriptionHi = "स्क्रीनशॉट लें",
        descriptionEn = "Capture a screenshot (requires MediaProjection consent)",
        parameters = emptyMap()
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        logger.w(TAG, "screenshot requested — MediaProjection flow not yet armed")
        return ToolResult.Err(
            "Screenshot requires MediaProjection consent. Please grant it from Settings → Screenshots.",
            "needs_media_projection")
    }
    companion object { private const val TAG = "ScreenshotTool" }
}
