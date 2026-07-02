package com.example.hykesync.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteHistoryScreen(
    routeHistory: List<RouteHistoryItem>,
    onOpenRouteDetail: (Long) -> Unit,
    selectedTab: MainTab,
    onNavigateTo: (MainTab) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = { MainBottomBar(selectedTab = selectedTab, onNavigateTo = onNavigateTo) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (routeHistory.isEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Aun no hay rutas guardadas", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Inicia una sesion de senderismo para comenzar a llenar tu historial.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(routeHistory.size) { index ->
                    val route = routeHistory[index]
                    HistoryRouteCard(route = route, onClick = { onOpenRouteDetail(route.sessionId) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRouteCard(route: RouteHistoryItem, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = route.startTime.toRouteDate(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Sesion ${route.sessionId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HistoryStatusBadge(route.status.toStatusLabel())
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HistoryMetricChip("Duracion", route.durationText(), Modifier.weight(1f))
                HistoryMetricChip("Lecturas", route.telemetryCount.toString(), Modifier.weight(1f))
                HistoryMetricChip("GPS", route.locationCount.toString(), Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HistoryMetricChip("Pulso", route.lastHeartRate?.let { "${it.toInt()} bpm" } ?: "--", Modifier.weight(1f))
                HistoryMetricChip("Altitud", route.lastAltitude?.let { "${it.toInt()} m" } ?: "--", Modifier.weight(1f))
            }

            Text(
                text = route.locationSummary(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryMetricChip(label: String, value: String, modifier: Modifier = Modifier) {
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
private fun HistoryStatusBadge(status: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

private fun Long.toRouteDate(): String = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(this))

private fun RouteHistoryItem.durationText(): String {
    val durationMillis = (endTime ?: System.currentTimeMillis()) - startTime
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis.coerceAtLeast(0L))
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
}

private fun RouteHistoryItem.locationSummary(): String = if (lastLatitude != null && lastLongitude != null) {
    "Ultima posicion ${lastLatitude.formatCoord()}, ${lastLongitude.formatCoord()}"
} else {
    "Sin posicion guardada en esta ruta"
}

private fun String.toStatusLabel(): String = when (this) {
    "ACTIVE" -> "Activa"
    "COMPLETED" -> "Completada"
    "PAUSED" -> "Pausada"
    else -> this
}

private fun Double.formatCoord(): String = String.format(Locale.US, "%.5f", this)
