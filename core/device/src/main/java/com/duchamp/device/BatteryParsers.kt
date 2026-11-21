package com.duchamp.device

// ---------------------------------------------------------------------------
// Battery sysfs: individual values are parsed from single-value reads.
// ---------------------------------------------------------------------------

/** `status` field is a string: Charging / Not charging / Discharging / Full. */
fun parseBatteryStatus(raw: String): String = raw.trim()

fun parseBatteryInt(raw: String): Int = raw.trim().toIntOrNull() ?: 0

fun parseBatteryCmd(raw: String): String = raw.trim()

// ---------------------------------------------------------------------------
// dumpsys battery
// ---------------------------------------------------------------------------

private val AC_POWERED = Regex("""AC powered:\s*(true|false)""")
private val USB_POWERED = Regex("""USB powered:\s*(true|false)""")
private val STATUS = Regex("""status:\s*(\d+)""")
private val LEVEL = Regex("""level:\s*(\d+)""")
private val VOLTAGE = Regex("""voltage:\s*(\d+)""")
private val TEMP = Regex("""temperature:\s*(\d+)""")

fun parseBatteryDumpsys(raw: String): BatteryDumpsys? {
    val ac = AC_POWERED.find(raw)?.groupValues?.get(1)?.lowercase()
    val usb = USB_POWERED.find(raw)?.groupValues?.get(1)?.lowercase()
    val status = STATUS.find(raw)?.groupValues?.get(1)?.toIntOrNull()
    val level = LEVEL.find(raw)?.groupValues?.get(1)?.toIntOrNull()
    val voltage = VOLTAGE.find(raw)?.groupValues?.get(1)?.toIntOrNull()
    val temp = TEMP.find(raw)?.groupValues?.get(1)?.toIntOrNull()
    if (ac == null || status == null) return null
    return BatteryDumpsys(
        acPowered = ac == "true",
        usbPowered = usb == "true",
        status = status,
        level = level ?: 0,
        voltageMv = voltage ?: 0,
        // dumpsys reports temperature in tenths of a degree Celsius.
        temperatureCelsius = temp?.toDouble()?.div(10.0) ?: 0.0,
    )
}

// ---------------------------------------------------------------------------
// dumpsys batterystats -> "Estimated power use (mAh)"
// ---------------------------------------------------------------------------

private val POWER_CAPACITY = Regex("""Capacity:\s*([\d.]+)""")
private val POWER_COMPUTED = Regex("""Computed drain:\s*([\d.]+)""")
private val POWER_ACTUAL = Regex("""actual drain:\s*([\d.]+)-([\d.]+)""")
private val UID_LINE = Regex("""UID\s+(\S+):\s*([\d.]+)""")

fun parsePowerEstimate(raw: String): PowerEstimate? {
    if (!raw.contains("Estimated power use")) return null
    val capacity = POWER_CAPACITY.find(raw)?.groupValues?.get(1)?.toDoubleOrNull()
    val computed = POWER_COMPUTED.find(raw)?.groupValues?.get(1)?.toDoubleOrNull()
    if (capacity == null) return null
    val actual = POWER_ACTUAL.find(raw)?.let {
        Pair(it.groupValues[1].toDoubleOrNull() ?: 0.0, it.groupValues[2].toDoubleOrNull() ?: 0.0)
    }
    val perUid = mutableListOf<UidPower>()
    for (m in UID_LINE.findAll(raw)) {
        perUid.add(UidPower(uid = m.groupValues[1], mAh = m.groupValues[2].toDoubleOrNull() ?: 0.0, screenHours = null))
    }
    return PowerEstimate(
        capacityMah = capacity,
        computedDrainMah = computed ?: 0.0,
        actualDrainRange = actual,
        perUid = perUid,
    )
}
