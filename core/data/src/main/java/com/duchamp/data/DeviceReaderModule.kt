package com.duchamp.data

import com.duchamp.device.DeviceReader
import com.duchamp.root.CommandRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the device probe as a singleton. */
@Module
@InstallIn(SingletonComponent::class)
object DeviceReaderModule {
    @Provides
    @Singleton
    fun provideDeviceReader(runner: CommandRunner): DeviceReader = DeviceReader(runner)
}
