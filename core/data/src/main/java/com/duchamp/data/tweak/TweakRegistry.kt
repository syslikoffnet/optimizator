package com.duchamp.data.tweak

import com.duchamp.data.device.DeviceReader
import com.duchamp.data.profile.ProfileRepository
import com.duchamp.data.tweak.impl.AnimatorAnimationTweak
import com.duchamp.data.tweak.impl.BatteryBypassTweak
import com.duchamp.data.tweak.impl.BleScanTweak
import com.duchamp.data.tweak.impl.SwappinessTweak
import com.duchamp.data.tweak.impl.TouchFeedbackTweak
import com.duchamp.data.tweak.impl.TransitionAnimationTweak
import com.duchamp.data.tweak.impl.VibrationTweak
import com.duchamp.data.tweak.impl.WindowAnimationTweak
import com.duchamp.data.tweak.impl.WifiScanTweak
import com.duchamp.root.CommandRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Builds and holds the full list of tweaks. Exposed as `List<Tweak>` so feature modules can
 * inject the exact set the engine will run, in a stable order.
 */
@Module
@InstallIn(SingletonComponent::class)
object TweakRegistryModule {

    @Provides
    @Singleton
    fun provideTweaks(
        runner: CommandRunner,
        reader: DeviceReader,
        profile: ProfileRepository,
    ): List<Tweak> = TweakRegistry(runner, reader, profile).tweaks
}

@Singleton
class TweakRegistry(
    private val runner: CommandRunner,
    private val reader: DeviceReader,
    private val profile: ProfileRepository,
) {
    val tweaks: List<Tweak> = listOf(
        VibrationTweak(runner),
        TouchFeedbackTweak(runner),
        WifiScanTweak(runner),
        BleScanTweak(runner),
        WindowAnimationTweak(runner),
        TransitionAnimationTweak(runner),
        AnimatorAnimationTweak(runner),
        SwappinessTweak(runner, reader, profile),
        BatteryBypassTweak(runner),
    )

    fun byId(id: String): Tweak? = tweaks.firstOrNull { it.id == id }
}
