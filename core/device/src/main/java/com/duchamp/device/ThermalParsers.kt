package com.duchamp.device

// ---------------------------------------------------------------------------
// Thermal zones: "thermal_zone0 soc_max 56222" (one per line)
// ---------------------------------------------------------------------------

fun parseThermalZones(raw: String): List<ThermalZone> {
    val out = mutableListOf<ThermalZone>()
    for (line in raw.lineSequence()) {
        val parts = line.trim().split(Regex("""\s+"""))
        if (parts.size < 3) continue
        val name = parts[1]
        val temp = parts[2].toIntOrNull() ?: continue
        out.add(ThermalZone(name = name, tempMilli = temp))
    }
    return out
}

/** Reads a single thermal zone temp (milli-degrees C). Returns null when unreadable. */
fun parseThermalTempMilli(raw: String): Int? = raw.trim().toIntOrNull()

// ---------------------------------------------------------------------------
// dumpsys thermalservice
// ---------------------------------------------------------------------------

private val THERMAL_STATUS = Regex("""Thermal Status:\s*(\d+)""")
private val THERMAL_READING =
    Regex(
        """Temperature\{mValue=([\d.]+),\s*mType=(\d+),\s*mName=([^,]+),\s*mStatus=(\d+)\}""",
    )

fun parseThermalService(raw: String): ThermalService? {
    val status = THERMAL_STATUS.find(raw)?.groupValues?.get(1)?.toIntOrNull() ?: return null
    val readings = mutableListOf<ThermalReading>()
    for (m in THERMAL_READING.findAll(raw)) {
        readings.add(
            ThermalReading(
                value = m.groupValues[1].toDoubleOrNull() ?: 0.0,
                type = m.groupValues[2].toIntOrNull() ?: 0,
                name = m.groupValues[3],
                status = m.groupValues[4].toIntOrNull() ?: 0,
            ),
        )
    }
    return ThermalService(status = status, readings = readings)
}

/** Human-readable thermal status label. */
fun thermalStatusLabel(status: Int): String = when (status) {
    0 -> "Нет"
    1 -> "Лёгкое торможение"
    2 -> "Торможение"
    3 -> "Сильное торможение"
    4 -> "Сверхтонкое"
    5 -> "Критично"
    6 -> "Выключение"
    else -> "Неизвестно"
}
