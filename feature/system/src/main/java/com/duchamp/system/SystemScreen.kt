package com.duchamp.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duchamp.data.tweak.Tweak
import com.duchamp.design.DataCard
import com.duchamp.design.DuchampColor
import com.duchamp.design.MonoText
import com.duchamp.design.SectionLabel
import com.duchamp.design.StatusChip
import com.duchamp.design.StatusKind
import kotlinx.coroutines.launch

/** The functions screen: every tweak, its real current state, and a per-tweak apply/revert. */
@Composable
fun SystemScreen(
    modifier: Modifier = Modifier,
    viewModel: SystemViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Surface(modifier = modifier.fillMaxSize(), color = DuchampColor.Background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            SectionLabel("ФУНКЦИИ")
            Spacer(Modifier.height(16.dp))
            Text("Настройки системы", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            if (busy.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("«$busy»", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(8.dp))
            }

            if (rows.isEmpty()) {
                Text("Загрузка…", color = DuchampColor.TextTertiary)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rows, key = { it.tweak.id }) { row ->
                        TweakCard(
                            tweak = row.tweak,
                            state = row.state,
                            busy = busy == row.tweak.title,
                            onApply = { scope.launch { viewModel.apply(row.tweak) } },
                            onRevert = { scope.launch { viewModel.revert(row.tweak, row.state) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TweakCard(
    tweak: Tweak,
    state: com.duchamp.data.tweak.TweakState,
    busy: Boolean,
    onApply: () -> Unit,
    onRevert: () -> Unit,
) {
    DataCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tweak.title, style = MaterialTheme.typography.titleSmall)
                StatusChip(kind = statusKind(state))
            }
            Spacer(Modifier.height(6.dp))
            Text(tweak.description, style = MaterialTheme.typography.bodySmall, color = DuchampColor.TextSecondary)
            Spacer(Modifier.height(6.dp))
            MonoText(state.rawValue.ifBlank { "—" })
            if (state.detail.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(state.detail, style = MaterialTheme.typography.labelSmall, color = DuchampColor.TextTertiary)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRevert, enabled = !busy) { Text("Сброс") }
                Button(onClick = onApply, enabled = !busy) { Text("Применить") }
            }
        }
    }
}

private fun statusKind(state: com.duchamp.data.tweak.TweakState): StatusKind = when (state.status) {
    com.duchamp.data.tweak.TweakStatus.ON -> StatusKind.ON
    com.duchamp.data.tweak.TweakStatus.OFF -> StatusKind.OFF
    com.duchamp.data.tweak.TweakStatus.DIVERGED -> StatusKind.DIVERGED
    com.duchamp.data.tweak.TweakStatus.UNAVAILABLE -> StatusKind.UNAVAILABLE
    com.duchamp.data.tweak.TweakStatus.ERROR -> StatusKind.ERROR
}
