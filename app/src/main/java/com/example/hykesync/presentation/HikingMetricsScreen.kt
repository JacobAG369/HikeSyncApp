package com.example.hykesync.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikingMetricsScreen(
    routeDetail: RouteDetailUiState,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Metricas de hiking") },
                navigationIcon = {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) { Text("Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Distancia", "${routeDetail.distanceKm.pretty(2)} km", Modifier.weight(1f))
                    MetricCard("Velocidad media", routeDetail.averageSpeedKmh?.let { "${it.pretty(1)} km/h" } ?: "--", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Desnivel +", "${routeDetail.elevationGainMeters.pretty(0)} m", Modifier.weight(1f))
                    MetricCard("Desnivel -", "${routeDetail.elevationLossMeters.pretty(0)} m", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Pulso medio", routeDetail.averageHeartRate?.let { "${it.toInt()} bpm" } ?: "--", Modifier.weight(1f))
                    MetricCard("Pulso max", routeDetail.maxHeartRate?.let { "${it.toInt()} bpm" } ?: "--", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Altitud min", routeDetail.minAltitude?.let { "${it.toInt()} m" } ?: "--", Modifier.weight(1f))
                    MetricCard("Altitud max", routeDetail.maxAltitude?.let { "${it.toInt()} m" } ?: "--", Modifier.weight(1f))
                }
            }
            item {
                ChartCard(
                    title = "Perfil cardiaco",
                    subtitle = "Ultimas lecturas de frecuencia cardiaca guardadas",
                    values = routeDetail.heartRateSeries,
                    accent = Color(0xFFD94A4A),
                    unit = "bpm",
                )
            }
            item {
                ChartCard(
                    title = "Perfil de altitud",
                    subtitle = "Lecturas recientes de ascenso y descenso",
                    values = routeDetail.altitudeSeries,
                    accent = MaterialTheme.colorScheme.primary,
                    unit = "m",
                )
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    values: List<Float>,
    accent: Color,
    unit: String,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (values.size >= 2) {
                MetricsLineChart(
                    values = values,
                    accent = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                )
                Text(
                    "Min ${values.minOrNull()?.toInt() ?: 0} $unit · Max ${values.maxOrNull()?.toInt() ?: 0} $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        "Aun no hay suficientes datos para dibujar esta grafica.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsLineChart(values: List<Float>, accent: Color, modifier: Modifier = Modifier) {
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: min
    val range = (max - min).takeIf { it > 0f } ?: 1f

    Canvas(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))) {
        val horizontalStep = if (values.size > 1) size.width / (values.size - 1) else size.width
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * horizontalStep
            val y = size.height - (((value - min) / range) * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = accent, style = Stroke(width = 6f))
    }
}

private fun Float.pretty(decimals: Int): String = String.format(java.util.Locale.US, "%.${decimals}f", this)
