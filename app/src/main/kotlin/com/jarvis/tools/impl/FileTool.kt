package com.jarvis.tools.impl

import android.content.Context
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File read, sandboxed to the app's external-files directory.
 * Rejects absolute paths, null bytes, and any path whose canonical form
 * does not stay under the sandbox root.
 */
@Singleton
class FileTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "file_read",
        descriptionHi = "फ़ाइल पढ़ें",
        descriptionEn = "Read a file's text content (sandboxed)",
        parameters = mapOf(
            "path" to ParamSpec("string", true, "Path relative to JARVIS files dir"),
            "maxBytes" to ParamSpec("int", false, "Limit", "65536")
        )
    )

    private fun root(): File = (ctx.getExternalFilesDir(null) ?: ctx.filesDir).canonicalFile

    private fun resolveSafe(rel: String): File? {
        if (rel.isBlank() || rel.contains('\u0000')) return null
        // Reject absolute and Windows-style absolute paths up front.
        if (rel.startsWith("/") || rel.startsWith("\\") ||
            (rel.length >= 2 && rel[1] == ':')
        ) return null
        val root = root()
        val candidate = File(root, rel).canonicalFile
        val rootPath = root.path
        if (candidate.path != rootPath && !candidate.path.startsWith(rootPath + File.separator)) {
            return null
        }
        return candidate
    }

    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val rel = args["path"] as? String ?: return ToolResult.Err("path required", "bad_arg")
        val max = ((args["maxBytes"] as? Number)?.toInt() ?: 65_536).coerceIn(1, 1_048_576)
        val f = resolveSafe(rel)
            ?: return ToolResult.Err("path outside sandbox or invalid: $rel", "path_denied")
        if (!f.exists() || !f.isFile) return ToolResult.Err("not found: $rel", "not_found")
        val bytes = f.readBytes()
        val clipped = if (bytes.size > max) bytes.copyOf(max) else bytes
        return ToolResult.Ok(clipped.toString(Charsets.UTF_8))
    }
}
