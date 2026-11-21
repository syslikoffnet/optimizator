package com.duchamp.data

import com.duchamp.data.db.JournalDao
import com.duchamp.data.db.JournalEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [JournalDao] for unit tests. */
class FakeJournalDao : JournalDao {
    private val _all = MutableStateFlow<List<JournalEntryEntity>>(emptyList())
    val all: Flow<List<JournalEntryEntity>> = _all.asStateFlow()

    private var nextId = 1L
    private val entries = mutableListOf<JournalEntryEntity>()

    override suspend fun insert(entry: JournalEntryEntity): Long {
        val id = if (entry.id == 0L) nextId++ else entry.id
        val stored = entry.copy(id = id)
        entries.add(stored)
        _all.value = entries.toList()
        return id
    }

    override fun observeAll(): Flow<List<JournalEntryEntity>> = _all

    override suspend fun byStatus(status: String): List<JournalEntryEntity> =
        entries.filter { it.status == status }

    override suspend fun forTweak(tweakId: String): List<JournalEntryEntity> =
        entries.filter { it.tweakId == tweakId }

    override suspend fun forSession(sessionId: Long): List<JournalEntryEntity> =
        entries.filter { it.sessionId == sessionId }

    override suspend fun tweaksByStatus(sessionId: Long, status: String): List<String> =
        entries.filter { it.sessionId == sessionId && it.status == status }
            .map { it.tweakId }
            .distinct()

    override suspend fun updateStatus(id: Long, status: String) {
        entries.forEachIndexed { i, e -> if (e.id == id) entries[i] = e.copy(status = status) }
        _all.value = entries.toList()
    }

    override suspend fun updateResult(
        id: Long,
        exitCode: Int,
        stdout: String,
        stderr: String,
        durationMs: Long,
        status: String,
    ) {
        entries.forEachIndexed { i, e ->
            if (e.id == id) {
                entries[i] = e.copy(
                    exitCode = exitCode,
                    stdout = stdout,
                    stderr = stderr,
                    durationMs = durationMs,
                    status = status,
                )
            }
        }
        _all.value = entries.toList()
    }

    override suspend fun deleteForTweak(tweakId: String) {
        entries.removeAll { it.tweakId == tweakId }
        _all.value = entries.toList()
    }

    override suspend fun deleteForSession(sessionId: Long) {
        entries.removeAll { it.sessionId == sessionId }
        _all.value = entries.toList()
    }
}
