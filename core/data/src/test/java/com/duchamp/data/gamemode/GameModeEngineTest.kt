package com.duchamp.data.gamemode

import com.duchamp.data.FakeJournalDao
import com.duchamp.data.FakeShell
import com.duchamp.data.profile.GameAction
import com.duchamp.data.profile.GameProfile
import com.duchamp.data.repo.JournalRepository
import com.duchamp.data.repo.SessionStore
import com.duchamp.data.tweak.Risk
import com.duchamp.data.tweak.Tweak
import com.duchamp.data.tweak.TweakGroup
import com.duchamp.data.tweak.TweakState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Game-mode state machine + journal/revert, driven by a [FakeShell]. No device, no root: the
 * engine's exact command sequence and journal transitions are what's being verified.
 */
class GameModeEngineTest {

    /** A tweak that runs a real command through the shell on apply and its inverse on revert. */
    private class ToggleTweak(
        override val id: String,
        private val shell: FakeShell,
        private val applyCmd: String,
        private val revertCmd: String,
    ) : Tweak {
        override val title: String = id
        override val group: TweakGroup = TweakGroup.SYSTEM
        override val risk: Risk = Risk.SYSTEM
        override val description: String = "test tweak $id"

        override suspend fun snapshot(): TweakState = TweakState.off("off")

        override suspend fun apply(): Result<Unit> {
            shell.run(applyCmd)
            return Result.success(Unit)
        }

        override suspend fun verify(): Boolean = true

        override suspend fun revert(from: TweakState): Result<Unit> {
            shell.run(revertCmd)
            return Result.success(Unit)
        }

        override fun applyCommands(): List<String> = listOf(applyCmd)
        override fun revertCommands(from: TweakState): List<String> = listOf(revertCmd)
    }

    private fun profile(): GameProfile = GameProfile(
        onboarded = true,
        gameActions = mapOf("com.example.game" to GameAction.FREEZE),
        enabledTweaks = emptySet(),
    )

    /** Fake session store that records what the engine asked it to do. */
    private class FakeSessionStore : SessionStore {
        var lastSummary: String = ""
        var finished: Boolean = false
        override suspend fun startSession(startMillis: Long): Long = 1L
        override suspend fun addEvent(sessionId: Long, timestampMillis: Long, type: String, label: String) {}
        override suspend fun updateSummary(sessionId: Long, summary: String) { lastSummary = summary }
        override suspend fun finish(sessionId: Long, endMillis: Long, summary: String) { finished = true }
    }

    private fun engine(
        shell: FakeShell,
        tweaks: List<Tweak>,
    ): Triple<GameModeEngine, FakeSessionStore, JournalRepository> {
        val journal = JournalRepository(FakeJournalDao())
        val session = FakeSessionStore()
        val engine = GameModeEngine(
            root = shell,
            profileProvider = { profile() },
            sessionStore = session,
            journal = journal,
            tweaks = tweaks,
            clock = { 1000L },
        )
        return Triple(engine, session, journal)
    }

    @Test
    fun `start applies tweaks freezes packages and reaches ACTIVE`() = runTest {
        val shell = FakeShell()
        shell.succeed("id -u", "0")
        shell.succeed("pm freeze com.example.game")
        val tweak = ToggleTweak(
            "wifi_scan_always",
            shell,
            "settings put global wifi_scan_always_enabled 1",
            "settings put global wifi_scan_always_enabled 0",
        )
        val (engine, _, journal) = engine(shell, listOf(tweak))

        assertTrue(engine.start())

        val state = engine.state.value
        assertEquals(GameModePhase.ACTIVE, state.phase)
        assertEquals(listOf("wifi_scan_always"), state.result!!.applied)
        assertEquals(listOf("com.example.game"), state.result!!.frozen)
        assertTrue(shell.ran { it.startsWith("pm freeze") })

        // Journal recorded the tweak as APPLIED.
        assertEquals(listOf("wifi_scan_always"), journal.appliedTweaks(1L))
    }

    @Test
    fun `finish reverts applied tweaks and switches battery bypass off`() = runTest {
        val shell = FakeShell()
        shell.succeed("id -u", "0")
        shell.succeed("pm freeze com.example.game")
        val tweak = ToggleTweak(
            "wifi_scan_always",
            shell,
            "settings put global wifi_scan_always_enabled 1",
            "settings put global wifi_scan_always_enabled 0",
        )
        val (engine, session, _) = engine(shell, listOf(tweak))

        assertTrue(engine.start())
        assertTrue(engine.finish("test"))

        // Revert command ran, and the battery bypass was switched off.
        assertTrue(shell.ran { it == "settings put global wifi_scan_always_enabled 0" })
        assertTrue(shell.ran { it.contains("mtk_battery_cmd") })
        assertEquals(GameModePhase.DONE, engine.state.value.phase)
        assertTrue(session.finished)
    }

    @Test
    fun `start fails without root and does not apply anything`() = runTest {
        val shell = FakeShell(failUnmatched = true)
        val tweak = ToggleTweak(
            "wifi_scan_always",
            shell,
            "settings put global wifi_scan_always_enabled 1",
            "settings put global wifi_scan_always_enabled 0",
        )
        val (engine, _, _) = engine(shell, listOf(tweak))

        assertFalse(engine.start())
        assertEquals(GameModePhase.FAILED, engine.state.value.phase)
        assertFalse(shell.ran { it.startsWith("settings put") })
    }

    @Test
    fun `a failing tweak is journaled as failed`() = runTest {
        val shell = FakeShell()
        shell.succeed("id -u", "0")
        shell.succeed("pm freeze com.example.game")
        shell.fail("settings put global wifi_scan_always_enabled 1")
        val tweak = ToggleTweak(
            "wifi_scan_always",
            shell,
            "settings put global wifi_scan_always_enabled 1",
            "settings put global wifi_scan_always_enabled 0",
        )
        val (engine, _, journal) = engine(shell, listOf(tweak))

        assertTrue(engine.start())
        val state = engine.state.value
        assertEquals(GameModePhase.ACTIVE, state.phase)
        assertEquals(listOf("wifi_scan_always"), state.result!!.failed)
        // The failed command was journaled.
        val failed = journal.forSession(1L).filter { it.status == "FAILED" }
        assertEquals(1, failed.size)
    }
}
