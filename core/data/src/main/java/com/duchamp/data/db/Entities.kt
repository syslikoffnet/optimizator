package com.duchamp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A completed (or abandoned) game-mode session with its summary metrics. */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long,
    val durationSec: Long,
    val avgFps: Double,
    val low1Fps: Double,
    val peakSocTempC: Double,
    /** Seconds from session start when the CPU ceiling first dropped (throttle), or -1. */
    val throttleStartSec: Long,
    val avgPingMs: Double,
    val batteryDeltaPercent: Double,
    val batteryBypass: Boolean,
    val summary: String,
)

/** One 1 Hz telemetry sample belonging to a session. */
@Entity(tableName = "metric_samples")
data class MetricSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestampMillis: Long,
    val cpu0FreqKb: Long,
    val cpu4FreqKb: Long,
    val cpu7FreqKb: Long,
    val cpuMax0Kb: Long,
    val cpuMax4Kb: Long,
    val cpuMax7Kb: Long,
    val gpuFreqKhz: Long,
    val gpuUtil: Int,
    val socTempC: Double,
    val bigTempC: Double,
    val gpuTempC: Double,
    val batteryTempC: Double,
    val thermalStatus: Int,
    val batteryCurrent: Int,
    val batteryPower: Double,
    val memAvailableKb: Long,
    val swapUsedKb: Long,
    val fps: Int,
)

/**
 * Write-ahead journal entry. Written *before* a command runs, marked APPLIED after. Survives
 * crashes/reboots so an interrupted session can be reverted on next start.
 */
@Entity(tableName = "journal")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Owning session; 0 for standalone (pre-session) tweaks. */
    val sessionId: Long,
    val tweakId: String,
    val title: String,
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val atMillis: Long,
    val status: String,
    val risk: String,
    /**
     * The real raw value of this tweak *before* it was applied in this session (e.g. the
     * `settings get global <key>` output captured just before `apply`). Revert uses it to restore
     * the pre-change value; without it a revert would write back the already-applied value and do
     * nothing. Empty for entries written before this field existed.
     */
    val previousRaw: String = "",
)

/** A marker drawn on a session chart (throttle start, charge disconnect, thermal step, ...). */
@Entity(tableName = "events")
data class EventMarkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestampMillis: Long,
    val type: String,
    val label: String,
)

/** Key/value snapshot used for the baseline ("everything looked like this before duchamp"). */
@Entity(tableName = "state")
data class StateEntryEntity(
    @PrimaryKey val key: String,
    val value: String,
    val atMillis: Long,
)
