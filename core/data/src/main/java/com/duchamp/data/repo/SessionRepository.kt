package com.duchamp.data.repo

import com.duchamp.data.db.EventDao
import com.duchamp.data.db.EventMarkerEntity
import com.duchamp.data.db.MetricSampleDao
import com.duchamp.data.db.MetricSampleEntity
import com.duchamp.data.db.SessionDao
import com.duchamp.data.db.SessionEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionRepositoryModule {
    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: SessionDao,
        metricSampleDao: MetricSampleDao,
        eventDao: EventDao,
    ): SessionRepository = SessionRepository(sessionDao, metricSampleDao, eventDao)
}

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val metricSampleDao: MetricSampleDao,
    private val eventDao: EventDao,
) : SessionStore {

    fun observeSessions(): Flow<List<SessionEntity>> = sessionDao.observeAll()

    override suspend fun startSession(startMillis: Long): Long =
        sessionDao.insert(
            SessionEntity(
                startMillis = startMillis,
                endMillis = 0,
                durationSec = 0,
                avgFps = 0.0,
                low1Fps = 0.0,
                peakSocTempC = 0.0,
                throttleStartSec = -1,
                avgPingMs = 0.0,
                batteryDeltaPercent = 0.0,
                batteryBypass = false,
                summary = "",
            ),
        )

    suspend fun addSample(sample: MetricSampleEntity) {
        metricSampleDao.insert(sample)
    }

    suspend fun addSamples(samples: List<MetricSampleEntity>) {
        metricSampleDao.insertAll(samples)
    }

    override suspend fun addEvent(sessionId: Long, timestampMillis: Long, type: String, label: String) {
        eventDao.insert(EventMarkerEntity(sessionId = sessionId, timestampMillis = timestampMillis, type = type, label = label))
    }

    /** Close a session with its final timestamps and summary. */
    override suspend fun finish(sessionId: Long, endMillis: Long, summary: String) {
        val started = sessionDao.getById(sessionId)
        sessionDao.finish(
            id = sessionId,
            endMillis = endMillis,
            durationSec = maxOf(0L, (endMillis - (started?.startMillis ?: endMillis)) / 1000),
            summary = summary,
        )
    }

    override suspend fun updateSummary(sessionId: Long, summary: String) {
        sessionDao.updateSummary(sessionId, summary)
    }

    suspend fun finishSession(session: SessionEntity): Long =
        sessionDao.insert(session.copy(id = 0, endMillis = session.endMillis))

    suspend fun samplesFor(sessionId: Long): List<MetricSampleEntity> =
        metricSampleDao.forSession(sessionId)

    suspend fun eventsFor(sessionId: Long): List<EventMarkerEntity> =
        eventDao.forSession(sessionId)

    suspend fun recent(limit: Int = 50): List<SessionEntity> = sessionDao.recent(limit)
}
