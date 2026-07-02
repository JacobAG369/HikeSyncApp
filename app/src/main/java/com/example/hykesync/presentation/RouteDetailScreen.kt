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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    routeDetail: RouteDetailUiState,
    onBack: () -> Unit,
    onOpenMetrics: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Detalle de ruta") },
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
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(routeDetail.startTime.toRouteDate(), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Sesion ${routeDetail.sessionId} · ${routeDetail.status.toStatusLabel()}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DetailChip("Duracion", routeDetail.durationText(), Modifier.weight(1f))
                            DetailChip("Lecturas", routeDetail.telemetryCount.toString(), Modifier.weight(1f))
                            DetailChip("GPS", routeDetail.locationCount.toString(), Modifier.weight(1f))
                        }
                        Button(
                            onClick = onOpenMetrics,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Ver metricas de hiking")
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Recorrido GPS", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Vista simplificada del trazado guardado durante la ruta.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (routeDetail.routePoints.size >= 2) {
                            RouteDetailPlot(
                                points = routeDetail.routePoints,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                            )
                            Text(
                                "Inicio ${routeDetail.routePoints.first().latitude.formatCoord()}, ${routeDetail.routePoints.first().longitude.formatCoord()} · Fin ${routeDetail.routePoints.last().latitude.formatCoord()}, ${routeDetail.routePoints.last().longitude.formatCoord()}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            Text("No hay suficientes puntos GPS para dibujar la ruta.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RouteDetailPlot(points: List<RouteMapPoint>, modifier: Modifier = Modifier) {
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }
    val latRange = (maxLat - minLat).takeIf { it > 0.0 } ?: 1.0
    val lonRange = (maxLon - minLon).takeIf { it > 0.0 } ?: 1.0
    val routeColor = MaterialTheme.colorScheme.primary
    val trackBackground = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.background(trackBackground, RoundedCornerShape(14.dp))) {
        val padding = 24f
        val drawWidth = size.width - (padding * 2)
        val drawHeight = size.height - (padding * 2)
        val path = Path()

        points.forEachIndexed { index, point ->
            val x = padding + (((point.longitude - minLon) / lonRange).toFloat() * drawWidth)
            val y = padding + (drawHeight - (((point.latitude - minLat) / latRange).toFloat() * drawHeight))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = routeColor, style = Stroke(width = 8f))

        val start = points.first().toOffset(minLat, latRange, minLon, lonRange, padding, drawWidth, drawHeight)
        val end = points.last().toOffset(minLat, latRange, minLon, lonRange, padding, drawWidth, drawHeight)
        drawCircle(color = Color(0xFF2E7D32), radius = 10f, center = start)
        drawCircle(color = Color(0xFFF57C00), radius = 10f, center = end)
    }
}

private fun RouteMapPoint.toOffset(
    minLat: Double,
    latRange: Double,
    minLon: Double,
    lonRange: Double,
    padding: Float,
    drawWidth: Float,
    drawHeight: Float,
): Offset {
    return Offset(
        x = padding + (((longitude - minLon) / lonRange).toFloat() * drawWidth),
        y = padding + (drawHeight - (((latitude - minLat) / latRange).toFloat() * drawHeight)),
    )
}

private fun Long.toRouteDate(): String = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(this))

private fun RouteDetailUiState.durationText(): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis.coerceAtLeast(0L))
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
}

private fun String.toStatusLabel(): String = when (this) {
    "ACTIVE" -> "Activa"
    "COMPLETED" -> "Completada"
    "PAUSED" -> "Pausada"
    else -> this
}

private fun Double.formatCoord(): String = String.format(Locale.US, "%.5f", this)
