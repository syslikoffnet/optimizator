package com.duchamp.onboarding
import androidx.lifecycle.ViewModel

import com.duchamp.data.profile.GameAction
import com.duchamp.data.profile.GameProfile
import com.duchamp.data.repo.ProfileRepository
import com.duchamp.device.DeviceReader
import com.duchamp.root.CommandRunner
import com.duchamp.root.RootAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** One-time setup: verify the device, pick audio/messenger apps, choose toggles, finish. */
@HiltViewModel
class OnboardingViewModel : ViewModel() @Inject constructor(
    private val runner: CommandRunner,
    private val reader: DeviceReader,
    private val profileRepository: ProfileRepository,
) {
    private val availability = RootAvailability(runner)

    private val _rootStatus = MutableStateFlow(RootAvailability.Status(false, false, ""))
    val rootStatus: StateFlow<RootAvailability.Status> = _rootStatus.asStateFlow()

    private val _installed = MutableStateFlow<List<String>>(emptyList())
    val installed: StateFlow<List<String>> = _installed.asStateFlow()

    private val _profile = MutableStateFlow(GameProfile.defaultProfile())
    val profile: StateFlow<GameProfile> = _profile.asStateFlow()

    private val _selectedAction = MutableStateFlow<Map<String, GameAction>>(emptyMap())
    val selectedAction: StateFlow<Map<String, GameAction>> = _selectedAction.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Checks root + device and loads the installed third-party package list. */
    suspend fun load() {
        _loading.value = true
        runCatching {
            _rootStatus.value = availability.status()
            _installed.value = reader.readPackages()
            _profile.value = profileRepository.current()
            _selectedAction.value = profileRepository.current().gameActions.toMap()
        }.onFailure { _rootStatus.value = RootAvailability.Status(false, false, it.message ?: "ошибка") }
        _loading.value = false
    }

    fun setAction(packageName: String, action: GameAction) {
        val next = _selectedAction.value.toMutableMap().apply { put(packageName, action) }
        _selectedAction.value = next
    }

    fun clearAction(packageName: String) {
        _selectedAction.value = _selectedAction.value.toMutableMap().apply { remove(packageName) }
    }

    fun setMusic(packageName: String) {
        _profile.value = _profile.value.copy(musicPackage = packageName)
    }

    fun setMessenger(packageName: String) {
        _profile.value = _profile.value.copy(messengerPackage = packageName)
    }

    fun setUseVpn(useVpn: Boolean) {
        _profile.value = _profile.value.copy(useVpn = useVpn)
    }

    fun setBypassOnCharge(enable: Boolean) {
        _profile.value = _profile.value.copy(enableBypassOnCharge = enable)
    }

    fun setSwappiness(value: Int) {
        _profile.value = _profile.value.copy(swappinessTarget = value)
    }

    /** Persists the profile, merges the chosen per-package actions and marks onboarding done. */
    suspend fun finish(): Boolean {
        val merged = _selectedAction.value
        val final = _profile.value.copy(
            gameActions = merged,
            whitelist = buildWhitelist(),
            onboarded = true,
        )
        profileRepository.setProfile(final)
        return true
    }

    private fun buildWhitelist(): Set<String> {
        val base = mutableSetOf<String>()
        _profile.value.musicPackage.takeIf { it.isNotBlank() }?.let { base.add(it) }
        _profile.value.messengerPackage.takeIf { it.isNotBlank() }?.let { base.add(it) }
        return base
    }
}
