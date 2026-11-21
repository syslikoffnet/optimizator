package com.duchamp.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duchamp.data.db.SessionEntity
import com.duchamp.design.DataCard
import com.duchamp.design.DuchampColor
import com.duchamp.design.MonoText
import com.duchamp.design.SectionLabel
import kotlinx.coroutines.launch

/**
 * History of completed game-mode sessions with a per-session post-game report: duration, average
 * CPU, peak temperature and the throttle point — all from real recorded samples.
 */
@Composable
fun SessionsScreen(
    modifier: Modifier = Modifier,
    viewModel: SessionsViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val report by viewModel.report.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Surface(modifier = modifier.fillMaxSize(), color = DuchampColor.Background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            SectionLabel("ИСТОРИЯ СЕАНСОВ")
            Spacer(Modifier.height(16.dp))

            report?.let { r ->
                ReportCard(r) { viewModel.closeReport() }
                Spacer(Modifier.height(24.dp))
            }

            if (sessions.isEmpty()) {
                Text("Нет сеансов. Запустите игровой режим.", color = DuchampColor.TextTertiary)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sessions, key = { it.id }) { session ->
                        SessionRow(
                            session = session,
                            onClick = { viewModel.open(session.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: SessionEntity, onClick: () -> Unit) {
    DataCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                MonoText("сеанс #${session.id}")
                Spacer(Modifier.height(4.dp))
                Text("${session.durationSec / 60} мин", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(onClick = onClick) { Text("Открыть") }
        }
    }
}

@Composable
private fun ReportCard(report: SessionReport, onClose: () -> Unit) {
    DataCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Отчёт по сеансу", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onClose) { Text("Скрыть") }
            }
            Spacer(Modifier.height(12.dp))
            SectionLabel("ФАКТЫ")
            Spacer(Modifier.height(8.dp))
            MonoText("длительность: ${report.durationMin} мин")
            Spacer(Modifier.height(4.dp))
            MonoText("средний CPU: ${"%.0f".format(report.avgCpuMhz)} МГц")
            Spacer(Modifier.height(4.dp))
            MonoText("пиковая температура: ${"%.1f".format(report.peakTempC)} °C")
            Spacer(Modifier.height(4.dp))
            MonoText(
                "троттл: ${
                    if (report.throttleStart >= 0) "через ${report.throttleStart} с" else "не зафиксирован"
                }",
            )
            Spacer(Modifier.height(4.dp))
            MonoText("записано точек: ${report.samples.size}")
        }
    }
}
