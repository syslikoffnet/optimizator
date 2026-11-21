package com.duchamp.device

/**
 * Every sysfs / proc / settings path and package name the app touches, in one place.
 * Nothing here is guessed: these are the paths confirmed to work on the POCO X6 Pro (duchamp).
 */
object DevicePaths {

    // --- Battery (MediaTek bypass) ---
    const val BATTERY_CMD = "/proc/mtk_battery_cmd/current_cmd"
    const val BATTERY_STATUS = "/sys/class/power_supply/battery/status"
    const val BATTERY_CURRENT_NOW = "/sys/class/power_supply/battery/current_now"
    const val BATTERY_CYCLE_COUNT = "/sys/class/power_supply/battery/cycle_count"
    const val BATTERY_CHARGE_FULL = "/sys/class/power_supply/battery/charge_full"
    const val BATTERY_CHARGE_FULL_DESIGN = "/sys/class/power_supply/battery/charge_full_design"

    const val BATTERY_CMD_ENABLE = "0 1"
    const val BATTERY_CMD_DISABLE = "0 0"

    // --- Memory ---
    const val MEMINFO = "/proc/meminfo"
    const val SWAP_S = "/proc/swaps"
    const val ZRAM_MM_STAT = "/sys/block/zram0/mm_stat"
    const val SWAPPINESS = "/proc/sys/vm/swappiness"

    // --- CPU / GPU ---
    val CPU_POLICIES = listOf("policy0", "policy4", "policy7")
    const val GPU_FREQ_STATUS = "/proc/gpufreqv2/gpufreq_status"
    const val GPU_GED_FREQ = "/sys/kernel/ged/hal/current_freqency"
    const val GPU_GED_UTIL = "/sys/kernel/ged/hal/gpu_utilization"
    const val FPSGO_STATUS = "/sys/kernel/fpsgo/fstb/fpsgo_status"

    // --- Thermal ---
    const val THERMAL_ZONE_DIR = "/sys/class/thermal"
    const val THERMAL_MESSAGE_SCONFIG = "/sys/class/thermal/thermal_message/sconfig"

    /** Thermal zones worth showing; zones that report -274000 are skipped by the parser. */
    val THERMAL_ZONES_TO_SHOW = listOf(
        "soc_max",
        "cpu-big-core7-1",
        "cpu-medium-core4-1",
        "cpu-little-core0",
        "gpu1",
        "battery",
        "bms",
        "md1",
        "wifi_therm",
        "charger1_therm",
        "quiet_therm",
    )

    // --- Packages / processes ---
    const val PROC_PREFIX = "/proc"

    // --- KernelSU boot script ---
    const val KSU_SERVICE_D = "/data/adb/service.d"
    const val BOOT_SCRIPT = "/data/adb/service.d/duchamp.sh"
    const val BOOT_LOG = "/data/adb/duchamp/boot.log"
    const val BASELINE_JSON = "/data/adb/duchamp/baseline.json"

    // --- Settings keys ---
    const val WIFI_SCAN_ALWAYS = "wifi_scan_always_enabled"
    const val BLE_SCAN_ALWAYS = "ble_scan_always_enabled"
    const val WINDOW_ANIMATION_SCALE = "window_animation_scale"
    const val TRANSITION_ANIMATION_SCALE = "transition_animation_scale"
    const val ANIMTOR_ANIMATION_SCALE = "animator_animation_scale"
    const val VIBRATE_WHEN_CALLS = "vibrate_when_calls"
    const val HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled"

    // --- PUBG / target game ---
    const val GAME_PACKAGE = "com.tencent.ig"

    /** Thermal status codes from `dumpsys thermalservice`. */
    const val THERMAL_STATUS_NONE = 0
    const val THERMAL_STATUS_THROTTLE_1 = 1
    const val THERMAL_STATUS_THROTTLE_2 = 2
    const val THERMAL_STATUS_SEVERE = 3
    const val THERMAL_STATUS_SHUTDOWN = 6
}

/** Well-known package names used across the app. */
object KnownPackages {
    const val PUBG = DevicePaths.GAME_PACKAGE
    const val MI_PICKS = "com.xiaomi.mipicks"
    const val SECURITY_CENTER = "com.miui.securitycenter"
    const val POWERKEEPER = "com.miui.powerkeeper"
    const val PHONE = "com.android.phone"
    const val SYSTEMUI = "com.android.systemui"
    const val GMS = "com.google.android.gms"
    const val GAME_DRIVER = "com.mediatek.mt6897.gamedriver"
    const val GLGM = "com.xiaomi.glgm"
    const val JOYOSE = "com.xiaomi.joyose"

    const val XSPACE_USER = 999
    const val GAME_TURBINE_INTENT = "miui.gamebooster.action.GLOBAL_GAMEBOX"

    /** Default on-device "close these" suggestions derived from real diagnostics. */
    val DEFAULT_CLOSE_PACKAGES = listOf(
        "com.zhiliaoapp.musically",
        "com.deepseek.chat",
        "com.android.chrome",
        "bin.mt.plus",
        "com.google.android.googlequicksearchbox",
    )
    val DEFAULT_FREEZE_PACKAGES = listOf("com.radolyn.ayugram")
}
