package com.duchamp.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser tests against the section-9 fixtures: real command output shapes as captured on duchamp.
 * Every parser is pure, so these run on the JVM with no device or root.
 */
class ParsersTest {

    @Test
    fun `parseMemInfo reads total available and swap`() {
        val raw = """
            MemTotal:       11940768 kB
            MemFree:          288544 kB
            MemAvailable:     671088 kB
            Buffers:          102400 kB
            Cached:           335544 kB
            SReclaimable:     102400 kB
            SwapTotal:        4194300 kB
            SwapFree:         3145728 kB
        """.trimIndent()
        val mem = parseMemInfo(raw)
        assertNotNull(mem)
        assertEquals(11940768L, mem!!.memTotalKb)
        assertEquals(671088L, mem.memAvailableKb)
        assertEquals(288544L, mem.memFreeKb)
        assertEquals(4194300L, mem.swapTotalKb)
        assertEquals(3145728L, mem.swapFreeKb)
    }

    @Test
    fun `parseMemInfo returns null on garbage`() {
        assertNull(parseMemInfo("not a meminfo at all"))
    }

    @Test
    fun `parseSwaps reads each swap line`() {
        val raw = """
            Filename                            Type        Size        Used        Priority
            /dev/block/dm-0                       file        4194304     1048576     -2
            /dev/block/dm-1                       partition    8388608     2097152     -3
        """.trimIndent()
        val swaps = parseSwaps(raw)
        assertEquals(2, swaps.size)
        assertEquals("/dev/block/dm-0", swaps[0].file)
        assertEquals("file", swaps[0].type)
        assertEquals(4194304L, swaps[0].sizeKb)
        assertEquals(1048576L, swaps[0].usedKb)
        assertEquals(-2, swaps[0].priority)
    }

    @Test
    fun `parseCpuPolicies reads governor and freqs`() {
        val raw = """
            policy0 gov=sugov_ext cur=1300000 min=700000 max=1300000 hwmax=2200000
            policy4 gov=sugov_ext cur=1100000 min=700000 max=1100000 hwmax=2200000
        """.trimIndent()
        val policies = parseCpuPolicies(raw)
        assertEquals(2, policies.size)
        assertEquals("policy0", policies[0].id)
        assertEquals("sugov_ext", policies[0].governor)
        assertEquals(1300000L, policies[0].curFreqKb)
        assertEquals(2200000L, policies[0].hwmaxKb)
        assertTrue(policies[0].throttled) // max < hwmax
    }

    @Test
    fun `parseGpuStatus reads opp index freq and voltage`() {
        val raw = "[GPU   OPP]      Index: 64, Freq:  265000, Volt:  48750, Temp:  45"
        val gpu = parseGpuStatus(raw)
        assertNotNull(gpu)
        assertEquals(64, gpu!!.oppIndex)
        assertEquals(265000L, gpu.freqKhz)
        assertEquals(48750L, gpu.voltageUv)
    }

    @Test
    fun `parseGpuFreqKhz scales large values`() {
        assertEquals(265000L, parseGpuFreqKhz("265000"))
        assertEquals(265000L, parseGpuFreqKhz("265000000"))
    }

    @Test
    fun `parseThermalZones reads name and temp`() {
        val raw = "thermal_zone0 soc_max 56222"
        val zones = parseThermalZones(raw)
        assertEquals(1, zones.size)
        assertEquals("soc_max", zones[0].name)
        assertEquals(56222, zones[0].tempMilli)
        assertEquals(56.222, zones[0].tempCelsius!!, 0.001)
    }

    @Test
    fun `parseThermalService extracts status and readings`() {
        val raw = """
            Thermal Status: 2
            Temperature{mValue=58.5, mType=0, mName=soc, mStatus=2}
            Temperature{mValue=52.0, mType=1, mName=gpu, mStatus=1}
        """.trimIndent()
        val service = parseThermalService(raw)
        assertNotNull(service)
        assertEquals(2, service!!.status)
        assertEquals(2, service.readings.size)
        assertEquals("soc", service.readings[0].name)
    }

