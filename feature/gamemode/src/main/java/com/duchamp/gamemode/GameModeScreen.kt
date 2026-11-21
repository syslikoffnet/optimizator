package com.duchamp.gamemode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duchamp.design.DataCard
import com.duchamp.design.DuchampColor
import com.duchamp.design.MonoText
import com.duchamp.design.SectionLabel
import com.duchamp.design.StatusChip
import com.duchamp.design.StatusKind
import com.duchamp.data.gamemode.GameModePhase
import kotlinx.coroutines.launch

/**
 * The single-screen hero: one big "Игровой режим" button, live progress of the start sequence,
 * and a compact summary once active.
 */
@Composable
fun GameModeScreen(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
    viewModel: GameModeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DuchampColor.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SectionLabel("ИГРОВОЙ РЕЖИМ")
            Spacer(Modifier.height(16.dp))

            // The hero button.
            Box(contentAlignment = Alignment.Center) {
                when {
                    state.isBusy -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(128.dp),
                            strokeWidth = 6.dp,
                            color = DuchampColor.Accent,
                        )
                    }
                    state.isActive -> {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    viewModel.finish("Завершение пользователем")
                                    onFinished()
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .height(128.dp)
                                .width(220.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.PowerSettingsNew, contentDescription = null)
                                Spacer(Modifier.height(8.dp))
                                Text("ЗАВЕРШИТЬ", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = { scope.launch { viewModel.start() } },
                            shape = CircleShape,
                            modifier = Modifier
                                .height(128.dp)
                                .width(220.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Gamepad, contentDescription = null)
                                Spacer(Modifier.height(8.dp))
                                Text("ИГРОВОЙ РЕЖИМ", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                state.message.ifBlank { "Нажмите, чтобы подготовить телефон к игре" },
                style = MaterialTheme.typography.bodyMedium,
                color = DuchampColor.TextSecondary,
                textAlign = TextAlign.Center,
            )

            if (state.progress > 0f && state.phase != GameModePhase.ACTIVE) {
                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = DuchampColor.Accent,
                    trackColor = DuchampColor.Hairline,
                )
            }

            Spacer(Modifier.height(24.dp))
            DataCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionLabel("СТАТУС")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Режим", style = MaterialTheme.typography.labelMedium)
                        StatusChip(
                            kind = when (state.phase) {
                                GameModePhase.ACTIVE -> StatusKind.ON
                                GameModePhase.FAILED -> StatusKind.ERROR
                                GameModePhase.DONE -> StatusKind.OFF
                                else -> StatusKind.OFF
                            },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    val result = state.result
                    if (result != null) {
                        MonoText("сеанс #${result.sessionId}")
                        Spacer(Modifier.height(4.dp))
                        MonoText("${result.applied.size} настроек · ${result.frozen.size} заморожено")
                        if (result.failed.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            MonoText("сбои: ${result.failed.size}", modifier = Modifier.padding(top = 8.dp))
                        }
                    } else {
                        MonoText("—")
                    }
                }
            }
        }
    }
}
