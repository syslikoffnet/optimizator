package com.duchamp.device

/**
 * Hard safety rules from the product spec (section 7). These packages are never disabled or
 * uninstalled, because breaking them breaks Game Turbo / the OS / the display.
 */
object ProtectedPackages {

    /** Never touch: Game Turbo lives here. */
    val NEVER_TOUCH = setOf(
        KnownPackages.MI_PICKS,
        KnownPackages.SECURITY_CENTER,
        KnownPackages.POWERKEEPER,
        KnownPackages.PHONE,
        KnownPackages.SYSTEMUI,
        KnownPackages.GMS,
        KnownPackages.GAME_DRIVER,
    )

    /**
     * Never touch without an explicit, loud warning about the risk of breaking Game Turbo /
     * screen refresh rate. Default behaviour is to leave these alone.
     */
    val CAUTION_TOUCH = setOf(
        KnownPackages.GLGM,
        KnownPackages.JOYOSE,
    )

    /** Actions that destroy user data and are therefore never performed. */
    val FORBIDDEN_ACTIONS = setOf(
        "pm clear",
        "pm remove-user",
        "swapoff",
        "input_suspend",
    )

    fun isNeverTouch(packageName: String): Boolean = packageName in NEVER_TOUCH

    fun isCautionTouch(packageName: String): Boolean =
        packageName in CAUTION_TOUCH && packageName !in NEVER_TOUCH

    /**
     * Health-check used after any package mutation: Game Turbo's launcher intent must still
     * resolve to at least one activity. If it does not, the last change must be reverted.
     */
    fun gameTurboHealthCheckNeeded(packageName: String): Boolean =
        packageName == KnownPackages.MI_PICKS || packageName == KnownPackages.GLGM
}
