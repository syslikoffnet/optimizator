package com.duchamp.system
import androidx.lifecycle.ViewModel

import com.duchamp.data.tweak.Tweak
import com.duchamp.data.tweak.TweakState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** One row in the functions list: the tweak plus its last real snapshot. */
data class TweakRow(
    val tweak: Tweak,
    val state: TweakState,
)

@HiltViewModel
class SystemViewModel : ViewModel() @Inject constructor(
    private val tweaks: List<Tweak>,
) {
    private val _rows = MutableStateFlow<List<TweakRow>>(emptyList())
    val rows: StateFlow<List<TweakRow>> = _rows.asStateFlow()

    private val _busy = MutableStateFlow("")
    val busy: StateFlow<String> = _busy.asStateFlow()

    /** Reads the real current state of every tweak. */
    suspend fun refresh() {
        _rows.value = tweaks.map { tweak ->
            TweakRow(tweak, tweak.snapshot())
        }
    }

    /** Applies one tweak and re-reads its state. */
    suspend fun apply(tweak: Tweak) {
        _busy.value = tweak.title
        runCatching {
            tweak.apply()
            refresh()
        }
        _busy.value = ""
    }

    /** Reverts one tweak to its last snapshot. */
    suspend fun revert(tweak: Tweak, from: TweakState) {
        _busy.value = tweak.title
        runCatching {
            tweak.revert(from)
            refresh()
        }
        _busy.value = ""
    }
}
