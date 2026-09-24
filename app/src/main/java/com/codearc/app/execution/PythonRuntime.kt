package com.codearc.app.execution

import android.content.Context
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File

data class PyRunOutcome(val stdout: String, val stderr: String, val exitCode: Int)

/** Genuine offline CPython execution via Chaquopy's bundled interpreter — this is a real
 *  Python 3, not a simulation, but it runs in-process inside CodeArc's own app sandbox: there
 *  is no separate OS process, no process-level timeout kill, and no filesystem jail beyond
 *  what the app itself already has. Security in the Phase 4 plan's sense (execution timeout,
 *  restricted working directory) is applied at the sys.path / caller level, not via OS
 *  isolation — see PHASE4.md for the honest limitation and what a stronger sandbox would need. */
object PythonRuntime {
    @Volatile private var started = false

    /** True once a prior attempt to start the interpreter has failed because Chaquopy's runtime
     *  assets (the bundled Python interpreter + stdlib, normally packaged by the Chaquo Gradle
     *  plugin's own build tasks) aren't present in this APK. On CodeAssist that's expected — its
     *  build engine reads plain `dependencies {}` lines but doesn't run a plugin's own tasks, so
     *  Python.start() has nothing to load. Cached so every Run after the first fails fast with a
     *  clear message instead of repeating the same slow failed start attempt. */
    @Volatile private var assetsUnavailable = false

    private fun unavailableOutcome() = PyRunOutcome(
        "",
        "Offline Python isn't available in this build: the bundled interpreter/standard " +
            "library that Chaquopy normally packages weren't included (CodeAssist doesn't run " +
            "that packaging step). Set a Cloud Run endpoint in Settings > Execution to run " +
            "Python remotely instead, or build this project with real Gradle/AGP (e.g. Android " +
            "Studio) for working offline execution.",
        -1,
    )

    @Synchronized
    private fun ensureStarted(context: Context) {
        if (started) return
        if (!Python.isStarted()) Python.start(AndroidPlatform(context.applicationContext))
        started = true
    }

    /** Runs [mainFile] (project-relative, e.g. "src/main.py") with [projectDir]/<its folder>
     *  added to sys.path so sibling files in the same project can be imported. [stdin] is fed
     *  to input() one line at a time. Blocking — call from a background dispatcher. */
    fun run(context: Context, projectDir: File, mainFile: String, stdin: String): PyRunOutcome {
        if (assetsUnavailable) return unavailableOutcome()
        try {
            ensureStarted(context)
        } catch (e: Throwable) {
            // Python.start() throws when Chaquopy's bundled interpreter/stdlib assets are
            // missing from the APK — expected on this build engine, see the class doc above.
            // Caught broadly (not just PyException) because the underlying failure here is a
            // missing-asset FileNotFoundException/NoClassDefFoundError from Chaquopy's native
            // init path, not a Python-level error.
            assetsUnavailable = true
            return unavailableOutcome()
        }
        val entry = File(projectDir, mainFile)
        if (!entry.exists()) return PyRunOutcome("", "Main file not found: $mainFile", -1)
        val srcDir = entry.parentFile ?: projectDir

        val py = Python.getInstance()
        val builtins = py.getBuiltins()
        val sys = py.getModule("sys")
        val io = py.getModule("io")
        val traceback = py.getModule("traceback")

        val path = sys["path"]!!
        path.callAttr("insert", 0, srcDir.absolutePath)

        val outBuf = io.callAttr("StringIO")
        val errBuf = io.callAttr("StringIO")
        val stdinBuf = io.callAttr("StringIO", stdin)
        val originalOut = sys["stdout"]
        val originalErr = sys["stderr"]
        val originalIn = sys["stdin"]
        sys["stdout"] = outBuf
        sys["stderr"] = errBuf
        sys["stdin"] = stdinBuf

        var exitCode = 0
        try {
            val code = entry.readText()
            val compiled = builtins.callAttr("compile", code, entry.name, "exec")
            val globals = builtins.callAttr("dict")
            globals.callAttr("__setitem__", "__name__", "__main__")
            builtins.callAttr("exec", compiled, globals)
        } catch (e: PyException) {
            val msg = e.message.orEmpty()
            exitCode = when {
                msg.contains("SystemExit") -> Regex("-?\\d+").find(msg)?.value?.toIntOrNull() ?: 0
                else -> 1
            }
            if (exitCode != 0 || !msg.contains("SystemExit")) {
                val formatted = runCatching { traceback.callAttr("format_exc").toString() }.getOrNull()
                errBuf.callAttr("write", (formatted ?: msg) + "\n")
            }
        } finally {
            sys["stdout"] = originalOut
            sys["stderr"] = originalErr
            sys["stdin"] = originalIn
            runCatching { path.callAttr("remove", srcDir.absolutePath) }
        }

        val stdout = outBuf.callAttr("getvalue").toString()
        val stderr = errBuf.callAttr("getvalue").toString()
        return PyRunOutcome(stdout, stderr, exitCode)
    }
}
