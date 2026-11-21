package com.duchamp.data.gamemode

import com.duchamp.data.repo.JournalRepository
import com.duchamp.data.repo.SessionStore
import com.duchamp.data.tweak.Risk
import com.duchamp.data.tweak.Tweak
import com.duchamp.root.CommandResult
import com.duchamp.root.CommandRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Stages of the single "Start game mode" action, in order. */
enum class GameModePhase(val label: String) {
    IDLE("—"),
    CHECKING_ROOT("Проверка root"),
    STARTING("Запуск"),
    APPLYING("Применение настроек"),
    FREEZING("Заморозка приложений"),
    FINALIZING("Финализация"),
    ACTIVE("Активен"),
    FINISHING("Завершение"),
    DONE("Готово"),
    FAILED("Ошибка"),
}

/** Progress 0.0..1.0 of the whole start sequence. */
data class GameModeState(
    val phase: GameModePhase = GameModePhase.IDLE,
    val progress: Float = 0f,
    val message: String = "",
    val result: StartResult? = null,
    val error: String? = null,
) {
    val isBusy: Boolean get() = phase == GameModePhase.CHECKING_ROOT ||
        phase == GameModePhase.STARTING ||
        phase == GameModePhase.APPLYING ||
        phase == GameModePhase.FREEZING ||
        phase == GameModePhase.FINALIZING ||
        phase == GameModePhase.FINISHING

    val isActive: Boolean get() = phase == GameModePhase.ACTIVE
}

/** Outcome of a full game-mode start sequence. */
data class StartResult(
    val sessionId: Long,
    val profileVersion: Int,
    val applied: List<String> = emptyList(),
    val skipped: List<String> = emptyList(),
    val failed: List<String> = emptyList(),
    val frozen: List<String> = emptyList(),
    val killRequested: List<String> = emptyList(),
    val commandFailures: List<String> = emptyList(),
    val durationMs: Long = 0L,
) {
    val summary: String
        get() = "${applied.size} настроек · ${frozen.size} заморожено · ${skipped.size} пропущено"
}

/** Result of a single tweak application. */
internal sealed interface TweakOutcome {
    object Applied : TweakOutcome
    object Skipped : TweakOutcome
    data class Error(val message: String) : TweakOutcome
}

/**
 * The one-button game-mode state machine.
 *
 * Everything it needs (root access, tweaks, profile, session/journal storage) is injected, so the
 * exact sequence can be unit-tested with fakes. It performs, in order, and never throws:
 *
 *  1. verify root and load the chosen profile;
 *  2. open a session, and write each tweak into the journal *before* touching the system;
 *  3. apply each enabled tweak (system settings first, then the riskier process controls);
 *  4. freeze the marked packages;
 *  5. write the session summary and transition to ACTIVE.
 *
 * On any step failing, it reverts everything applied in this run and reports the reason.
 */
