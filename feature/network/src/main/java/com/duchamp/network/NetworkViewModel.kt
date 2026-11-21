package com.duchamp.network

import com.duchamp.device.DeviceReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Network state: Wi-Fi status, gateway, latency to the game server, VPN presence. */
data class NetworkSnapshot(
    val wifi: String = "",
    val gateway: String = "",
    val pingMs: Double? = null,
    val pingLoss: Double? = null,
    val vpnActive: Boolean = false,
    val vpnTransport: String = "",
    val readAtMillis: Long = 0L,
)

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val reader: DeviceReader,
) {
    private val _snapshot = MutableStateFlow(NetworkSnapshot())
    val snapshot: StateFlow<NetworkSnapshot> = _snapshot.asStateFlow()

    private val _pinging = MutableStateFlow(false)
    val pinging: StateFlow<Boolean> = _pinging.asStateFlow()

    /** Reads Wi-Fi, gateway and VPN state. */
    suspend fun refresh() {
        runCatching {
            val wifi = reader.readWifiStatus()
            val gateway = reader.readGateway()
            val vpn = reader.readVpn()
            _snapshot.value = NetworkSnapshot(
                wifi = wifi?.let { "${it.connectedSsid} · ${it.rssi} дБм · ${it.linkSpeed}" } ?: "неизвестно",
                gateway = gateway ?: "—",
                vpnActive = vpn.active,
                vpnTransport = vpn.transport,
                readAtMillis = System.currentTimeMillis(),
            )
        }
    }

    /** Measures latency to the given host. */
    suspend fun ping(host: String) {
        _pinging.value = true
        runCatching {
            val result = reader.readPing(host, count = 30)
            _snapshot.value = _snapshot.value.copy(
                pingMs = result?.avgMs,
                pingLoss = result?.lossPercent,
                readAtMillis = System.currentTimeMillis(),
            )
        }
        _pinging.value = false
    }
}
