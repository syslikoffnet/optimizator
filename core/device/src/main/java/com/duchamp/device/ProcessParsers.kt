package com.duchamp.device

// ---------------------------------------------------------------------------
// Processes. DeviceReader emits "<pid> <name> <rssKb> <swapKb>" per line.
// ---------------------------------------------------------------------------

fun parseProcesses(raw: String): List<AppProcess> {
    val out = mutableListOf<AppProcess>()
    for (line in raw.lineSequence()) {
        val parts = line.trim().split(Regex("""\s+"""))
        if (parts.size < 4) continue
        val pid = parts[0].toIntOrNull() ?: continue
        val name = parts[1]
        val rss = parts[2].toLongOrNull() ?: 0L
        val swap = parts[3].toLongOrNull() ?: 0L
        out.add(AppProcess(pid = pid, name = name, rssKb = rss, swapKb = swap, cpuPercent = 0.0))
    }
    return out
}

/** Parses `top -b -n 1 -m 15` CPU lines into name + cpuPercent. */
fun parseCpuTop(raw: String): List<Pair<String, Double>> {
    val out = mutableListOf<Pair<String, Double>>()
    val headerSeen = false
    for (line in raw.lineSequence()) {
        val t = line.trim()
        if (t.isEmpty()) continue
        val parts = t.split(Regex("""\s+"""))
        // Skip header lines that do not start with a numeric PID.
        if (!parts.first().toIntOrNull().let { it != null && it > 0 }) continue
        if (parts.size < 12) continue
        val cpu = parts[8].toDoubleOrNull() ?: continue
        val name = parts.last()
        out.add(name to cpu)
    }
    return out
}
