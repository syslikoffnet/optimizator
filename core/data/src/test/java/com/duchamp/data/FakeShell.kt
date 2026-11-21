package com.duchamp.data

import com.duchamp.root.CommandResult
import com.duchamp.root.CommandRunner

/**
 * Test double for [CommandRunner]. Records every command it is asked to run and returns scripted
 * results, so journal/revert and the game-mode state machine can be tested without a device.
 *
 * Scripting model: [script] maps a command (exact match, or a predicate via [match]) to a
 * [CommandResult]. Unmatched commands succeed with empty output by default, or fail when
 * [failUnmatched] is true.
 */
class FakeShell(
    private val failUnmatched: Boolean = false,
) : CommandRunner {
    override val defaultTimeoutMs: Long = 5_000L

    /** Every command executed, in order. */
    val commands: MutableList<String> = mutableListOf()

    private val script = mutableListOf<Pair<(String) -> Boolean, (String) -> CommandResult>>()

    /** Script a command to succeed with the given stdout. */
    fun succeed(command: String, stdout: String = "") {
        script.add({ it == command } to { CommandResult.success(command, stdout) })
    }

    /** Script a command to fail with the given stderr. */
    fun fail(command: String, stderr: String = "failed") {
        script.add({ it == command } to { CommandResult.failure(stderr, command) })
    }

    /** Script any command matching [predicate] to return [result]. */
    fun on(predicate: (String) -> Boolean, result: (String) -> CommandResult) {
        script.add(predicate to result)
    }

    override suspend fun run(command: String, timeoutMs: Long): CommandResult {
        commands += command
        val match = script.firstOrNull { (predicate, _) -> predicate(command) }
        return match?.second?.invoke(command)
            ?: if (failUnmatched) CommandResult.failure("unmatched: $command", command)
            else CommandResult.success(command)
    }

    /** True if a command matching [predicate] was executed. */
    fun ran(predicate: (String) -> Boolean): Boolean = commands.any(predicate)
}
