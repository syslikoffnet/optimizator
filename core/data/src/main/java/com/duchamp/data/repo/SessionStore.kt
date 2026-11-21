package com.duchamp.data.repo

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The session-lifecycle operations the game-mode engine needs, abstracted so the engine can be
 * tested with a fake. [SessionRepository] is the production implementation.
 */
interface SessionStore {
    suspend fun startSession(startMillis: Long): Long
    suspend fun addEvent(sessionId: Long, timestampMillis: Long, type: String, label: String)
    suspend fun updateSummary(sessionId: Long, summary: String)
    suspend fun finish(sessionId: Long, endMillis: Long, summary: String)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionStoreModule {
    @Binds
    abstract fun bindSessionStore(impl: SessionRepository): SessionStore
}
