package com.jarvis.logging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Append-only file logger + Logcat tee. 30-day retention. */
@Singleton
class JarvisLogger @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun i(tag: String, msg: String) { Log.i(tag, msg); write("I", tag, msg, null) }
    fun w(tag: String, msg: String) { Log.w(tag, msg); write("W", tag, msg, null) }
    fun e(tag: String, msg: String, t: Throwable? = null) {
        Log.e(tag, msg, t); write("E", tag, msg, t)
    }

    fun dir(name: String): File = File(ctx.filesDir, name).apply { mkdirs() }

    private fun write(level: String, tag: String, msg: String, t: Throwable?) {
        runCatching {
            val f = File(dir("logs"), "${fmt.format(Date())}.log")
            val line = "${timeFmt.format(Date())} $level/$tag: $msg" +
                    (t?.let { " :: ${it.message}" } ?: "")
            f.appendText(line + "\n")
            rotate()
        }
    }

    private fun rotate() {
        val dir = dir("logs")
        val cutoff = System.currentTimeMillis() - 30L * 86_400_000
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }
}
