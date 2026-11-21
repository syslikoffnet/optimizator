package com.duchamp.gamemode

import androidx.lifecycle.ViewModel

import com.duchamp.data.profile.GameAction
import com.duchamp.data.repo.ProfileRepository
import com.duchamp.device.DeviceReader
import com.duchamp.root.CommandRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** One row in the task manager: a running process and what the profile currently does with it. */
data class TaskRow(
    val name: String,
    val rssMb: Double,
    val swapMb: Double,
    val weightMb: Double,
    val action: GameAction,
)

/**
 * The task manager: what is actually running right now, how much RAM/swap each process eats, and
 * what the game list does with it. Freeze / close / add-to-list all go through real `pm` / `am`
 * commands and are recorded in the profile so game mode honours them.
 */
@HiltViewModel
class TaskManagerViewModel @Inject constructor(
    private val reader: DeviceReader,
    private val profile: ProfileRepository,
    private val runner: CommandRunner,
) : ViewModel() {

    private val _rows = MutableStateFlow<List<TaskRow>>(emptyList())
    val rows: StateFlow<List<TaskRow>> = _rows.asStateFlow()

    /** Packages in the game list with their action, including ones not running right now. */
    private val _gameList = MutableStateFlow<Map<String, GameAction>>(emptyMap())
    val gameList: StateFlow<Map<String, GameAction>> = _gameList.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _busy = MutableStateFlow("")
    val busy: StateFlow<String> = _busy.asStateFlow()

    private var allRows: List<TaskRow> = emptyList()

    /** Reads the live process list and the current game-list actions. */
    suspend fun refresh() {
        runCatching {
            val procs = reader.readProcesses()
            val actions = profile.current().gameActions
            allRows = procs.map {
                TaskRow(
                    name = it.name,
                    rssMb = it.rssMb,
                    swapMb = it.swapMb,
                    weightMb = it.weightMb,
                    action = actions[it.name] ?: GameAction.IGNORE,
                )
            }.sortedByDescending { it.weightMb }
            _gameList.value = actions
            _rows.value = filter()
        }
    }

    private fun filter(): List<TaskRow> {
        val q = _query.value.trim()
        if (q.isEmpty()) return allRows
        return allRows.filter { it.name.contains(q, ignoreCase = true) }
    }

    fun setQuery(q: String) {
        _query.value = q
        _rows.value = filter()
    }

    /** Adds a package to the game list with the given action (no immediate command). */
    suspend fun setAction(pkg: String, action: GameAction) {
        _busy.value = pkg
        runCatching { profile.update { it.copy(gameActions = it.gameActions + (pkg to action)) } }
        _busy.value = ""
        refresh()
    }

    /** Freezes a package now (`pm freeze`) and records FREEZE in the game list. */
    suspend fun freeze(pkg: String) {
        _busy.value = pkg
        runCatching { runner.run("pm freeze $pkg") }
        runCatching { profile.update { it.copy(gameActions = it.gameActions + (pkg to GameAction.FREEZE)) } }
        _busy.value = ""
        refresh()
    }

    /** Closes a package now (`am force-stop`) and records CLOSE in the game list. */
    suspend fun close(pkg: String) {
        _busy.value = pkg
        runCatching { runner.run("am force-stop $pkg") }
        runCatching { profile.update { it.copy(gameActions = it.gameActions + (pkg to GameAction.CLOSE)) } }
        _busy.value = ""
        refresh()
    }

    /** Unfreezes a package (`pm unfreeze`) and removes it from the game list. */
    suspend fun unfreeze(pkg: String) {
        _busy.value = pkg
        runCatching { runner.run("pm unfreeze $pkg") }
        runCatching { profile.update { it.copy(gameActions = it.gameActions - pkg) } }
        _busy.value = ""
        refresh()
    }
}
