package com.duchamp.gamemode
import kotlin.jvm.JvmSuppressWildcards
import androidx.lifecycle.ViewModel

import com.duchamp.data.gamemode.GameModeEngine
import com.duchamp.data.gamemode.GameModeState
import com.duchamp.data.tweak.Tweak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Drives the one-button game-mode flow. Delegates the actual work to the injected
 * [GameModeEngine], which owns the state machine and the journal.
 */
@HiltViewModel
class GameModeViewModel @Inject constructor(
    private val engine: GameModeEngine,
    private val tweaks: @JvmSuppressWildcards List<Tweak>,
) : ViewModel() {
    val state: StateFlow<GameModeState> = engine.state

    suspend fun start(): Boolean = engine.start()

    suspend fun finish(reason: String): Boolean = engine.finish(reason)

    val tweaksSnapshot: List<Tweak> get() = tweaks
}
