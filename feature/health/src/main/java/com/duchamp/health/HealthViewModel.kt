package com.duchamp.health
import androidx.lifecycle.ViewModel

import com.duchamp.device.DeviceReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Battery + thermal health summary, read once per refresh. */
data class HealthSnapshot(
    val status: String = "",
    val healthPercent: Double? = null,
    val cycleCount: Int = 0,
    val chargeFull: Int = 0,
    val chargeFullDesign: Int = 0,
    val currentNow: Int = 0,
    val currentCmd: String = "",
    val isBypass: Boolean = false,
    val peakTempC: Double = 0.0,
    val thermalZones: List<String> = emptyList(),
    val powerEstimate: Double? = null,
    val readAtMillis: Long = 0L,
)

@HiltViewModel
class HealthViewModel : ViewModel() @Inject constructor(
    private val reader: DeviceReader,
) {
    private val _snapshot = MutableStateFlow(HealthSnapshot())
    val snapshot: StateFlow<HealthSnapshot> = _snapshot.asStateFlow()

    /** Reads battery sysfs, thermal zones and the power estimate. Never throws. */
    suspend fun refresh() {
        runCatching {
            val battery = reader.readBatterySysfs()
            val zones = reader.readThermalZones()
            val power = reader.readPowerEstimate()?.computedDrainMah
            _snapshot.value = HealthSnapshot(
                status = battery.status,
                healthPercent = battery.healthPercent,
                cycleCount = battery.cycleCount,
                chargeFull = battery.chargeFull,
                chargeFullDesign = battery.chargeFullDesign,
                currentNow = battery.currentNow,
                currentCmd = battery.currentCmd,
                isBypass = battery.isBypass,
                peakTempC = zones.mapNotNull { it.tempCelsius }.maxOrNull() ?: 0.0,
                thermalZones = zones.map { it.name },
                powerEstimate = power,
                readAtMillis = System.currentTimeMillis(),
            )
        }
    }
}
