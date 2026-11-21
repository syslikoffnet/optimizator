package com.duchamp.network

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
import com.duchamp.design.DataCard
import com.duchamp.design.DuchampColor
import com.duchamp.design.MonoText
import com.duchamp.design.SectionLabel
import com.duchamp.design.StatusChip
import com.duchamp.design.StatusKind
import kotlinx.coroutines.launch

/** Wi-Fi state, gateway, latency to the game server and VPN presence. */
@Composable
fun NetworkScreen(
    modifier: Modifier = Modifier,
    viewModel: NetworkViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val pinging by viewModel.pinging.collectAsStateWithLifecycle()
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
            SectionLabel("СЕТЬ")
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Связь", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { scope.launch { viewModel.refresh() } }) { Text("Обновить") }
            }
            Spacer(Modifier.height(16.dp))

            DataCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionLabel("WI-FI")
                    Spacer(Modifier.height(8.dp))
                    MonoText(snapshot.wifi)
                    Spacer(Modifier.height(4.dp))
                    MonoText("gateway: ${snapshot.gateway}")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("VPN", style = MaterialTheme.typography.labelMedium)
                        StatusChip(kind = if (snapshot.vpnActive) StatusKind.ON else StatusKind.OFF)
                    }
                    if (snapshot.vpnTransport.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        MonoText("transport: ${snapshot.vpnTransport}")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            DataCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionLabel("PING · game.epicgames.com")
                    Spacer(Modifier.height(8.dp))
                    MonoText(
                        snapshot.pingMs?.let { "среднее: %.1f мс".format(it) } ?: "не измерено",
                    )
                    snapshot.pingLoss?.let {
                        Spacer(Modifier.height(4.dp))
                        MonoText("потери: %.0f%%".format(it))
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { scope.launch { viewModel.ping("game.epicgames.com") } },
                        enabled = !pinging,
                    ) { Text(if (pinging) "Измерение…" else "Измерить ping") }
                }
            }
        }
    }
}
