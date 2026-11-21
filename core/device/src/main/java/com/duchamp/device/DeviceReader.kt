package com.duchamp.device

import com.duchamp.root.CommandRunner

/**
 * High-level, read-only probes over the device. Every method runs a real command through the
 * [CommandRunner], parses the output with a pure parser and never throws: unparseable output
 * yields `null` / empty, which the UI renders as "нет данных".
 */
class DeviceReader(
    private val runner: CommandRunner,
) {

    private suspend fun cat(path: String): String? =
        runner.run("cat '$path'").takeIf { it.success }?.stdout?.trim()

    // --- Memory ---

    suspend fun readMemInfo(): MemInfo? =
        runner.run("cat '${DevicePaths.MEMINFO}'").takeIf { it.success }?.stdout?.let(::parseMemInfo)

    suspend fun readSwaps(): List<SwapInfo> =
        runner.run("cat '${DevicePaths.SWAP_S}'").takeIf { it.success }?.stdout?.let(::parseSwaps)
            .orEmpty()

    suspend fun readZram(): ZramStat? =
        runner.run("cat '${DevicePaths.ZRAM_MM_STAT}'").takeIf { it.success }?.stdout?.let(::parseZramStat)

    suspend fun readSwappiness(): Int? =
        cat(DevicePaths.SWAPPINESS)?.toIntOrNull()

    // --- CPU / GPU ---

    suspend fun readCpuPolicies(): List<CpuPolicy> {
        // Read each policy's four values and assemble one parseable line per policy.
        val sb = StringBuilder()
        for (policy in DevicePaths.CPU_POLICIES) {
            val cur = cat("/sys/devices/system/cpu/cpufreq/$policy/scaling_cur_freq") ?: "0"
            val max = cat("/sys/devices/system/cpu/cpufreq/$policy/scaling_max_freq") ?: "0"
            val hwmax = cat("/sys/devices/system/cpu/cpufreq/$policy/cpuinfo_max_freq") ?: "0"
            val min = cat("/sys/devices/system/cpu/cpufreq/$policy/cpuinfo_min_freq") ?: "0"
            sb.append(policy).append(" gov=sugov_ext cur=").append(cur)
                .append(" min=").append(min).append(" max=").append(max)
                .append(" hwmax=").append(hwmax).append('\n')
        }
        return parseCpuPolicies(sb.toString())
    }

    suspend fun readGpu(): GpuStatus? =
        runner.run("cat '${DevicePaths.GPU_FREQ_STATUS}'").takeIf { it.success }?.stdout?.let(::parseGpuStatus)

    suspend fun readGpuFreqKhz(): Long? = cat(DevicePaths.GPU_GED_FREQ)?.let(::parseGpuFreqKhz)

    suspend fun readGpuUtil(): Int? = cat(DevicePaths.GPU_GED_UTIL)?.let(::parseGpuUtil)

    suspend fun readLoadAverage(): Double? =
        cat("/proc/loadavg")?.let(::parseLoadAverage)

    // --- Thermal ---

    suspend fun readThermalZones(): List<ThermalZone> {
        val cmd =
            "for z in /sys/class/thermal/thermal_zone*; do printf '%s %s %s\\n' " +
                "\"\$(basename \$z)\" \"\$(cat \$z/type 2>/dev/null)\" \"\$(cat \$z/temp 2>/dev/null)\"; done"
        return runner.run(cmd).takeIf { it.success }?.stdout?.let(::parseThermalZones).orEmpty()
    }

    suspend fun readThermalService(): ThermalService? =
        runner.run("dumpsys thermalservice").takeIf { it.success }?.stdout?.let(::parseThermalService)

    // --- Battery ---

    suspend fun readBatterySysfs(): BatterySysfs =
        BatterySysfs(
            status = cat(DevicePaths.BATTERY_STATUS)?.let(::parseBatteryStatus) ?: "",
            currentNow = cat(DevicePaths.BATTERY_CURRENT_NOW)?.let(::parseBatteryInt) ?: 0,
            cycleCount = cat(DevicePaths.BATTERY_CYCLE_COUNT)?.let(::parseBatteryInt) ?: 0,
            chargeFull = cat(DevicePaths.BATTERY_CHARGE_FULL)?.let(::parseBatteryInt) ?: 0,
            chargeFullDesign = cat(DevicePaths.BATTERY_CHARGE_FULL_DESIGN)?.let(::parseBatteryInt) ?: 0,
            currentCmd = cat(DevicePaths.BATTERY_CMD)?.let(::parseBatteryCmd) ?: DevicePaths.BATTERY_CMD_DISABLE,
        )

    suspend fun readBatteryDumpsys(): BatteryDumpsys? =
        runner.run("dumpsys battery").takeIf { it.success }?.stdout?.let(::parseBatteryDumpsys)

    suspend fun readPowerEstimate(): PowerEstimate? =
        runner.run("dumpsys batterystats").takeIf { it.success }?.stdout?.let(::parsePowerEstimate)

    // --- Network ---

    suspend fun readWifiStatus(): WifiStatus? =
        runner.run("cmd wifi status").takeIf { it.success }?.stdout?.let(::parseWifiStatus)

    suspend fun readGateway(): String? =
        runner.run("ip route").takeIf { it.success }?.stdout?.let(::parseGateway)

    suspend fun readPing(host: String, count: Int = 50, intervalSec: String = "0.03"): PingResult? =
        runner.run("ping -c $count -i $intervalSec -W 2 $host", timeoutMs = 15_000)
            .takeIf { it.success }?.stdout?.let(::parsePing)

    suspend fun readVpn(): VpnInfo =
        runner.run("dumpsys connectivity").takeIf { it.success }?.stdout?.let(::parseVpn)
            ?: VpnInfo(active = false, transport = "", uids = emptyList())

    // --- Processes / packages ---

    suspend fun readProcesses(): List<AppProcess> {
        val cmd =
            "for p in /proc/[0-9]*; do pid=\$(basename \$p); name=\$(cat \$p/comm 2>/dev/null); " +
                "rss=\$(awk '/VmRSS/{print \$2}' \$p/status 2>/dev/null); " +
                "swap=\$(awk '/VmSwap/{print \$2}' \$p/status 2>/dev/null); " +
                "echo \"\$pid \$name \$rss \$swap\"; done"
        return runner.run(cmd, timeoutMs = 10_000).takeIf { it.success }?.stdout?.let(::parseProcesses).orEmpty()
    }

    suspend fun isProcessRunning(packageName: String): Boolean {
        val res = runner.run("pidof $packageName")
        return res.success && res.stdout.trim().isNotEmpty()
    }

    suspend fun readPackages(user: Int? = null): List<String> {
        val arg = if (user != null) "--user $user" else ""
        val res = runner.run("pm list packages $arg -3")
        return res.takeIf { it.success }?.stdout?.let(::parsePackageList).orEmpty()
    }

    suspend fun readDisabledPackages(): List<String> =
        runner.run("pm list packages -d").takeIf { it.success }?.stdout?.let(::parsePackageList).orEmpty()

    suspend fun readAppOps(packageName: String): String? =
        runner.run("cmd appops get $packageName RUN_ANY_IN_BACKGROUND")
            .takeIf { it.success }?.stdout?.let(::parseAppOpsBackground)

    suspend fun readStandbyBucket(packageName: String): String =
        runner.run("am get-standby-bucket $packageName")
            .takeIf { it.success }?.stdout?.let(::parseStandbyBucket) ?: "unknown"

    suspend fun readPackageState(packageName: String): PackageInfo? {
        val res = runner.run("dumpsys package $packageName")
        if (!res.success) return null
        val uid = parsePackageUserId(res.stdout) ?: 0
        val enabled = parsePackageEnabled(res.stdout) ?: true
        val suspended = parsePackageSuspended(res.stdout) ?: false
        return PackageInfo(packageName, enabled, suspended, uid)
    }

    suspend fun readGameTurbo(): GameTurboCheck =
        runner.run("pm query-activities -a ${KnownPackages.GAME_TURBINE_INTENT}")
            .takeIf { it.success }?.stdout?.let(::parseGameTurbo)
            ?: GameTurboCheck(false, null, null, null)
}
