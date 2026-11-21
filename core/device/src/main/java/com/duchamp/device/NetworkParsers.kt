package com.duchamp.device

// ---------------------------------------------------------------------------
// ping summary:
//   50 packets transmitted, 50 received, 0% packet loss, time 9843ms
//   rtt min/avg/max/mdev = 1.081/1.787/3.240/0.530 ms
// ---------------------------------------------------------------------------

private val PING_COUNTS = Regex("""(\d+)\s+packets transmitted,\s*(\d+)\s+received,\s*(\d+)%\s*packet loss""")
private val PING_RTT = Regex("""rtt min/avg/max/mdev\s*=\s*([\d.]+)/([\d.]+)/([\d.]+)/([\d.]+)""")

fun parsePing(raw: String): PingResult? {
    val counts = PING_COUNTS.find(raw)
    val rtt = PING_RTT.find(raw)
    if (counts == null || rtt == null) return null
    return PingResult(
        transmitted = counts.groupValues[1].toIntOrNull() ?: 0,
        received = counts.groupValues[2].toIntOrNull() ?: 0,
        lossPercent = counts.groupValues[3].toDoubleOrNull() ?: 0.0,
        minMs = rtt.groupValues[1].toDoubleOrNull() ?: 0.0,
        avgMs = rtt.groupValues[2].toDoubleOrNull() ?: 0.0,
        maxMs = rtt.groupValues[3].toDoubleOrNull() ?: 0.0,
        mdevMs = rtt.groupValues[4].toDoubleOrNull() ?: 0.0,
    )
}

// ---------------------------------------------------------------------------
// cmd wifi status
// ---------------------------------------------------------------------------

private val WIFI_SSID = Regex("""Wifi is connected to "([^"]*)"|SSID:\s*"([^"]*)"""")
private val WIFI_RSSI = Regex("""RSSI:\s*(-?\d+)""")
private val WIFI_LINK = Regex("""Link speed:\s*(\d+(?:\.\d+)?\s*Mbps)""")
private val WIFI_FREQ = Regex("""Frequency:\s*(\d+)MHz""")
private val WIFI_STANDARD = Regex("""Wi-Fi standard:\s*(\S+)""")

fun parseWifiStatus(raw: String): WifiStatus? {
    val ssid = WIFI_SSID.find(raw)?.let { it.groupValues.firstOrNull { g -> g.isNotEmpty() } }
    val rssi = WIFI_RSSI.find(raw)?.groupValues?.get(1)?.toIntOrNull()
    val link = WIFI_LINK.find(raw)?.groupValues?.get(1)
    val freq = WIFI_FREQ.find(raw)?.groupValues?.get(1)?.toIntOrNull()
    val standard = WIFI_STANDARD.find(raw)?.groupValues?.get(1)
    if (rssi == null) return null
    return WifiStatus(
        connectedSsid = ssid ?: "",
        rssi = rssi,
        linkSpeed = link ?: "",
        frequencyMhz = freq ?: 0,
        standard = standard ?: "",
    )
}

/** Extracts the default gateway IP from `ip route`. */
fun parseGateway(raw: String): String? {
    for (line in raw.lineSequence()) {
        val m = Regex("""default via (\d+\.\d+\.\d+\.\d+)""").find(line) ?: continue
        return m.groupValues[1]
    }
    return null
}

// ---------------------------------------------------------------------------
// dumpsys connectivity -> active VPN transport
// ---------------------------------------------------------------------------

private val VPN_TRANSPORT = Regex("""Transports:\s*VPN[^\n]*?Uids:\s*([\d,\s]+)""", RegexOption.IGNORE_CASE)

fun parseVpn(raw: String): VpnInfo {
    val m = VPN_TRANSPORT.find(raw)
    if (m == null) return VpnInfo(active = false, transport = "", uids = emptyList())
    val uids = m.groupValues[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return VpnInfo(active = true, transport = "VPN", uids = uids)
}

/** Extracts `userId=` from `dumpsys package <pkg>`. */
fun parsePackageUserId(raw: String): Int? =
    Regex("""userId=(\d+)""").find(raw)?.groupValues?.get(1)?.toIntOrNull()

/** Extracts `enabled=` from `dumpsys package <pkg>`. */
fun parsePackageEnabled(raw: String): Boolean? {
    val m = Regex("""enabled=(\w+)""").find(raw) ?: return null
    return m.groupValues[1].lowercase() != "false"
}

/** Extracts `suspended=` from `dumpsys package <pkg>`. */
fun parsePackageSuspended(raw: String): Boolean? {
    val m = Regex("""suspended=(\w+)""").find(raw) ?: return null
    return m.groupValues[1].lowercase() == "true"
}
