package com.jarvis.tools.impl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "clipboard_get",
        descriptionHi = "क्लिपबोर्ड पढ़ें",
        descriptionEn = "Read the clipboard",
        confirmation = Confirmation.VOICE,
        parameters = emptyMap()
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString() ?: ""
        return ToolResult.Ok(text)
    }
}
