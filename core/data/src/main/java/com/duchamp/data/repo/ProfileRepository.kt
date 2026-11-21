package com.duchamp.data.repo

import com.duchamp.data.profile.GameAction
import com.duchamp.data.profile.GameProfile
import com.duchamp.data.profile.ProfileDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProfileRepositoryModule {
    @Provides
    @Singleton
    fun provideProfileRepository(store: ProfileDataStore): ProfileRepository = ProfileRepository(store)
}

@Singleton
open class ProfileRepository @Inject constructor(
    private val store: ProfileDataStore,
) {

    val profile: Flow<GameProfile> = store.profile

    open suspend fun current(): GameProfile = profile.first()

    suspend fun setProfile(profile: GameProfile) = store.setProfile(profile)

    suspend fun update(transform: (GameProfile) -> GameProfile) =
        store.setProfile(transform(profile.first()))

    suspend fun markOnboarded() = store.markOnboarded()

    fun exportJson(profile: GameProfile): String {
        val o = JSONObject()
        o.put("onboarded", profile.onboarded)
        o.put("musicPackage", profile.musicPackage)
        o.put("messengerPackage", profile.messengerPackage)
        o.put("useVpn", profile.useVpn)
        o.put("enableBypassOnCharge", profile.enableBypassOnCharge)
        o.put("swappinessTarget", profile.swappinessTarget)
        o.put("version", profile.version)
        val tw = JSONArray()
        profile.enabledTweaks.forEach { tw.put(it) }
        o.put("enabledTweaks", tw)
        val actions = JSONObject()
        profile.gameActions.forEach { (pkg, action) -> actions.put(pkg, action.name) }
        o.put("gameActions", actions)
        val wl = JSONArray()
        profile.whitelist.forEach { wl.put(it) }
        o.put("whitelist", wl)
        return o.toString(2)
    }

    fun importJson(raw: String): GameProfile? = runCatching {
        val o = JSONObject(raw)
        val actions = mutableMapOf<String, GameAction>()
        val a = o.optJSONObject("gameActions")
        if (a != null) {
            a.keys().asSequence().forEach { pkg ->
                runCatching { GameAction.valueOf(a.getString(pkg)) }.getOrNull()
                    ?.let { actions[pkg] = it }
            }
        }
        val wl = mutableSetOf<String>()
        val arr = o.optJSONArray("whitelist")
        if (arr != null) {
            for (i in 0 until arr.length()) wl.add(arr.getString(i))
        }
        val tw = mutableSetOf<String>()
        val twArr = o.optJSONArray("enabledTweaks")
        if (twArr != null) {
            for (i in 0 until twArr.length()) tw.add(twArr.getString(i))
        }
        GameProfile(
            onboarded = o.optBoolean("onboarded", false),
            musicPackage = o.optString("musicPackage", ""),
            messengerPackage = o.optString("messengerPackage", ""),
            useVpn = o.optBoolean("useVpn", false),
            enableBypassOnCharge = o.optBoolean("enableBypassOnCharge", false),
            swappinessTarget = o.optInt("swappinessTarget", 60),
            version = o.optInt("version", 1),
            gameActions = actions,
            whitelist = wl,
            enabledTweaks = tw,
        )
    }.getOrNull()
}
