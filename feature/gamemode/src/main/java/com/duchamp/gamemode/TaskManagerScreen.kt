package com.duchamp.gamemode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duchamp.data.profile.GameAction
import com.duchamp.design.DataCard
import com.duchamp.design.DuchampColor
import com.duchamp.design.MonoText
import com.duchamp.design.SectionLabel
import com.duchamp.design.StatusChip
import com.duchamp.design.StatusKind
import kotlinx.coroutines.launch

/** Task manager: live processes with their weight, plus the game list. */
@Composable
fun TaskManagerScreen(
    modifier: Modifier = Modifier,
    viewModel: TaskManagerViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val gameList by viewModel.gameList.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Surface(modifier = modifier.fillMaxSize(), color = DuchampColor.Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            SectionLabel("ДИСПЕТЧЕР ЗАДАЧ")
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Запущенные", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { scope.launch { viewModel.refresh() } }) { Text("Обновить") }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                label = { Text("Поиск по процессу") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            if (rows.isEmpty()) {
                Text("Нет данных о процессах", color = DuchampColor.TextTertiary)
            }
            rows.forEach { row ->
                TaskRowCard(
                    row = row,
                    busy = busy == row.name,
                    onFreeze = { scope.launch { viewModel.freeze(row.name) } },
                    onClose = { scope.launch { viewModel.close(row.name) } },
                    onUnfreeze = { scope.launch { viewModel.unfreeze(row.name) } },
                )
                Spacer(Modifier.height(12.dp))
            }

            SectionLabel("ИГРОВОЙ СПИСОК")
            Spacer(Modifier.height(12.dp))
            if (gameList.isEmpty()) {
                Text("Список пуст — добавьте процессы выше", color = DuchampColor.TextTertiary)
            }
            gameList.forEach { (pkg, action) ->
                DataCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(pkg, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            MonoText(actionLabel(action))
                        }
                        StatusChip(kind = actionKind(action))
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { scope.launch { viewModel.setAction(pkg, GameAction.IGNORE) } },
                            enabled = busy != pkg,
                        ) { Text("Убрать") }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TaskRowCard(
    row: TaskRow,
    busy: Boolean,
    onFreeze: () -> Unit,
    onClose: () -> Unit,
    onUnfreeze: () -> Unit,
) {
    DataCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(row.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    MonoText(
                        "${row.weightMb.toInt()} МБ · ${row.rssMb.toInt()} RSS · " +
                            "${row.swapMb.toInt()} своп",
                    )
                }
                StatusChip(kind = actionKind(row.action))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onFreeze, enabled = !busy) { Text("Заморозить") }
                OutlinedButton(onClick = onClose, enabled = !busy) { Text("Закрыть") }
                OutlinedButton(onClick = onUnfreeze, enabled = !busy) { Text("Сброс") }
            }
        }
    }
}

private fun actionKind(action: GameAction): StatusKind = when (action) {
    GameAction.FREEZE -> StatusKind.ON
    GameAction.CLOSE -> StatusKind.OFF
    GameAction.CLOSE_BLOCK -> StatusKind.DIVERGED
    GameAction.IGNORE -> StatusKind.UNAVAILABLE
}

private fun actionLabel(action: GameAction): String = when (action) {
    GameAction.FREEZE -> "заморожено"
    GameAction.CLOSE -> "закрывается"
    GameAction.CLOSE_BLOCK -> "закрыть и не пускать"
    GameAction.IGNORE -> "не трогать"
}
