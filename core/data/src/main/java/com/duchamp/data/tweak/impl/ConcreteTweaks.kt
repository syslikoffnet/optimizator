package com.duchamp.data.tweak.impl

import com.duchamp.data.repo.ProfileRepository
import com.duchamp.data.tweak.Risk
import com.duchamp.data.tweak.Tweak
import com.duchamp.data.tweak.TweakGroup
import com.duchamp.data.tweak.TweakState
import com.duchamp.device.DeviceReader
import com.duchamp.device.DevicePaths
import com.duchamp.root.CommandRunner

/** Wi-Fi scanning stays enabled so the game keeps a low-latency connection. */
class WifiScanTweak(runner: CommandRunner) :
    GlobalSettingTweak(
        id = "wifi_scan_always",
        title = "Wi-Fi сканирование",
        group = TweakGroup.NETWORK,
        risk = Risk.SAFE,
        description = "Отключает «оптимизацию батареи» для Wi-Fi, чтобы ping не скакал.",
        runner = runner,
        key = "wifi_scan_always_enabled",
        desired = "1",
    )

/** Bluetooth scanning stays enabled (used by some game audio routing). */
class BleScanTweak(runner: CommandRunner) :
    GlobalSettingTweak(
        id = "ble_scan_always",
        title = "Bluetooth сканирование",
        group = TweakGroup.NETWORK,
        risk = Risk.SAFE,
        description = "Отключает «оптимизацию батареи» для Bluetooth.",
        runner = runner,
        key = "ble_scan_always_enabled",
        desired = "1",
    )

/** Window animation scale: 0.5 makes transitions instant and frees GPU time. */
class WindowAnimationTweak(runner: CommandRunner) :
    GlobalSettingTweak(
        id = "window_animation",
        title = "Анимация окон",
        group = TweakGroup.SYSTEM,
        risk = Risk.SAFE,
        description = "Устанавливает window_animation_scale = 0.5.",
        runner = runner,
        key = "window_animation_scale",
        desired = "0.5",
    )

/** Transition animation scale. */
class TransitionAnimationTweak(runner: CommandRunner) :
    GlobalSettingTweak(
        id = "transition_animation",
        title = "Переходная анимация",
        group = TweakGroup.SYSTEM,
        risk = Risk.SAFE,
        description = "Устанавливает transition_animation_scale = 0.5.",
        runner = runner,
        key = "transition_animation_scale",
        desired = "0.5",
    )

/** Animator animation scale. */
class AnimatorAnimationTweak(runner: CommandRunner) :
    GlobalSettingTweak(
        id = "animator_animation",
        title = "Аниматор",
        group = TweakGroup.SYSTEM,
        risk = Risk.SAFE,
        description = "Устанавливает animator_animation_scale = 0.5.",
        runner = runner,
        key = "animator_animation_scale",
        desired = "0.5",
    )

/** Disables system haptics during a game. */
class VibrationTweak(runner: CommandRunner) :
    GlobalSettingTweak(
        id = "vibration",
        title = "Виброотклик",
        group = TweakGroup.GAME_MODE,
        risk = Risk.SAFE,
        description = "Отключает системную вибрацию (вибрацию приложений не трогает).",
        runner = runner,
        key = "vibrate_when_calls",
        desired = "0",
    )

/** Disables touch feedback haptics. */
class TouchFeedbackTweak(runner: CommandRunner) :
    GlobalSettingTweak(
        id = "touch_feedback",
        title = "Тач-ответ",
        group = TweakGroup.GAME_MODE,
        risk = Risk.SAFE,
        description = "Отключает haptics-ответ на касание.",
        runner = runner,
        key = "haptic_feedback_enabled",
        desired = "0",
    )

/**
 * MTK charging bypass: forces the charger to not feed the battery, so the phone runs off the
 * wall and does not heat the cell. Reverted on every session end and every boot — never left on.
 */
class BatteryBypassTweak(
    private val runner: CommandRunner,
) : Tweak {
    override val id = "battery_bypass"
    override val title = "Отключение зарядки"
    override val group = TweakGroup.POWER
    override val risk = Risk.SYSTEM
    override val description =
        "Переключает /proc/mtk_battery_cmd/current_cmd в 0 0, чтобы не греться батарея. Сбрасывается после каждой игры."

    override suspend fun snapshot(): TweakState {
        val result = runner.run("cat ${DevicePaths.BATTERY_CMD}")
        if (!result.success) return TweakState.unavailable(detail = "${DevicePaths.BATTERY_CMD} не доступно")
        return TweakState.on(result.stdout.trim(), detail = "подача тока отключена")
    }

    override suspend fun apply(): Result<Unit> {
        val result = runner.run("echo '0 0' > ${DevicePaths.BATTERY_CMD}")
        return if (result.success) Result.success(Unit) else Result.failure(IllegalStateException(result.stderr))
    }

    override suspend fun verify(): Boolean =
        runner.run("cat ${DevicePaths.BATTERY_CMD}").stdout.trim().let { it == "0 0" }

    override suspend fun revert(from: TweakState): Result<Unit> {
        val result = runner.run("echo '0 0' > ${DevicePaths.BATTERY_CMD}")
        return if (result.success) Result.success(Unit) else Result.failure(IllegalStateException(result.stderr))
    }

    override fun applyCommands(): List<String> = listOf("echo '0 0' > ${DevicePaths.BATTERY_CMD}")
    override fun revertCommands(from: TweakState): List<String> = applyCommands()
}

/**
 * Sets vm.swappiness from the profile. Resettable: on revert it restores the value read at
 * snapshot time. The target is refreshed from the profile on every [apply] so a changed
 * profile is honoured, while [applyCommands] stays synchronous.
 */
class SwappinessTweak(
    private val runner: CommandRunner,
    private val reader: DeviceReader,
    private val profile: ProfileRepository,
) : Tweak {
    override val id = "swappiness"
    override val title = "Swappiness"
    override val group = TweakGroup.MEMORY
    override val risk = Risk.SYSTEM_OPTIMIZATION
    override val description = "Устанавливает vm.swappiness, чтобы финтовый мержолейт лучше работал."

    /** Latest target; refreshed from the profile on every [apply]/[verify]. */
    @Volatile
    private var target: Int = 60

    private suspend fun refreshTarget(): Int {
        val value = profile.current().swappinessTarget.also { target = it }
        return value
    }

    override suspend fun snapshot(): TweakState {
        val value = reader.readSwappiness() ?: return TweakState.unavailable(detail = "файл не доступен")
        return TweakState.on(value.toString())
    }

    override suspend fun apply(): Result<Unit> {
        val value = refreshTarget()
        val result = runner.run("echo '$value' > ${DevicePaths.SWAPPINESS}")
        return if (result.success) Result.success(Unit) else Result.failure(IllegalStateException(result.stderr))
    }

    override suspend fun verify(): Boolean {
        val value = refreshTarget()
        return reader.readSwappiness() == value
    }

    override suspend fun revert(from: TweakState): Result<Unit> {
        val restore = from.rawValue.toIntOrNull() ?: return Result.success(Unit)
        val result = runner.run("echo '$restore' > ${DevicePaths.SWAPPINESS}")
        return if (result.success) Result.success(Unit) else Result.failure(IllegalStateException(result.stderr))
    }

    override fun applyCommands(): List<String> = listOf("echo '$target' > ${DevicePaths.SWAPPINESS}")

    override fun revertCommands(from: TweakState): List<String> {
        val restore = from.rawValue.toIntOrNull() ?: return emptyList()
        return listOf("echo '$restore' > ${DevicePaths.SWAPPINESS}")
    }
}
