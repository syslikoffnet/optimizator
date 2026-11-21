package com.duchamp.root

/**
 * Immutable record of one root command execution. Every field is real: nothing is invented.
 *
 * @param command the exact command line that was executed
 * @param exitCode process exit code, or [NO_CODE] when the command never produced one
 *                 (timeout, process failure, shell not available)
 * @param stdout captured standard output
 * @param stderr captured standard error
 * @param durationMs wall-clock duration in milliseconds
 * @param atMillis epoch millis when the command completed
 */
data class CommandResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val atMillis: Long,
) {
    val success: Boolean
        get() = exitCode == 0

    companion object {
        const val NO_CODE = -1

        fun failed(command: String, stderr: String, durationMs: Long = 0L, atMillis: Long = 0L): CommandResult =
            CommandResult(command, NO_CODE, "", stderr, durationMs, atMillis)

        fun failure(message: String, command: String = message): CommandResult =
            failed(command, message)

        fun success(command: String, stdout: String = "", durationMs: Long = 0L, atMillis: Long = 0L): CommandResult =
            CommandResult(command, 0, stdout, "", durationMs, atMillis)
    }
}
