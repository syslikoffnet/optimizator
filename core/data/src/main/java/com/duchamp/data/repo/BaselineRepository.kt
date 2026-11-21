package com.duchamp.data.repo

import com.duchamp.device.DevicePaths
import com.duchamp.device.DeviceReader
import com.duchamp.data.db.StateDao
import com.duchamp.root.CommandRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Snapshot of every value duchamp can change, taken before the first change. */
data class BaselineSnapshot(
    val capturedAtMillis: Long,
    val swappiness: Int?,
    val wifiScanAlways: String?,
    val bleScanAlways: String?,
    val windowAnimation: String?,
    val transitionAnimation: String?,
    val animatorAnimation: String?,
    val batteryCmd: String?,
    val disabledPackages: List<String>,
)

@Module
@InstallIn(SingletonComponent::class)
object BaselineRepositoryModule {
    @Provides
    @Singleton
    fun provideBaselineRepository(
        dao: StateDao,
        runner: CommandRunner,
        reader: DeviceReader,
    ): BaselineRepository = BaselineRepository(dao, runner, reader)
}

@Singleton
class BaselineRepository @Inject constructor(
    private val dao: StateDao,
    private val runner: CommandRunner,
    private val reader: DeviceReader,
) {

    private val key = "baseline"

    suspend fun capture(): BaselineSnapshot {
        val snapshot = BaselineSnapshot(
            capturedAtMillis = System.currentTimeMillis(),
            swappiness = reader.readSwappiness(),
            wifiScanAlways = settingsGet(DevicePaths.WIFI_SCAN_ALWAYS),
            bleScanAlways = settingsGet(DevicePaths.BLE_SCAN_ALWAYS),
            windowAnimation = settingsGet(DevicePaths.WINDOW_ANIMATION_SCALE),
            transitionAnimation = settingsGet(DevicePaths.TRANSITION_ANIMATION_SCALE),
            animatorAnimation = settingsGet(DevicePaths.ANIMTOR_ANIMATION_SCALE),
            batteryCmd = cat(DevicePaths.BATTERY_CMD),
            disabledPackages = reader.readDisabledPackages(),
        )
        save(snapshot)
        return snapshot
    }

    suspend fun save(snapshot: BaselineSnapshot) {
        dao.put(
            com.duchamp.data.db.StateEntryEntity(
                key = key,
                value = toJson(snapshot),
                atMillis = snapshot.capturedAtMillis,
            ),
        )
        // Copy survives app reinstall; written via a plain shell redirect (not cmd/pm/settings).
        val json = toJson(snapshot)
        runner.run("mkdir -p /data/adb/duchamp && printf '%s' '${json.replace("'", "'\\''")}' > ${DevicePaths.BASELINE_JSON}")
    }

    suspend fun load(): BaselineSnapshot? {
        val entry = dao.get(key) ?: return null
        return fromJson(entry.value)
    }

    /** Restore the scalar values duchamp is allowed to change back to their baseline. */
    suspend fun restoreScalars(): RestoreResult {
        val snapshot = load() ?: return RestoreResult(restored = 0, errors = listOf("базовый снимок отсутствует"))
        var restored = 0
        val errors = mutableListOf<String>()

        snapshot.swappiness?.let { target ->
            if (runner.run("echo $target > ${DevicePaths.SWAPPINESS}").success) restored++
            else errors.add("swappiness")
        }
        listOf(
            DevicePaths.WIFI_SCAN_ALWAYS to snapshot.wifiScanAlways,
            DevicePaths.BLE_SCAN_ALWAYS to snapshot.bleScanAlways,
            DevicePaths.WINDOW_ANIMATION_SCALE to snapshot.windowAnimation,
            DevicePaths.TRANSITION_ANIMATION_SCALE to snapshot.transitionAnimation,
            DevicePaths.ANIMTOR_ANIMATION_SCALE to snapshot.animatorAnimation,
        ).forEach { (setting, value) ->
            value?.let { v ->
                if (runner.run("settings put global $setting $v").success) restored++
                else errors.add(setting)
            }
        }
        // Battery bypass must never be left on outside a session.
        if (runner.run("echo ${DevicePaths.BATTERY_CMD_DISABLE} > ${DevicePaths.BATTERY_CMD}").success) restored++
        return RestoreResult(restored = restored, errors = errors)
    }

    private suspend fun settingsGet(name: String): String? =
        runner.run("settings get global $name").takeIf { it.success }?.stdout?.trim()

    private suspend fun cat(path: String): String? =
        runner.run("cat '$path'").takeIf { it.success }?.stdout?.trim()

    private fun toJson(s: BaselineSnapshot): String = JSONObject().apply {
        put("capturedAtMillis", s.capturedAtMillis)
        put("swappiness", s.swappiness)
        put("wifiScanAlways", s.wifiScanAlways)
        put("bleScanAlways", s.bleScanAlways)
        put("windowAnimation", s.windowAnimation)
        put("transitionAnimation", s.transitionAnimation)
        put("animatorAnimation", s.animatorAnimation)
        put("batteryCmd", s.batteryCmd)
        put("disabledPackages", s.disabledPackages)
    }.toString()

    private fun fromJson(raw: String): BaselineSnapshot? = runCatching {
        val o = JSONObject(raw)
        BaselineSnapshot(
            capturedAtMillis = o.optLong("capturedAtMillis"),
            swappiness = o.opt("swappiness") as? Int,
            wifiScanAlways = o.opt("wifiScanAlways") as? String,
            bleScanAlways = o.opt("bleScanAlways") as? String,
            windowAnimation = o.opt("windowAnimation") as? String,
            transitionAnimation = o.opt("transitionAnimation") as? String,
            animatorAnimation = o.opt("animatorAnimation") as? String,
            batteryCmd = o.opt("batteryCmd") as? String,
            disabledPackages = o.optJSONArray("disabledPackages")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
        )
    }.getOrNull()
}

data class RestoreResult(val restored: Int, val errors: List<String>)
