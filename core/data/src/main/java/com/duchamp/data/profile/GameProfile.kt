package com.duchamp.data.profile

/** What duchamp should do with an app during a game-mode session. */
enum class GameAction {
    FREEZE,
    CLOSE,
    CLOSE_BLOCK,
    IGNORE,
}

/**
 * The user's one-time configuration. Everything the app changes during a session is derived
 * from this profile. Stored in DataStore; exportable/importable as JSON.
 */
data class GameProfile(
    val onboarded: Boolean = false,
    /** App that must keep playing audio during a game (never frozen/closed). */
    val musicPackage: String = "",
    /** Messenger allowed to deliver notifications during a game (whitelisted). */
    val messengerPackage: String = "",
    /** User uses a VPN; the VPN package is whitelisted. */
    val useVpn: Boolean = false,
    /** Enable battery bypass automatically when charging during a game. */
    val enableBypassOnCharge: Boolean = false,
    /** swappiness applied during a session. */
    val swappinessTarget: Int = 60,
    /** Schema version, bumped when the shape of the profile changes. */
    val version: Int = 1,
    /** Per-package action for the game list. */
    val gameActions: Map<String, GameAction> = emptyMap(),
    /** Extra packages never touched (whitelist). */
    val whitelist: Set<String> = emptySet(),
    /** Tweak ids the user chose to enable during a game. */
    val enabledTweaks: Set<String> = emptySet(),
) {
    /** Whether a tweak runs during a session. Unknown ids count as enabled so new tweaks work. */
    fun tweakEnabled(id: String): Boolean = enabledTweaks.isEmpty() || id in enabledTweaks

    val closePackages: Set<String>
        get() = gameActions.filterValues { it == GameAction.CLOSE || it == GameAction.CLOSE_BLOCK }.keys

    val freezePackages: Set<String>
        get() = gameActions.filterValues { it == GameAction.FREEZE }.keys

    val blockPackages: Set<String>
        get() = gameActions.filterValues { it == GameAction.CLOSE_BLOCK }.keys

    fun actionFor(packageName: String): GameAction =
        gameActions[packageName] ?: GameAction.IGNORE

    companion object {
        fun defaultProfile(): GameProfile = GameProfile()
    }
}
