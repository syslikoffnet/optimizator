package com.duchamp.data.tweak

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Snapshot test for the KernelSU boot script generator. */
class BootScriptGeneratorTest {

    @Test
    fun `generates the full script with all settings`() {
        val script = generateBootScript(
            BootScriptConfig(
                keepSwappiness = true,
                swappiness = 60,
                wifiScanAlways = "1",
                bleScanAlways = "1",
                windowAnimation = "0.5",
                transitionAnimation = "0.5",
                animatorAnimation = "0.5",
            ),
        )
        assertEquals(SNAPSHOT, script)
    }

    @Test
    fun `omits optional sections when not configured`() {
        val script = generateBootScript(BootScriptConfig())
        assertTrue(script.startsWith("#!/system/bin/sh"))
        assertTrue(script.contains("battery bypass OFF"))
        // No swappiness, no per-setting checks.
        assertTrue(!script.contains("swappiness"))
        assertTrue(!script.contains("check_setting wifi_scan_always_enabled"))
        assertTrue(script.contains("log \"boot done\""))
    }

    @Test
    fun `script is idempotent and always exits 0`() {
        val script = generateBootScript(BootScriptConfig(wifiScanAlways = "1"))
        assertTrue(script.trimEnd().endsWith("exit 0"))
    }

    private companion object {
        val SNAPSHOT = """#!/system/bin/sh
# duchamp boot script (KernelSU service.d). Idempotent, runs on every boot.
LOG=/data/adb/duchamp/boot.log
mkdir -p /data/adb/duchamp

log() { echo "[duchamp] $(date '+%Y-%m-%d %H:%M:%S') $1" >> ${'$'}LOG; }

# Wait for boot to complete and let HyperOS settle (~20s) before touching values.
while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done
sleep 20

log "boot start"

# 1. Battery bypass must never survive a reboot.
if echo '0 0' > /proc/mtk_battery_cmd/current_cmd 2>/dev/null; then
  log "battery bypass OFF: OK"
else
  log "battery bypass OFF: FAIL"
fi

# 2. Resettable-but-permanent settings, applied only when the user chose to keep them.
if echo '60' > /proc/sys/vm/swappiness 2>/dev/null; then
  log "swappiness 60: OK"
else
  log "swappiness 60: FAIL"
fi

# 3. Re-check permanent settings; re-apply on divergence.
check_setting() {
  name=$1; want=$2; cur=$(settings get global ${'$'}name 2>/dev/null | tr -d '\n')
  if [ "${'$'}cur" = "${'$'}want" ]; then
    log "${'$'}name=${'$'}want: OK"
  else
    settings put global ${'$'}name ${'$'}want 2>/dev/null && log "${'$'}name=${'$'}want: re-applied" || log "${'$'}name=${'$'}want: FAIL"
  fi
}
check_setting wifi_scan_always_enabled $1
check_setting ble_scan_always_enabled $1
check_setting window_animation_scale $0.5
check_setting transition_animation_scale $0.5
check_setting animator_animation_scale $0.5

log "boot done"
exit 0
"""
    }
}
