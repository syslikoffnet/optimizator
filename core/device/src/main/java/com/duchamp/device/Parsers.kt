package com.duchamp.device

import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// /proc/meminfo
// ---------------------------------------------------------------------------

private val MEMINFO_LINE = Regex("""^(\w+):\s+(\d+)(?:\s+kB)?\s*$""")

fun parseMemInfo(raw: String): MemInfo? {
    val v = HashMap<String, Long>()
    for (line in raw.lineSequence()) {
        val m = MEMINFO_LINE.matchEntire(line.trim()) ?: continue
        v[m.groupValues[1]] = m.groupValues[2].toLong()
    }
    if (v.isEmpty()) return null
    return MemInfo(
        memTotalKb = v["MemTotal"] ?: 0,
        memAvailableKb = v["MemAvailable"] ?: v["MemFree"] ?: 0,
        memFreeKb = v["MemFree"] ?: 0,
        buffersKb = v["Buffers"] ?: 0,
        cachedKb = v["Cached"] ?: 0,
        sReclaimableKb = v["SReclaimable"] ?: 0,
        swapTotalKb = v["SwapTotal"] ?: 0,
        swapFreeKb = v["SwapFree"] ?: 0,
    )
}

// ---------------------------------------------------------------------------
// /proc/swaps
// ---------------------------------------------------------------------------

fun parseSwaps(raw: String): List<SwapInfo> {
    val out = mutableListOf<SwapInfo>()
    for (line in raw.lineSequence()) {
        val t = line.trim()
        if (t.isEmpty() || t.startsWith("Filename")) continue
        val parts = t.split(Regex("""\s+"""))
        if (parts.size < 5) continue
        out.add(
            SwapInfo(
                file = parts[0],
                type = parts[1],
                sizeKb = parts[2].toLongOrNull() ?: 0,
                usedKb = parts[3].toLongOrNull() ?: 0,
                priority = parts[4].toIntOrNull() ?: 0,
            ),
        )
    }
    return out
}

// ---------------------------------------------------------------------------
// /sys/block/zram0/mm_stat
// ---------------------------------------------------------------------------

fun parseZramStat(raw: String): ZramStat? {
    val parts = raw.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
    if (parts.size < 9) return null
    val l = List(9) { parts[it].toLongOrNull() ?: 0L }
    return ZramStat(
        origDataSize = l[0],
        comprDataSize = l[1],
        memUsedTotal = l[2],
        memLimit = l[3],
        memUsedMax = l[4],
        samePages = l[5],
        pagesCompacted = l[6],
        hugePages = l[7],
        hugePagesSince = l[8],
    )
}

// ---------------------------------------------------------------------------
// cpufreq policy lines: "policy0 gov=sugov_ext cur=1300000 min=700000 max=1300000 hwmax=2200000"
// ---------------------------------------------------------------------------

private val CPU_POLICY_LINE =
    Regex("""^(\w+)\s+gov=(\S+)\s+cur=(\d+)\s+min=(\d+)\s+max=(\d+)\s+hwmax=(\d+)""")

fun parseCpuPolicies(raw: String): List<CpuPolicy> {
    val out = mutableListOf<CpuPolicy>()
    for (line in raw.lineSequence()) {
        val m = CPU_POLICY_LINE.matchEntire(line.trim()) ?: continue
        out.add(
            CpuPolicy(
                id = m.groupValues[1],
                governor = m.groupValues[2],
                curFreqKb = m.groupValues[3].toLong(),
                minFreqKb = m.groupValues[4].toLong(),
                maxFreqKb = m.groupValues[5].toLong(),
                hwmaxKb = m.groupValues[6].toLong(),
            ),
        )
    }
    return out
}

// ---------------------------------------------------------------------------
// /proc/gpufreqv2/gpufreq_status: "[GPU   OPP]      Index: 64, Freq:  265000, Volt:  48750, ..."
// ---------------------------------------------------------------------------

private val GPU_LINE =
    Regex("""\[GPU\s+OPP\].*?Index:\s*(\d+),\s*Freq:\s*(\d+),\s*Volt:\s*(\d+)""")

fun parseGpuStatus(raw: String): GpuStatus? {
    for (line in raw.lineSequence()) {
        val m = GPU_LINE.find(line) ?: continue
        return GpuStatus(
            freqKhz = m.groupValues[2].toLong(),
            oppIndex = m.groupValues[1].toInt(),
            voltageUv = m.groupValues[3].toLong(),
        )
    }
    return null
}

/** Parses a bare GPU frequency in kHz (from the `ged` sysfs fallback). */
fun parseGpuFreqKhz(raw: String): Long? =
    raw.trim().toLongOrNull()?.let { if (it > 1_000_000) it / 1000 else it }

/** Parses a bare GPU utilization percentage (0-100). */
fun parseGpuUtil(raw: String): Int? = raw.trim().toIntOrNull()?.coerceIn(0, 100)

/** Parses `/proc/loadavg` first field (1-minute load average). */
fun parseLoadAverage(raw: String): Double? =
    raw.trim().split(Regex("""\s+""")).firstOrNull()?.toDoubleOrNull()

/** Rounds a MHz/kHz double to one decimal, for display. */
fun freqGhz(freqKb: Long): Double = (freqKb / 1_000_000.0).round(1)

private fun Double.round(digits: Int): Double =
    Math.pow(10, digits).let { factor -> (this * factor).roundToInt() / factor }
