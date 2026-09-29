package com.jarvis.brain

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.jarvis.logging.JarvisLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Boots the Chaquopy interpreter and holds a handle to the `brain` module. */
@Singleton
class PythonRuntime @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val logger: JarvisLogger
) {
    @Volatile private var module: PyObject? = null

    @Synchronized
    fun startIfNeeded() {
        if (module != null) return
        if (!Python.isStarted()) Python.start(AndroidPlatform(ctx))
        module = Python.getInstance().getModule("brain")
        logger.i(TAG, "Python runtime started")
    }

    fun module(): PyObject = module ?: error("Python not started")
    fun call(fn: String, vararg args: Any?): PyObject? = module()?.callAttr(fn, *args)
    companion object { private const val TAG = "PythonRuntime" }
}
