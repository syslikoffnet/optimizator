package com.duchamp.sessions
import androidx.lifecycle.ViewModel

import com.duchamp.data.db.EventMarkerEntity
import com.duchamp.data.db.MetricSampleEntity
import com.duchamp.data.db.SessionEntity
import com.duchamp.data.repo.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** A session with its telemetry and events, ready to render in the post-game report. */
data class SessionReport(
    val session: SessionEntity,
    val samples: List<MetricSampleEntity>,
    val events: List<EventMarkerEntity>,
) {
    val durationMin: Int get() = (session.durationSec / 60).coerceAtLeast(0).toInt()

    val avgCpuMhz: Double
        get() = samples.map { it.cpu0FreqKb / 1000.0 }.average().takeIf { samples.isNotEmpty() } ?: 0.0

    val peakTempC: Double
        get() = samples.map { it.socTempC }.maxOrNull() ?: 0.0

    val throttleStart: Long get() = session.throttleStartSec
}

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions: StateFlow<List<SessionEntity>> = _sessions.asStateFlow()

    private val _report = MutableStateFlow<SessionReport?>(null)
    val report: StateFlow<SessionReport?> = _report.asStateFlow()

    /** Loads the most recent sessions and auto-opens the newest one. */
    suspend fun load() {
        _sessions.value = sessionRepository.recent(50)
        val newest = _sessions.value.firstOrNull()
        if (newest != null) open(newest.id)
    }

    suspend fun open(sessionId: Long) {
        val session = sessionRepository.recent(50).firstOrNull { it.id == sessionId }
            ?: sessionRepository.recent(1).firstOrNull { it.id == sessionId }
            ?: return
        _report.value = SessionReport(
            session = session,
            samples = sessionRepository.samplesFor(session.id),
            events = sessionRepository.eventsFor(session.id),
        )
    }

    fun closeReport() {
        _report.value = null
    }
}
