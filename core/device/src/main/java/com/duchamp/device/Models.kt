package com.duchamp.device

/**
 * Plain data models for everything parsed out of the device. Parsers are pure functions that
 * turn raw [String] output into these; nothing here performs I/O.
 */

data class MemInfo(
    val memTotalKb: Long,
    val memAvailableKb: Long,
    val memFreeKb: Long,
    val buffersKb: Long,
    val cachedKb: Long,
    val sReclaimableKb: Long,
    val swapTotalKb: Long,
    val swapFreeKb: Long,
) {
    val memAvailableMb: Double get() = memAvailableKb / 1024.0
    val swapUsedMb: Double get() = (swapTotalKb - swapFreeKb).coerceAtLeast(0) / 1024.0
    val swapTotalMb: Double get() = swapTotalKb / 1024.0
}

data class SwapInfo(
    val file: String,
    val type: String,
    val sizeKb: Long,
    val usedKb: Long,
    val priority: Int,
)

data class ZramStat(
    val origDataSize: Long,
    val comprDataSize: Long,
    val memUsedTotal: Long,
    val memLimit: Long,
    val memUsedMax: Long,
    val samePages: Long,
    val pagesCompacted: Long,
    val hugePages: Long,
    val hugePagesSince: Long,
) {
    /** compression ratio, or null when there is nothing to divide by. */
    val ratio: Double?
        get() = if (origDataSize > 0) comprDataSize.toDouble() / origDataSize else null
}

data class CpuPolicy(
    val id: String,
    val governor: String,
    val curFreqKb: Long,
    val minFreqKb: Long,
    val maxFreqKb: Long,
    val hwmaxKb: Long,
) {
    val curFreqGHz: Double get() = curFreqKb / 1_000_000.0
    val maxFreqGHz: Double get() = maxFreqKb / 1_000_000.0
    val hwmaxGHz: Double get() = hwmaxKb / 1_000_000.0
    /** True when the ceiling is below the hardware maximum (thermal throttling). */
    val throttled: Boolean get() = maxFreqKb < hwmaxKb
}

data class GpuStatus(
    val freqKhz: Long,
    val oppIndex: Int,
    val voltageUv: Long,
) {
    val freqGHz: Double get() = freqKhz / 1_000_000.0
}

/** A thermal zone reading. `tempMilli == -274000` means "sensor unavailable". */
data class ThermalZone(
    val name: String,
    val tempMilli: Int,
) {
    val available: Boolean get() = tempMilli != UNAVAILABLE
    val tempCelsius: Double? get() = if (available) tempMilli / 1000.0 else null

    companion object {
        const val UNAVAILABLE = -274_000
    }
}

data class ThermalReading(
    val value: Double,
    val type: Int,
    val name: String,
    val status: Int,
)

data class ThermalService(
    val status: Int,
    val readings: List<ThermalReading>,
)

data class BatterySysfs(
    val status: String,
    val currentNow: Int,
    val cycleCount: Int,
    val chargeFull: Int,
    val chargeFullDesign: Int,
    val currentCmd: String,
) {
    /** Battery health = charge_full / charge_full_design, or null if unknown. */
    val healthPercent: Double?
        get() = if (chargeFullDesign > 0) chargeFull.toDouble() / chargeFullDesign * 100.0 else null

    val isBypass: Boolean get() = currentCmd.trim() == DevicePaths.BATTERY_CMD_ENABLE
}

data class BatteryDumpsys(
    val acPowered: Boolean,
    val usbPowered: Boolean,
    val status: Int,
    val level: Int,
    val voltageMv: Int,
    val temperatureCelsius: Double,
) {
    val charging: Boolean get() = status == 2
    val plugged: Boolean get() = acPowered || usbPowered
}

data class WifiStatus(
    val connectedSsid: String,
    val rssi: Int,
    val linkSpeed: String,
    val frequencyMhz: Int,
    val standard: String,
) {
    /** DFS channels 52-144 sit in 5260-5720 MHz and can be reclaimed by radar. */
    val isDfsChannel: Boolean
        get() = frequencyMhz in 5260..5720
}

data class PingResult(
    val transmitted: Int,
    val received: Int,
    val lossPercent: Double,
    val minMs: Double,
    val avgMs: Double,
    val maxMs: Double,
    val mdevMs: Double,
)

/** A running application process with its measured weight. */
data class AppProcess(
    val pid: Int,
    val name: String,
    val rssKb: Long,
    val swapKb: Long,
    val cpuPercent: Double,
) {
    /** "Weight" used for sorting the task manager: RSS + swap, scaled by CPU. */
    val weightMb: Double get() = (rssKb + swapKb) / 1024.0 + cpuPercent
    val rssMb: Double get() = rssKb / 1024.0
    val swapMb: Double get() = swapKb / 1024.0
}

/** A parsed package with its enablement / suspension state. */
data class PackageInfo(
    val packageName: String,
    val enabled: Boolean,
    val suspended: Boolean,
    val userId: Int,
)

data class GameTurboCheck(
    val available: Boolean,
    val activity: String?,
    val packageName: String?,
    val processName: String?,
)

/** Result of a battery-drain estimate parsed from `dumpsys batterystats`. */
data class PowerEstimate(
    val capacityMah: Double,
    val computedDrainMah: Double,
    val actualDrainRange: Pair<Double, Double>?,
    val perUid: List<UidPower>,
)

data class UidPower(
    val uid: String,
    val mAh: Double,
    val screenHours: Double?,
)

/** Active VPN transport detected from `dumpsys connectivity`. */
data class VpnInfo(
    val active: Boolean,
    val transport: String,
    val uids: List<String>,
)
