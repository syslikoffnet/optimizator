package com.duchamp.root

/**
 * Single abstraction over every root action in the app. All tweaks go through this, so:
 * - every command has a timeout,
 * - the exit code is checked,
 * - nothing runs on the main thread (implementations are suspend functions),
 * - failures are captured as a [CommandResult] instead of throwing.
 *
 * Implementations: [LibsuCommandRunner] (real device) and [FakeShell] (demo / unit tests).
 */
interface CommandRunner {

    /** Default timeout applied when the caller does not specify one. */
    val defaultTimeoutMs: Long

    /**
     * Runs [command] with a timeout and returns a fully-populated [CommandResult]. Never throws
     * for a non-zero exit code or a shell failure: failures are reported in the result.
     */
    suspend fun run(command: String, timeoutMs: Long = defaultTimeoutMs): CommandResult

    /** Convenience for commands whose output is parsed. */
    suspend fun capture(command: String, timeoutMs: Long = defaultTimeoutMs): CommandResult =
        run(command, timeoutMs)

    /** True when the shell can execute as root (`id -u` returns 0). */
    suspend fun isRootAvailable(): Boolean {
        val result = run("id -u")
        return result.success && result.stdout.trim() == "0"
    }

    /** Runs the canonical root probe and returns its raw result. */
    suspend fun checkRoot(): CommandResult = run("id -u")
}
