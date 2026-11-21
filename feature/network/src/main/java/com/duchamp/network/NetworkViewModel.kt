package com.duchamp.network

import androidx.lifecycle.ViewModel

import com.duchamp.device.DeviceReader
import com.duchamp.root.CommandRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Network state: Wi-Fi status, gateway, latency to the gateway, low-latency mode, VPN. */
data class NetworkSnapshot(
    val wifi: String = "",
    val gateway: String = "",
    val pingMs: Double? = null,
    val pingLoss: Double? = null,
    /** Latency with Wi-Fi low-latency mode OFF (the "before" half of the A/B test). */
    val pingOffMs: Double? = null,
    /** Latency with Wi-Fi low-latency mode ON (the "after" half of the A/B test). */
    val pingOnMs: Double? = null,
    val vpnActive: Boolean = false,
    val vpnTransport: String = "",
    val lowLatency: Boolean = false,
    val readAtMillis: Long = 0L,
)

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val reader: DeviceReader,
    private val runner: CommandRunner,
) : ViewModel() {
    private val _snapshot = MutableStateFlow(NetworkSnapshot())
    val snapshot: StateFlow<NetworkSnapshot> = _snapshot.asStateFlow()

    private val _pinging = MutableStateFlow(false)
    val pinging: StateFlow<Boolean> = _pinging.asStateFlow()

    private val _busy = MutableStateFlow("")
    val busy: StateFlow<String> = _busy.asStateFlow()

    /** Reads Wi-Fi, gateway, VPN and low-latency state. */
    suspend fun refresh() {
        runCatching {
            val wifi = reader.readWifiStatus()
            val gateway = reader.readGateway()
            val vpn = reader.readVpn()
            _snapshot.value = _snapshot.value.copy(
                wifi = wifi?.let { "${it.connectedSsid} · ${it.rssi} дБм · ${it.linkSpeed}" } ?: "неизвестно",
                gateway = gateway?.ifBlank { "—" } ?: "—",
                vpnActive = vpn.active,
                vpnTransport = vpn.transport,
                readAtMillis = System.currentTimeMillis(),
            )
        }
    }

    /**
     * Measures latency to the gateway — the one number that reflects real Wi-Fi quality. Pinging an
     * external host (e.g. a game server) is unreliable: DNS can fail, and a VPN completes the
     * handshake locally so the result is fake. The gateway is on the local link and unaffected by
     * the VPN, so it is always trustworthy.
     */
    suspend fun ping() {
        val gateway = _snapshot.value.gateway
        if (gateway.isBlank() || gateway == "—") return
        _pinging.value = true
        runCatching {
            val result = reader.readPing(gateway, count = 30)
            _snapshot.value = _snapshot.value.copy(
                pingMs = result?.avgMs,
                pingLoss = result?.lossPercent,
                readAtMillis = System.currentTimeMillis(),
            )
        }
        _pinging.value = false
    }

    /** Toggles the Wi-Fi low-latency mode. State is tracked in-memory (the system may not expose it). */
    suspend fun setLowLatency(enabled: Boolean) {
        _busy.value = "low-latency"
        runCatching {
            val cmd = if (enabled) "cmd wifi force-low-latency-mode enabled"
            else "cmd wifi force-low-latency-mode disabled"
            runner.run(cmd)
            _snapshot.value = _snapshot.value.copy(lowLatency = enabled)
        }
        _busy.value = ""
    }

    /**
     * A/B test: ping the gateway with low-latency OFF, then ON, and show both. This is the one
     * measurement that shows whether the mode actually helps on this device.
     */
    suspend fun compare() {
        val gateway = _snapshot.value.gateway
        if (gateway.isBlank() || gateway == "—") return
        _pinging.value = true
        runCatching {
            // Force OFF first so both halves are measured under a known state.
            runCatching { runner.run("cmd wifi force-low-latency-mode disabled") }
            val off = reader.readPing(gateway, count = 30)
            runCatching { runner.run("cmd wifi force-low-latency-mode enabled") }
            val on = reader.readPing(gateway, count = 30)
            _snapshot.value = _snapshot.value.copy(
                pingOffMs = off?.avgMs,
                pingOnMs = on?.avgMs,
                pingLoss = on?.lossPercent,
                lowLatency = true,
                readAtMillis = System.currentTimeMillis(),
            )
        }
        _pinging.value = false
    }
}
