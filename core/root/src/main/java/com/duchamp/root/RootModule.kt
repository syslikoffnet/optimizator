package com.duchamp.root

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the real libsu-backed [CommandRunner] as a singleton. */
@Module
@InstallIn(SingletonComponent::class)
object RootModule {
    @Provides
    @Singleton
    fun provideCommandRunner(): CommandRunner = LibsuCommandRunner()
}
