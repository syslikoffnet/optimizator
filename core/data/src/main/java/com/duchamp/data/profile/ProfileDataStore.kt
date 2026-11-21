package com.duchamp.data.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "duchamp_profile")

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {
    @Provides
    @Singleton
    fun provideProfileDataStore(@ApplicationContext context: Context): ProfileDataStore =
        ProfileDataStore(context)
}

@Singleton
class ProfileDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.profileDataStore

    val profile: Flow<GameProfile> = store.data.map { p ->
        GameProfile(
            onboarded = p[KEY_ONBOARDED] ?: false,
            musicPackage = p[KEY_MUSIC] ?: "",
            messengerPackage = p[KEY_MESSENGER] ?: "",
            useVpn = p[KEY_USE_VPN] ?: false,
            enableBypassOnCharge = p[KEY_BYPASS_CHARGE] ?: false,
            swappinessTarget = p[KEY_SWAPPINESS] ?: 60,
            gameActions = parseActions(p[KEY_GAME_ACTIONS] ?: ""),
            whitelist = parseWhitelist(p[KEY_WHITELIST] ?: ""),
        )
    }

    suspend fun setProfile(profile: GameProfile) {
        store.edit {
            it[KEY_ONBOARDED] = profile.onboarded
            it[KEY_MUSIC] = profile.musicPackage
            it[KEY_MESSENGER] = profile.messengerPackage
            it[KEY_USE_VPN] = profile.useVpn
            it[KEY_BYPASS_CHARGE] = profile.enableBypassOnCharge
            it[KEY_SWAPPINESS] = profile.swappinessTarget
            it[KEY_GAME_ACTIONS] = encodeActions(profile.gameActions)
            it[KEY_WHITELIST] = profile.whitelist.joinToString(",")
        }
    }

    suspend fun markOnboarded() {
        store.edit { it[KEY_ONBOARDED] = true }
    }

    companion object {
        private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
        private val KEY_MUSIC = stringPreferencesKey("music_package")
        private val KEY_MESSENGER = stringPreferencesKey("messenger_package")
        private val KEY_USE_VPN = booleanPreferencesKey("use_vpn")
        private val KEY_BYPASS_CHARGE = booleanPreferencesKey("bypass_on_charge")
        private val KEY_SWAPPINESS = intPreferencesKey("swappiness_target")
        private val KEY_GAME_ACTIONS = stringPreferencesKey("game_actions")
        private val KEY_WHITELIST = stringPreferencesKey("whitelist")

        private fun encodeActions(map: Map<String, GameAction>): String =
            map.entries.joinToString(",") { (pkg, action) -> "$pkg:${action.name}" }

        private fun parseActions(raw: String): Map<String, GameAction> =
            raw.split(",")
                .mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size < 2) return@mapNotNull null
                    val pkg = parts[0]
                    val action = parts[1]
                    pkg.takeIf { it.isNotBlank() }?.let {
                        runCatching { GameAction.valueOf(action) }.getOrNull()
                    }?.let { pkg to it }
                }
                .toMap()

        private fun parseWhitelist(raw: String): Set<String> =
            raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}
