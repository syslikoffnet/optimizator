package com.duchamp.gamemode

import com.duchamp.data.gamemode.GameModeEngine
import com.duchamp.data.repo.JournalRepository
import com.duchamp.data.repo.ProfileRepository
import com.duchamp.data.repo.SessionStore
import com.duchamp.data.tweak.Tweak
import com.duchamp.root.CommandRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the game-mode engine as a singleton so the state survives config changes. */
@Module
@InstallIn(SingletonComponent::class)
object GameModeModule {
    @Provides
    @Singleton
    fun provideGameModeEngine(
        runner: CommandRunner,
        profileRepository: ProfileRepository,
        sessionStore: SessionStore,
        journalRepository: JournalRepository,
        tweaks: List<Tweak>,
    ): GameModeEngine = GameModeEngine(
        root = runner,
        profileProvider = { profileRepository.current() },
        sessionStore = sessionStore,
        journal = journalRepository,
        tweaks = tweaks,
    )
}
