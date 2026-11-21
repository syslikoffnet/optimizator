package com.duchamp.monitor
import androidx.lifecycle.ViewModel

import com.duchamp.device.DeviceReader
import com.duchamp.data.repo.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/** A single 1 Hz telemetry sample for the live chart. */
data class TelemetrySample(
    val t: Long,
    val cpuKb: Long,
    val gpuKhz: Long,
    val tempC: Double,
    val memKb: Long,
    val fps: Int,
)

/**
 * Samples the device once per second while the screen is open and feeds the live chart. When a
 * game-mode session is active it also persists the samples, so the post-game report has real data.
 */
@HiltViewModel
class MonitorViewModel : ViewModel() @Inject constructor(
    private val reader: DeviceReader,
    private val sessionRepository: SessionRepository,
) {
    private var job: Job? = null

    private val _samples = MutableStateFlow<List<TelemetrySample>>(emptyList())
    val samples: StateFlow<List<TelemetrySample>> = _samples.asStateFlow()

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    fun start(sessionId: Long?) {
        if (job?.isActive == true) return
        _active.value = true
        job = viewModelScope.launch {
            val max = 180
            while (isActive) {
                runCatching {
                    val cpu = reader.readCpuPolicies().firstOrNull()
                    val gpu = reader.readGpuFreqKhz() ?: 0L
                    val temp = reader.readThermalZones().firstOrNull()?.tempCelsius ?: 0.0
                    val mem = reader.readMemInfo()?.memAvailableKb ?: 0L
                    val sample = TelemetrySample(
                        t = System.currentTimeMillis(),
                        cpuKb = cpu?.curFreqKb ?: 0L,
                        gpuKhz = gpu,
                        tempC = temp,
                        memKb = mem,
                        fps = 0,
                    )
                    _samples.value = (_samples.value + sample).takeLast(max)
                    sessionId?.let { sessionRepository.addSample(
                        com.duchamp.data.db.MetricSampleEntity(
                            sessionId = it,
                            timestampMillis = sample.t,
                            cpu0FreqKb = sample.cpuKb,
                            cpu4FreqKb = 0,
                            cpu7FreqKb = 0,
                            cpuMax0Kb = 0,
                            cpuMax4Kb = 0,
                            cpuMax7Kb = 0,
                            gpuFreqKhz = sample.gpuKhz,
                            gpuUtil = 0,
                            socTempC = sample.tempC,
                            bigTempC = 0.0,
                            gpuTempC = 0.0,
                            batteryTempC = 0.0,
                            thermalStatus = 0,
                            batteryCurrent = 0,
                            batteryPower = 0.0,
                            memAvailableKb = sample.memKb,
                            swapUsedKb = 0,
                            fps = sample.fps,
                        ),
                    ) }
                }
                delay(1000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _active.value = false
    }

    fun clear() {
        _samples.value = emptyList()
    }

}
