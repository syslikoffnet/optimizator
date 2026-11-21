package com.duchamp.data.tweak

/**
 * Pure generator for the KernelSU boot script `/data/adb/service.d/duchamp.sh`.
 *
 * The script is idempotent: KernelSU re-runs it on every boot. It waits for the system to settle,
 * forcibly disables the battery bypass (which must never survive a reboot), re-applies the
 * "resettable but chosen permanent" settings, re-checks the permanent ones, and writes a
 * `boot.log` with the outcome of every step.
 *
 * Kept as a pure string function so it is unit-testable against a snapshot.
 */
data class BootScriptConfig(
    /** Apply swappiness on boot (user chose to keep it permanent). */
    val keepSwappiness: Boolean = false,
    val swappiness: Int = 60,
    /** Re-check (and re-apply on divergence) the permanent settings captured in the baseline. */
    val wifiScanAlways: String? = null,
    val bleScanAlways: String? = null,
    val windowAnimation: String? = null,
    val transitionAnimation: String? = null,
    val animatorAnimation: String? = null,
)

fun generateBootScript(config: BootScriptConfig): String = buildString {
    appendLine("#!/system/bin/sh")
    appendLine("# duchamp boot script (KernelSU service.d). Idempotent, runs on every boot.")
    appendLine("LOG=/data/adb/duchamp/boot.log")
    appendLine("mkdir -p /data/adb/duchamp")
    appendLine()
    appendLine("log() { echo \"[duchamp] \$(date '+%Y-%m-%d %H:%M:%S') \$1\" >> \$LOG; }")
    appendLine()
    appendLine("# Wait for boot to complete and let HyperOS settle (~20s) before touching values.")
    appendLine("while [ \"\$(getprop sys.boot_completed)\" != \"1\" ]; do sleep 2; done")
    appendLine("sleep 20")
    appendLine()
    appendLine("log \"boot start\"")
    appendLine()
    appendLine("# 1. Battery bypass must never survive a reboot.")
    appendLine("if echo '0 0' > /proc/mtk_battery_cmd/current_cmd 2>/dev/null; then")
    appendLine("  log \"battery bypass OFF: OK\"")
    appendLine("else")
    appendLine("  log \"battery bypass OFF: FAIL\"")
    appendLine("fi")
    appendLine()
    // 2. Resettable-but-permanent settings, applied only when the user chose to keep them.
    if (config.keepSwappiness) {
        appendLine("# 2. Resettable-but-permanent settings, applied only when the user chose to keep them.")
        appendLine("if echo '${config.swappiness}' > /proc/sys/vm/swappiness 2>/dev/null; then")
        appendLine("  log \"swappiness ${config.swappiness}: OK\"")
        appendLine("else")
        appendLine("  log \"swappiness ${config.swappiness}: FAIL\"")
        appendLine("fi")
        appendLine()
    }
    appendLine("# 3. Re-check permanent settings; re-apply on divergence.")
    appendLine("check_setting() {")
    appendLine("  name=\$1; want=\$2; cur=\$(settings get global \$name 2>/dev/null | tr -d '\\n')")
    appendLine("  if [ \"\$cur\" = \"\$want\" ]; then")
    appendLine("    log \"\$name=\$want: OK\"")
    appendLine("  else")
    appendLine("    settings put global \$name \$want 2>/dev/null && log \"\$name=\$want: re-applied\" || log \"\$name=\$want: FAIL\"")
    appendLine("  fi")
    appendLine("}")
    config.wifiScanAlways?.let { appendLine("check_setting wifi_scan_always_enabled \$$it") }
    config.bleScanAlways?.let { appendLine("check_setting ble_scan_always_enabled \$$it") }
    config.windowAnimation?.let { appendLine("check_setting window_animation_scale \$$it") }
    config.transitionAnimation?.let { appendLine("check_setting transition_animation_scale \$$it") }
    config.animatorAnimation?.let { appendLine("check_setting animator_animation_scale \$$it") }
    appendLine()
    appendLine("log \"boot done\"")
    appendLine("exit 0")
}
