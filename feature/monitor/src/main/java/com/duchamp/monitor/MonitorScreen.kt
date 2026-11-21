package com.duchamp.monitor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duchamp.design.DataCard
import com.duchamp.design.DuchampColor
import com.duchamp.design.MonoText
import com.duchamp.design.SectionLabel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart

/**
 * Live instrument panel: CPU frequency, GPU frequency, temperature and free memory, sampled once
 * per second and drawn on Vico line charts. Everything shown is read from the device.
 */
@Composable
fun MonitorScreen(
    modifier: Modifier = Modifier,
    sessionId: Long? = null,
    viewModel: MonitorViewModel = hiltViewModel(),
) {
    val samples by viewModel.samples.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.start(sessionId)
    }

    ProvideVicoTheme(rememberM3VicoTheme()) {
        Surface(modifier = modifier.fillMaxSize(), color = DuchampColor.Background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                SectionLabel("ИНСТРУМЕНТНАЯ ПАНЕЛЬ")
                Spacer(Modifier.height(16.dp))

                Text("Живые показания", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))

                val cpu = samples.map { it.cpuKb.toDouble() }
                val gpu = samples.map { it.gpuKhz.toDouble() }
                val temp = samples.map { it.tempC }
                val mem = samples.map { it.memKb.toDouble() / 1024.0 }

                LineChartCard(title = "CPU · МГц", values = cpu.map { it / 1000.0 })
                Spacer(Modifier.height(16.dp))
                LineChartCard(title = "GPU · МГц", values = gpu.map { it / 1000.0 })
                Spacer(Modifier.height(16.dp))
                LineChartCard(title = "ТЕМПЕРАТУРА · °C", values = temp)
                Spacer(Modifier.height(16.dp))
                LineChartCard(title = "СВОБОДНАЯ ПАМЯТЬ · МБ", values = mem)

                Spacer(Modifier.height(16.dp))
                DataCard {
                    Column(Modifier.padding(16.dp)) {
                        SectionLabel("ПОСЛЕДНИЕ ЗНАЧЕНИЯ")
                        Spacer(Modifier.height(8.dp))
                        val last = samples.lastOrNull()
                        MonoText(
                            last?.let {
                                "cpu ${it.cpuKb / 1000} МГц · gpu ${it.gpuKhz / 1000} МГц · " +
                                    "${it.tempC.toInt()}°C · ${it.memKb / 1024} МБ"
                            } ?: "—",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LineChartCard(title: String, values: List<Double>) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        producer.runTransaction {
            lineModel {
                series(
                    x = values.indices.map { it.toDouble() },
                    y = values,
                )
            }
        }
    }
    DataCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionLabel(title)
            Spacer(Modifier.height(8.dp))
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(),
                ),
                modelProducer = producer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }
    }
}
