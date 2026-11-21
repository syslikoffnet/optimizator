package com.duchamp.data.tweak

/** Risk level of a tweak, driving the confirmation flow and application order in the UI. */
enum class Risk {
    SAFE,
    SYSTEM,
    SYSTEM_OPTIMIZATION,
    PROCESS,
    SENSITIVE,
}

/** Logical grouping used by the "Функции" screen. */
enum class TweakGroup {
    GAME_MODE,
    POWER,
    NETWORK,
    MEMORY,
    BACKGROUND,
    XSPACE,
    SYSTEM,
    BOOT,
}

/** Unified state chip for every function (section 3.11). */
enum class TweakStatus {
    ON,
    OFF,
    DIVERGED,
    UNAVAILABLE,
    ERROR,
}

/**
 * The current, genuinely-read state of one function. [rawValue] is exactly what was parsed from
 * the system; [detail] is a short human explanation; [error] is set for [TweakStatus.ERROR].
 */
data class TweakState(
    val status: TweakStatus,
    val rawValue: String,
    val detail: String = "",
    val checkedAtMillis: Long = 0L,
    val error: String? = null,
) {
    companion object {
        fun on(raw: String, detail: String = "", at: Long = 0L): TweakState =
            TweakState(TweakStatus.ON, raw, detail, at)

        fun off(raw: String, detail: String = "", at: Long = 0L): TweakState =
            TweakState(TweakStatus.OFF, raw, detail, at)

        fun diverged(raw: String, detail: String, at: Long = 0L): TweakState =
            TweakState(TweakStatus.DIVERGED, raw, detail, at)

        fun unavailable(raw: String = "", detail: String = ""): TweakState =
            TweakState(TweakStatus.UNAVAILABLE, raw, detail)

        fun error(raw: String, message: String): TweakState =
            TweakState(TweakStatus.ERROR, raw, message, error = message)
    }
}

/**
 * One configurable function of the system. Every implementation provides:
 * - [snapshot] — read the real current state (never from memory),
 * - [apply] — make the state match the profile,
 * - [verify] — confirm the effect actually happened,
 * - [revert] — undo, restoring [from].
 *
 * Implementations never throw for a failed command: they return [Result] / false and the
 * journal records the raw [com.duchamp.root.CommandResult].
 */
interface Tweak {
    val id: String
    val title: String
    val group: TweakGroup
    val risk: Risk
    val description: String

    suspend fun snapshot(): TweakState

    suspend fun apply(): Result<Unit>

    suspend fun verify(): Boolean

    suspend fun revert(from: TweakState): Result<Unit>

    /**
     * Commands this tweak runs when applied. Recorded in the journal *before* [apply] runs so
     * the log survives a crash. Default empty for read-only tweaks.
     */
    fun applyCommands(): List<String> = emptyList()

    /** Commands this tweak runs when reverting back to [from]. */
    fun revertCommands(from: TweakState): List<String> = emptyList()
}
