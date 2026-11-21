package com.duchamp.device

// ---------------------------------------------------------------------------
// pm list packages -> bare package names
// ---------------------------------------------------------------------------

fun parsePackageList(raw: String): List<String> = raw.lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .map { if (it.startsWith("package:")) it.substring("package:".length) else it }
    .distinct()
    .toList()

// ---------------------------------------------------------------------------
// cmd appops get <pkg> RUN_ANY_IN_BACKGROUND
//   RUN_ANY_IN_BACKGROUND: ignore | allow
// ---------------------------------------------------------------------------

fun parseAppOpsBackground(raw: String): String? {
    val m = Regex("""RUN_ANY_IN_BACKGROUND:\s*(\w+)""").find(raw) ?: return null
    return m.groupValues[1]
}

// ---------------------------------------------------------------------------
// am get-standby-bucket <pkg>  -> active | restricted | unrestricted | exempted
// ---------------------------------------------------------------------------

fun parseStandbyBucket(raw: String): String = raw.trim().ifBlank { "unknown" }

// ---------------------------------------------------------------------------
// pm query-activities -a miui.gamebooster.action.GLOBAL_GAMEBOX
//   name=com.miui.gamebooster.GameBoosterRichWebActivity
//   packageName=com.xiaomi.mipicks
//   processName=com.xiaomi.mipicks:gamespace
// Empty output / "No activities found" => broken.
// ---------------------------------------------------------------------------

private val GAMEBOX_ACTIVITY = Regex("""name=(\S+)""")
private val GAMEBOX_PKG = Regex("""packageName=(\S+)""")
private val GAMEBOX_PROC = Regex("""processName=(\S+)""")

fun parseGameTurbo(raw: String): GameTurboCheck {
    val trimmed = raw.trim()
    if (trimmed.isEmpty() || trimmed.contains("No activities found")) {
        return GameTurboCheck(available = false, activity = null, packageName = null, processName = null)
    }
    return GameTurboCheck(
        available = true,
        activity = GAMEBOX_ACTIVITY.find(raw)?.groupValues?.get(1),
        packageName = GAMEBOX_PKG.find(raw)?.groupValues?.get(1),
        processName = GAMEBOX_PROC.find(raw)?.groupValues?.get(1),
    )
}
