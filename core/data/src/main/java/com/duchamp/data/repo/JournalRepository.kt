package com.duchamp.data.repo

import com.duchamp.data.db.JournalDao
import com.duchamp.data.db.JournalEntryEntity
import com.duchamp.data.tweak.Tweak
import com.duchamp.root.CommandResult
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Journal lifecycle states. */
enum class JournalStatus {
    /** Written before the command ran; not yet confirmed. */
    PENDING,
    /** Command ran successfully and was verified. */
    APPLIED,
    /** Command failed. */
    FAILED,
    /** Reverted back to the pre-change state. */
    REVERTED,
}

@Module
@InstallIn(SingletonComponent::class)
object JournalRepositoryModule {
    @Provides
    @Singleton
    fun provideJournalRepository(dao: JournalDao): JournalRepository = JournalRepository(dao)
}

/**
 * Write-ahead journal. Every root action is recorded before it runs and updated with the real
 * exit code / output afterwards, so the log survives crashes and reboots and can drive revert.
 *
 * Entries are grouped by the session that performed them, which is what makes a game-mode run
 * revertable as a unit after a reboot.
 */
@Singleton
class JournalRepository @Inject constructor(
    private val dao: JournalDao,
) {

    fun observe(): Flow<List<JournalEntryEntity>> = dao.observeAll()

    /** Write the intent *before* running the command. Returns the entry id. */
    suspend fun recordPending(
        sessionId: Long,
        tweak: Tweak,
        command: String,
        atMillis: Long,
        previousRaw: String = "",
    ): Long = dao.insert(
        JournalEntryEntity(
            sessionId = sessionId,
            tweakId = tweak.id,
            title = tweak.title,
            command = command,
            exitCode = CommandResult.NO_CODE,
            stdout = "",
            stderr = "",
            durationMs = 0,
            atMillis = atMillis,
            status = JournalStatus.PENDING.name,
            risk = tweak.risk.name,
            previousRaw = previousRaw,
        ),
    )

    /** Mark a successfully applied command and store its real output. */
    suspend fun markApplied(id: Long, result: CommandResult) {
        dao.updateResult(
            id = id,
            exitCode = result.exitCode,
            stdout = result.stdout,
            stderr = result.stderr,
            durationMs = result.durationMs,
            status = JournalStatus.APPLIED.name,
        )
    }

    /** Mark a failed command and store its real output. */
    suspend fun markFailed(id: Long, result: CommandResult) {
        dao.updateResult(
            id = id,
            exitCode = result.exitCode,
            stdout = result.stdout,
            stderr = result.stderr,
            durationMs = result.durationMs,
            status = JournalStatus.FAILED.name,
        )
    }

    suspend fun markReverted(id: Long) {
        dao.updateStatus(id, JournalStatus.REVERTED.name)
    }

    suspend fun pending(): List<JournalEntryEntity> = dao.byStatus(JournalStatus.PENDING.name)

    suspend fun forTweak(tweakId: String): List<JournalEntryEntity> = dao.forTweak(tweakId)

    suspend fun forSession(sessionId: Long): List<JournalEntryEntity> = dao.forSession(sessionId)

    suspend fun applied(): List<JournalEntryEntity> = dao.byStatus(JournalStatus.APPLIED.name)

    /** Tweak ids that were applied inside a given session, oldest first — the revert order. */
    suspend fun appliedTweaks(sessionId: Long): List<String> =
        dao.tweaksByStatus(sessionId, JournalStatus.APPLIED.name)
}