class GameModeEngine(
    private val root: CommandRunner,
    private val profileProvider: suspend () -> GameProfile,
    private val sessionStore: SessionStore,
    private val journal: JournalRepository,
    private val tweaks: List<Tweak>,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow(GameModeState())
    val state: StateFlow<GameModeState> = _state.asStateFlow()

    /** Starts the sequence. Returns true when the phone ended in game mode. */
    suspend fun start(): Boolean {
        val startedAt = clock()
        val profile = profileProvider()

        setPhase(GameModePhase.CHECKING_ROOT, "Проверка root", 0.02f)
        if (!root.isRootAvailable() || root.checkRoot().exitCode != 0) {
            return fail("Root не доступен. Запустите приложение через приложение KernelSU или установите root.")
        }

        setPhase(GameModePhase.STARTING, "Запуск «Игровой режим»", 0.08f)
        val sessionId = sessionStore.startSession(startedAt)
        sessionStore.addEvent(sessionId, startedAt, "start", "Сеанс #$sessionId")

        val applied = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val commandFailures = mutableListOf<String>()

        // Order: system settings first, then the riskier process controls.
        val ordered = tweaks.sortedWith(compareBy { riskOrdinal(it.risk) })
        val total = ordered.size
        for ((index, tweak) in ordered.withIndex()) {
            val fraction = 0.08f + (0.55f * index / maxOf(1, total))
            setPhase(GameModePhase.APPLYING, "«${tweak.title}»", fraction)
            if (!profile.tweakEnabled(tweak.id)) {
                skipped += tweak.id
                continue
            }
            when (val outcome = applyTweak(sessionId, tweak, commandFailures)) {
                is TweakOutcome.Applied -> applied += tweak.id
                is TweakOutcome.Skipped -> skipped += tweak.id
                is TweakOutcome.Error -> failed += tweak.id
            }
        }

        setPhase(GameModePhase.FREEZING, "Заморозка ${profile.freezePackages.size} приложений", 0.68f)
        val frozen = mutableListOf<String>()
        for (pkg in profile.freezePackages) {
            if (runPackageCommand("pm freeze $pkg")) frozen += pkg else commandFailures += "freeze:$pkg"
        }

        setPhase(GameModePhase.FINALIZING, "Финализация", 0.95f)
        val result = StartResult(
            sessionId = sessionId,
            profileVersion = profile.version,
            applied = applied,
            skipped = skipped,
            failed = failed,
            frozen = frozen,
            killRequested = profile.killPackages.toList(),
            commandFailures = commandFailures,
            durationMs = clock() - startedAt,
        )
        sessionStore.updateSummary(sessionId, result.summary)
        sessionStore.addEvent(sessionId, clock(), "done", result.summary)

        _state.value = GameModeState(
            phase = GameModePhase.ACTIVE,
            progress = 1f,
            message = "Игровой режим активен",
            result = result,
        )
        return true
    }

    /** Reverts everything applied in the active session. */
    suspend fun finish(reason: String = "Завершение пользователем"): Boolean {
        if (!_state.value.isActive) return false
        val sessionId = _state.value.result?.sessionId ?: return false
        setPhase(GameModePhase.FINISHING, "Завершение", 0.9f)

        // 1. Battery bypass off first — it must not survive outside game mode.
        runPackageCommand("echo '0 0' > /proc/mtk_battery_cmd/current_cmd")

        // 2. Revert tweaks, newest-applied-first.
        val applied = journal.appliedTweaks(sessionId)
        for ((index, id) in applied.withIndex()) {
            val tweak = tweaks.firstOrNull { it.id == id } ?: continue
            setPhase(
                GameModePhase.FINISHING,
                "Сброс «${tweak.title}»",
                0.9f + 0.08f * index / maxOf(1, applied.size),
            )
            val entryId = journal.forSession(sessionId).firstOrNull { it.tweakId == id }?.id ?: 0
            val result = runCatching {
                val previous = tweak.snapshot()
                tweak.revert(previous)
            }
            if (result.isSuccess) {
                journal.markReverted(entryId)
            } else {
                journal.markFailed(entryId, CommandResult.failure(result.exceptionOrNull()?.message ?: "сброс не выполнен"))
            }
        }

        sessionStore.addEvent(sessionId, clock(), "finish", reason)
        sessionStore.finish(sessionId, clock(), "Завершено: $reason")
        _state.value = GameModeState(
            phase = GameModePhase.DONE,
            progress = 1f,
            message = "Игровой режим завершён",
            result = _state.value.result?.copy(),
        )
        return true
    }

    /** Hard failure: revert everything applied in this session, then report the reason. */
    private suspend fun fail(message: String): Boolean {
        val sessionId = _state.value.result?.sessionId
        if (sessionId != null) {
            for (id in journal.appliedTweaks(sessionId)) {
                val tweak = tweaks.firstOrNull { it.id == id } ?: continue
                runCatching { tweak.revert(tweak.snapshot()) }
            }
            sessionStore.addEvent(sessionId, clock(), "fail", message)
            sessionStore.finish(sessionId, clock(), "Сбой: $message")
        }
        _state.value = GameModeState(
            phase = GameModePhase.FAILED,
            progress = _state.value.progress,
            message = message,
            error = message,
            result = _state.value.result?.copy(failed = _state.value.result.failed + message),
        )
        return false
    }

    private suspend fun applyTweak(
        sessionId: Long,
        tweak: Tweak,
        commandFailures: MutableList<String>,
    ): TweakOutcome {
        val commands = tweak.applyCommands()
        // Record the exact commands first so the journal survives a crash mid-run.
        val entryId = journal.recordPending(sessionId, tweak, commands.joinToString("\n"), clock())
        for (command in commands) {
            if (!runPackageCommand(command, commandFailures)) {
                journal.markFailed(entryId, CommandResult.failure("${tweak.id}: команда не выполнена"))
                return TweakOutcome.Error("${tweak.id}: команда не выполнена")
            }
        }
        return try {
            tweak.snapshot()
            tweak.apply()
            if (tweak.verify()) {
                journal.markApplied(entryId, CommandResult.success(tweak.id))
                TweakOutcome.Applied
            } else {
                journal.markFailed(entryId, CommandResult.failure("${tweak.id}: не прошла проверка"))
                TweakOutcome.Error("${tweak.id}: не прошла проверка")
            }
        } catch (throwable: Throwable) {
            journal.markFailed(entryId, CommandResult.failure("${tweak.id}: ${throwable.message}"))
            TweakOutcome.Error("${tweak.id}: ${throwable.message}")
        }
    }

    private suspend fun runPackageCommand(command: String, failures: MutableList<String>? = null): Boolean {
        val result: CommandResult = root.run(command)
        if (result.exitCode != 0) {
            failures?.add(command)
        }
        return result.exitCode == 0
    }

    private fun setPhase(phase: GameModePhase, message: String, progress: Float) {
        _state.value = _state.value.copy(phase = phase, message = message, progress = progress)
    }

    private fun riskOrdinal(risk: Risk): Int = when (risk) {
        Risk.SAFE -> 0
        Risk.SYSTEM -> 1
        Risk.SYSTEM_OPTIMIZATION -> 2
        Risk.PROCESS -> 3
        Risk.SENSITIVE -> 4
    }
}
