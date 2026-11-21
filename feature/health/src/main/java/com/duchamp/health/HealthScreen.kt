package com.duchamp.health

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duchamp.design.DataCard
import com.duchamp.design.DuchampColor
import com.duchamp.design.MonoText
import com.duchamp.design.SectionLabel
import com.duchamp.design.StatusChip
import com.duchamp.design.StatusKind
import kotlinx.coroutines.launch

/** Battery health, charge state, thermal zones and power drain — all read from the device. */
@Composable
fun HealthScreen(
    modifier: Modifier = Modifier,
    viewModel: HealthViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
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
            SectionLabel("ЗДОРОВЬЕ")
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Зарядка и батарея", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { scope.launch { viewModel.refresh() } }) { Text("Обновить") }
            }
            Spacer(Modifier.height(16.dp))

            DataCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionLabel("БАТЕРИЯ")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Состояние", style = MaterialTheme.typography.labelMedium)
                        StatusChip(
                            kind = if (snapshot.isBypass) StatusKind.ON else StatusKind.OFF,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    MonoText("health: ${snapshot.healthPercent?.let { "%.1f".format(it) } ?: "неизвестно"} %")
                    MonoText("циклы: ${snapshot.cycleCount}")
                    MonoText("charge_full: ${snapshot.chargeFull} / ${snapshot.chargeFullDesign}")
                    MonoText("current_now: ${snapshot.currentNow}")
                    MonoText("cmd: ${snapshot.currentCmd}")
                    snapshot.powerEstimate?.let {
                        Spacer(Modifier.height(4.dp))
                        MonoText("расход: %.0f мА·ч".format(it))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            DataCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionLabel("ТЕПЛО")
                    Spacer(Modifier.height(8.dp))
                    MonoText("пиковая: ${"%.1f".format(snapshot.peakTempC)} °C")
                    Spacer(Modifier.height(8.dp))
                    snapshot.thermalZones.forEach {
                        MonoText(it)
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}
