package com.darkan.crash

import java.awt.GraphicsEnvironment
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

object CrashHandler {
    const val DISCORD_INVITE = "https://discordapp.com/invite/Z32ggEB"
    const val DISCORD_CHANNEL = "#bug-reports"

    const val GAME_THREAD_NAME = "darkan-game"

    private const val RECENT_ERRORS_KEPT = 8
    private const val DIALOG_TIMEOUT_MS = 30_000L

    private val crashed = AtomicBoolean(false)
    private val recentErrors = ArrayDeque<String>()

    fun logsDir(): File {
        val override = System.getenv("DARKAN_HOME")?.takeIf { it.isNotBlank() }
        val root = override?.let(::File) ?: File(System.getProperty("user.home"), ".darkanrs")
        return File(root, "logs")
    }

    fun install() {
        ConsoleRecorder.install()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (thread.name == GAME_THREAD_NAME || thread.name == "main") {
                fatal(throwable, "uncaught on thread ${thread.name}")
            } else {
                noteError("uncaught on thread ${thread.name}", throwable)
            }
        }
        val test = System.getProperty("darkan.crashTest")
        if (test != null) {
            throw IllegalStateException("Test crash requested with -Ddarkan.crashTest=$test")
        }
    }

    @JvmStatic
    fun noteError(context: String?, throwable: Throwable?) {
        val label = context?.takeIf { it.isNotBlank() } ?: "error"
        System.err.println("[Client error] $label")
        throwable?.printStackTrace()
        val summary = buildString {
            append(label)
            if (throwable != null) append(": ").append(CrashReport.compactTrace(throwable, topFrames = 6, causeFrames = 2))
        }
        synchronized(recentErrors) {
            if (recentErrors.size >= RECENT_ERRORS_KEPT) recentErrors.removeFirst()
            recentErrors.addLast(summary)
        }
    }

    @JvmStatic
    fun fatal(throwable: Throwable, context: String) {
        if (!crashed.compareAndSet(false, true)) {
            System.err.println("[Crash] Further error after the first crash ($context):")
            throwable.printStackTrace()
            return
        }
        val report = runCatching { CrashReport.build(throwable, context, snapshotErrors(), fatal = true) }.getOrNull()
        if (report == null) {
            System.err.println("[Crash] Could not build a crash report for: $context")
            throwable.printStackTrace()
            exitProcess(1)
        }
        val file = report.save()
        System.err.println("[Crash] ${report.discordText}")
        System.err.println("[Crash] Full report saved to ${file?.absolutePath ?: "(could not write file)"}")
        System.err.println("[Crash] Full stack trace:")
        throwable.printStackTrace()

        if (GraphicsEnvironment.isHeadless() || System.getProperty("darkan.crashDialog") == "false") {
            exitProcess(1)
        }
        val shown = AtomicBoolean(false)
        SwingUtilities.invokeLater {
            runCatching {
                CrashDialog.show(report, file, CrashDialog.Mode.CRASH) { exitProcess(1) }
                shown.set(true)
            }.onFailure {
                System.err.println("[Crash] Could not show the crash dialog: $it")
                exitProcess(1)
            }
        }
        Thread({
            Thread.sleep(DIALOG_TIMEOUT_MS)
            if (!shown.get()) {
                System.err.println("[Crash] Crash dialog did not appear within ${DIALOG_TIMEOUT_MS / 1000}s; exiting")
                Runtime.getRuntime().halt(1)
            }
        }, "darkan-crash-watchdog").apply { isDaemon = true }.start()
    }

    fun userReport(context: String = "requested by the player"): CrashReport =
        CrashReport.build(null, context, snapshotErrors(), fatal = false)

    private fun snapshotErrors(): List<String> = synchronized(recentErrors) { recentErrors.toList() }
}
