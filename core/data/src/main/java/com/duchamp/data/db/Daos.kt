package com.duchamp.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY startMillis DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): SessionEntity?

    @Query("UPDATE sessions SET endMillis = :endMillis, durationSec = :durationSec, summary = :summary WHERE id = :id")
    suspend fun finish(
        id: Long,
        endMillis: Long,
        durationSec: Long,
        summary: String,
    )

    @Query("UPDATE sessions SET summary = :summary WHERE id = :id")
    suspend fun updateSummary(id: Long, summary: String)

    @Query("SELECT * FROM sessions ORDER BY startMillis DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<SessionEntity>
}

@Dao
interface MetricSampleDao {
    @Insert
    suspend fun insert(sample: MetricSampleEntity): Long

    @Insert
    suspend fun insertAll(samples: List<MetricSampleEntity>)

    @Query("SELECT * FROM metric_samples WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    suspend fun forSession(sessionId: Long): List<MetricSampleEntity>

    @Query("DELETE FROM metric_samples WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}

@Dao
interface JournalDao {
    @Insert
    suspend fun insert(entry: JournalEntryEntity): Long

    @Query("SELECT * FROM journal ORDER BY atMillis DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal WHERE status = :status ORDER BY atMillis ASC")
    suspend fun byStatus(status: String): List<JournalEntryEntity>

    @Query("SELECT * FROM journal WHERE tweakId = :tweakId ORDER BY atMillis DESC")
    suspend fun forTweak(tweakId: String): List<JournalEntryEntity>

    @Query("SELECT * FROM journal WHERE sessionId = :sessionId ORDER BY atMillis ASC")
    suspend fun forSession(sessionId: Long): List<JournalEntryEntity>

    @Query("SELECT DISTINCT tweakId FROM journal WHERE sessionId = :sessionId AND status = :status ORDER BY atMillis ASC")
    suspend fun tweaksByStatus(sessionId: Long, status: String): List<String>

    @Query("UPDATE journal SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query(
        "UPDATE journal SET exitCode = :exitCode, stdout = :stdout, stderr = :stderr, " +
            "durationMs = :durationMs, status = :status WHERE id = :id",
    )
    suspend fun updateResult(
        id: Long,
        exitCode: Int,
        stdout: String,
        stderr: String,
        durationMs: Long,
        status: String,
    )

    @Query("DELETE FROM journal WHERE tweakId = :tweakId")
    suspend fun deleteForTweak(tweakId: String)

    @Query("DELETE FROM journal WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventMarkerEntity): Long

    @Query("SELECT * FROM events WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    suspend fun forSession(sessionId: Long): List<EventMarkerEntity>

    @Query("DELETE FROM events WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}

@Dao
interface StateDao {
    @Upsert
    suspend fun put(entry: StateEntryEntity)

    @Query("SELECT * FROM state WHERE key = :key")
    suspend fun get(key: String): StateEntryEntity?

    @Query("SELECT * FROM state")
    suspend fun all(): List<StateEntryEntity>

    @Query("DELETE FROM state")
    suspend fun clear()
}