    @Test
    fun `parsePing reads counts and rtt`() {
        val raw = """
            50 packets transmitted, 50 received, 0% packet loss, time 9843ms
            rtt min/avg/max/mdev = 1.081/1.787/3.240/0.530 ms
        """.trimIndent()
        val ping = parsePing(raw)
        assertNotNull(ping)
        assertEquals(50, ping!!.transmitted)
        assertEquals(50, ping.received)
        assertEquals(0.0, ping.lossPercent, 0.001)
        assertEquals(1.787, ping.avgMs, 0.001)
        assertEquals(3.240, ping.maxMs, 0.001)
    }

    @Test
    fun `parseWifiStatus reads ssid rssi and link`() {
        val raw = """
            Wifi is connected to "HomeNet"
            RSSI: -52
            Link speed: 866.7 Mbps
            Frequency: 5240MHz
            Wi-Fi standard: 802.11ac
        """.trimIndent()
        val wifi = parseWifiStatus(raw)
        assertNotNull(wifi)
        assertEquals("HomeNet", wifi!!.connectedSsid)
        assertEquals(-52, wifi.rssi)
        assertEquals("866.7 Mbps", wifi.linkSpeed)
        assertEquals(5240, wifi.frequencyMhz)
        assertFalse(wifi.isDfsChannel)
    }

    @Test
    fun `parseWifiStatus detects dfs channel`() {
        val raw = """
            Wifi is connected to "Backhaul"
            RSSI: -60
            Link speed: 600 Mbps
            Frequency: 5320MHz
            Wi-Fi standard: 802.11ac
        """.trimIndent()
        assertTrue(parseWifiStatus(raw)!!.isDfsChannel)
    }

    @Test
    fun `parseGateway extracts default route`() {
        val raw = """
            default via 192.168.1.1 dev wlan0 proto dhcp metric 300
            192.168.1.0/24 dev wlan0 proto kernel scope link src 192.168.1.42
        """.trimIndent()
        assertEquals("192.168.1.1", parseGateway(raw))
    }

    @Test
    fun `parseBatteryDumpsys reads level and temperature`() {
        val raw = """
            AC powered: true
            USB powered: false
            status: 2
            level: 87
            voltage: 4350
            temperature: 345
        """.trimIndent()
        val battery = parseBatteryDumpsys(raw)
        assertNotNull(battery)
        assertTrue(battery!!.acPowered)
        assertFalse(battery.usbPowered)
        assertEquals(2, battery.status)
        assertEquals(87, battery.level)
        assertEquals(34.5, battery.temperatureCelsius, 0.001)
    }

    @Test
    fun `parsePowerEstimate reads capacity and drain`() {
        val raw = """
            Estimated power use (mAh):
            Capacity: 5000
            Computed drain: 3200.5
            actual drain: 3100.0-3300.0
            UID 1000: 1500.0
            UID 2000: 900.0
        """.trimIndent()
        val power = parsePowerEstimate(raw)
        assertNotNull(power)
        assertEquals(5000.0, power!!.capacityMah, 0.001)
        assertEquals(3200.5, power.computedDrainMah, 0.001)
        assertEquals(Pair(3100.0, 3300.0), power.actualDrainRange)
        assertEquals(2, power.perUid.size)
    }

    @Test
    fun `parsePackageList strips package prefix`() {
        val raw = "package:com.example.app\npackage:com.other.app"
        assertEquals(listOf("com.example.app", "com.other.app"), parsePackageList(raw))
    }

    @Test
    fun `parsePackageUserId enabled suspended`() {
        val raw = "Package [com.example] (uid 10245 user 0):\n    userId=10245\n    enabled=true\n    suspended=false"
        assertEquals(10245, parsePackageUserId(raw))
        assertEquals(true, parsePackageEnabled(raw))
        assertEquals(false, parsePackageSuspended(raw))
    }

    @Test
    fun `parseProcesses reads pid name rss swap`() {
        val raw = "1234 com.android.systemui 102400 20480\n5678 com.example.game 300000 0"
        val processes = parseProcesses(raw)
        assertEquals(2, processes.size)
        assertEquals(1234, processes[0].pid)
        assertEquals("com.android.systemui", processes[0].name)
        assertEquals(102400L, processes[0].rssKb)
    }
}
