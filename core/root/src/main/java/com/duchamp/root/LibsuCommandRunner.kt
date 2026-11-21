package com.duchamp.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Real [CommandRunner] backed by libsu's main root shell.
 *
 * Commands are enqueued on libsu's executor and awaited with a hard timeout, so a hung command
 * can never block a coroutine indefinitely. All work happens on [Dispatchers.IO]; the caller's
 * thread is never blocked, and the main thread is never touched.
 */
class LibsuCommandRunner(
    override val defaultTimeoutMs: Long = DEFAULT_TIMEOUT_MS,
) : CommandRunner {

    override suspend fun run(command: String, timeoutMs: Long): CommandResult = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        val at = System.currentTimeMillis()
        try {
            val future: Future<Shell.Result> = Shell.cmd(command).enqueue()
            val result = future.get(timeoutMs, TimeUnit.MILLISECONDS)
            val durationMs = elapsedMs(start)
            CommandResult(
                command = command,
                exitCode = result.code,
                stdout = result.out.joinToString(separator = "\n"),
                stderr = result.err.joinToString(separator = "\n"),
                durationMs = durationMs,
                atMillis = System.currentTimeMillis(),
            )
        } catch (e: TimeoutException) {
            CommandResult(
                command = command,
                exitCode = CommandResult.NO_CODE,
                stdout = "",
                stderr = "timeout after ${timeoutMs}ms",
                durationMs = elapsedMs(start),
                atMillis = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            CommandResult(
                command = command,
                exitCode = CommandResult.NO_CODE,
                stdout = "",
                stderr = e.message ?: e.javaClass.simpleName,
                durationMs = elapsedMs(start),
                atMillis = System.currentTimeMillis(),
            )
        }
    }

    private fun elapsedMs(startNanos: Long): Long =
        (System.nanoTime() - startNanos) / 1_000_000L

    companion object {
        const val DEFAULT_TIMEOUT_MS = 5_000L
    }
}
