package com.duchamp.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duchamp.data.profile.GameAction
import com.duchamp.design.DataCard
import com.duchamp.design.DuchampColor
import com.duchamp.design.MonoText
import com.duchamp.design.SectionLabel
import com.duchamp.design.StatusChip
import com.duchamp.design.StatusKind
import kotlinx.coroutines.launch

/**
 * First-run setup. Verify the device, pick which apps to freeze/close during a game, choose the
 * audio/messenger keep-alive, and finish — the profile is saved and the wizard is never shown again.
 */
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val root by viewModel.rootStatus.collectAsStateWithLifecycle()
    val installed by viewModel.installed.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val actions by viewModel.selectedAction.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Surface(modifier = modifier.fillMaxSize(), color = DuchampColor.Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            SectionLabel("НАСТРОЙКА")
            Spacer(Modifier.height(8.dp))
            Text("Приветствие", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "duchamp — игровой режим для POCO X6 Pro (duchamp). " +
                    "Проверим устройство, выберем приложения и подготовим телефон к игре.",
                style = MaterialTheme.typography.bodyMedium,
                color = DuchampColor.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))

            // Step 1: device check.
            DataCard {
                Column(Modifier.padding(16.dp)) {
                    SectionLabel("УСТРОЙСТВО")
                    Spacer(Modifier.height(8.dp))
                    StatusRow("Root", root.rootGranted)
                    StatusRow("Устройство", root.targetDevice)
                    MonoText(root.deviceName)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Step 2: audio / messenger keep-alive.
            SectionLabel("ОСТАВИТЬ ВОСПРОИЗВЕДЕНИЕМ")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.setMusic("") }) { Text("Сбросить") }
                Button(onClick = { viewModel.setMusic(profile.musicPackage.ifBlank { "com.google.android.apps.youtube.music" }) }) {
                    Text("YouTube Music")
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionLabel("MESSENGER")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.setMessenger("") }) { Text("Сбросить") }

            Spacer(Modifier.height(16.dp))

            // Step 3: per-package actions.
            SectionLabel("ДИСПЕТЧЕР ЗАДАЧ · ${installed.size} ПРИЛОЖЕНИЙ")
            Spacer(Modifier.height(8.dp))
            if (installed.isEmpty()) {
                Text("Нет данных о приложениях", color = DuchampColor.TextTertiary)
            } else {
                installed.take(60).forEach { pkg ->
                    val action = actions[pkg]
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(pkg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActionButton("ЗАМОРОЖ.", GameAction.FREEZE) { viewModel.setAction(pkg, it) }
                            ActionButton("ЗАКР.", GameAction.CLOSE) { viewModel.setAction(pkg, it) }
                            if (action != null) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 4.dp),
                                    tint = DuchampColor.Ok,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = DuchampColor.Hairline)
            Spacer(Modifier.height(16.dp))

            // Step 4: toggles.
            SectionLabel("ПАРАМЕТРЫ")
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                label = "Отключение зарядки",
                checked = profile.enableBypassOnCharge,
                onChange = { viewModel.setBypassOnCharge(it) },
            )
            ToggleRow(
                label = "Использую VPN",
                checked = profile.useVpn,
                onChange = { viewModel.setUseVpn(it) },
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.finish()) onFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("ГОТОВО · ЗАПУСКИТЬ ИГРОВОЙ РЕЖИМ")
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = DuchampColor.TextSecondary)
        StatusChip(kind = if (ok) StatusKind.ON else StatusKind.OFF)
    }
}

@Composable
private fun ActionButton(
    label: String,
    action: GameAction,
    onClick: (GameAction) -> Unit,
) {
    OutlinedButton(onClick = { onClick(action) }) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { onChange(!checked) }) {
            Text(if (checked) "ВКЛ" else "ВЫКЛ")
        }
    }
}
