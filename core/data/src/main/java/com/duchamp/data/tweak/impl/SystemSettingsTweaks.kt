package com.duchamp.data.tweak.impl

import com.duchamp.data.tweak.Risk
import com.duchamp.data.tweak.Tweak
import com.duchamp.data.tweak.TweakGroup
import com.duchamp.data.tweak.TweakState
import com.duchamp.root.CommandRunner

/**
 * Base class for tweaks that set an Android `settings put global <key> <value>` value.
 *
 * Reads back with `settings get global` and compares to [desired], so [verify] reflects the real
 * system state rather than a cached assumption.
 */
abstract class GlobalSettingTweak(
    final override val id: String,
    final override val title: String,
    final override val group: TweakGroup,
    final override val risk: Risk,
    final override val description: String,
    protected val runner: CommandRunner,
    protected val key: String,
    /** The value the tweak wants the key to hold. */
    protected val desired: String,
) : Tweak {

    final override suspend fun snapshot(): TweakState {
        val result = runner.run("settings get global $key")
        if (!result.success) return TweakState.unavailable(detail = "settings get $key не доступно")
        val value = result.stdout.trim()
        return if (value.isEmpty()) {
            TweakState.off("не задано")
        } else if (value == desired) {
            TweakState.on(value)
        } else {
            TweakState.diverged(value, "ожидается $desired")
        }
    }

    final override suspend fun apply(): Result<Unit> {
        val result = runner.run("settings put global $key $desired")
        return if (result.success) Result.success(Unit) else Result.failure(IllegalStateException(result.stderr))
    }

    final override suspend fun verify(): Boolean =
        runner.run("settings get global $key").stdout.trim() == desired

    final override suspend fun revert(from: TweakState): Result<Unit> {
        if (from.rawValue.isBlank()) return Result.success(Unit)
        val result = runner.run("settings put global $key ${from.rawValue}")
        return if (result.success) Result.success(Unit) else Result.failure(IllegalStateException(result.stderr))
    }

    final override fun applyCommands(): List<String> = listOf("settings put global $key $desired")

    final override fun revertCommands(from: TweakState): List<String> =
        if (from.rawValue.isBlank()) emptyList() else listOf("settings put global $key ${from.rawValue}")
}
