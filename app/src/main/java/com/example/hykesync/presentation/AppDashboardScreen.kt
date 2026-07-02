package com.example.hykesync.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hykesync.data.repository.SyncManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDashboardScreen(
    viewModel: AppViewModel,
    selectedTab: MainTab,
    onNavigateTo: (MainTab) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HikeSync",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            Column {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 0.dp,
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val syncResult = runCatching { viewModel.syncActiveSession() }
                                    .getOrElse { SyncManager.SyncResult.Error }
                                val message = when (syncResult) {
                                    SyncManager.SyncResult.Success -> "Sincronizacion exitosa"
                                    SyncManager.SyncResult.NoNewData -> "No hay datos nuevos para sincronizar"
                                    SyncManager.SyncResult.Error -> "Error de sincronizacion"
                                }
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        enabled = uiState.sessionId > 0L && !isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                        }
                        Text(if (isSyncing) "Guardando..." else "Guardar datos")
                    }
                }
                MainBottomBar(selectedTab = selectedTab, onNavigateTo = onNavigateTo)
            }
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
                SessionStatusCard(
                    sessionId = uiState.sessionId,
                    isSessionActive = uiState.isSessionActive,
                    connectionStatus = uiState.connectionStatus,
                    latestTelemetryConnectionStatus = uiState.latestTelemetryConnectionStatus,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Telemetria en vivo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TelemetryCard(
                            modifier = Modifier.weight(1f),
                            title = "Frecuencia cardiaca",
                            value = "${uiState.lastHeartRate.toInt()} bpm",
                            icon = Icons.Rounded.Favorite,
                            accent = Color(0xFFD94A4A),
                        )
                        TelemetryCard(
                            modifier = Modifier.weight(1f),
                            title = "Altitud",
                            value = "${uiState.lastAltitude.toInt()} m",
                            icon = Icons.Rounded.Landscape,
                            accent = MaterialTheme.colorScheme.primary,
                        )
                    }
                    TelemetryCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Coordenadas GPS",
                        value = "${uiState.latitude.formatCoord()}, ${uiState.longitude.formatCoord()}",
                        supporting = "Ultimo punto almacenado localmente",
                        icon = Icons.Rounded.Place,
                        accent = MaterialTheme.colorScheme.secondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TelemetryCard(
                            modifier = Modifier.weight(1f),
                            title = "Rumbo",
                            value = uiState.lastAzimuth.toHeadingText(),
                            supporting = "Orientacion recibida desde el reloj",
                            icon = Icons.Rounded.Explore,
                            accent = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    CompassTelemetryCard(azimuth = uiState.lastAzimuth)
                }
            }

            item {
                SessionActionCard(
                    isSessionActive = uiState.isSessionActive,
                    onToggleSession = viewModel::toggleSession,
                )
            }
        }
    }
}

@Composable
fun MainBottomBar(selectedTab: MainTab, onNavigateTo: (MainTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = selectedTab == MainTab.Dashboard,
            onClick = { onNavigateTo(MainTab.Dashboard) },
            icon = { Icon(Icons.Rounded.Place, contentDescription = null) },
            label = { Text("Inicio") },
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.History,
            onClick = { onNavigateTo(MainTab.History) },
            icon = { Icon(Icons.Rounded.History, contentDescription = null) },
            label = { Text("Historial") },
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.Devices,
            onClick = { onNavigateTo(MainTab.Devices) },
            icon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
            label = { Text("Dispositivos") },
        )
    }
}

@Composable
private fun CompassTelemetryCard(azimuth: Float) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Brujula",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            MobileCompassRose(
                azimuth = azimuth,
                modifier = Modifier.size(150.dp),
            )
            Text(
                text = azimuth.toHeadingText(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Brújula sincronizada desde el smartwatch",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MobileCompassRose(
    azimuth: Float,
    modifier: Modifier = Modifier,
) {
    val ringColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.secondary
    val centerDot = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.background(cardColor, RoundedCornerShape(18.dp))) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val ringRadius = radius * 0.76f
        val arrowLength = radius * 0.55f
        val angleRadians = Math.toRadians((-azimuth + 90f).toDouble())

        drawCircle(color = cardColor, radius = radius)
        drawCircle(color = ringColor, radius = ringRadius, style = Stroke(width = 5f))

        repeat(12) { index ->
            val tickAngle = Math.toRadians((index * 30.0) - 90.0)
            val outer = Offset(
                x = center.x + (kotlin.math.cos(tickAngle) * ringRadius).toFloat(),
                y = center.y + (kotlin.math.sin(tickAngle) * ringRadius).toFloat(),
            )
            val innerScale = if (index % 3 == 0) 0.7f else 0.82f
            val inner = Offset(
                x = center.x + (kotlin.math.cos(tickAngle) * ringRadius * innerScale).toFloat(),
                y = center.y + (kotlin.math.sin(tickAngle) * ringRadius * innerScale).toFloat(),
            )
            drawLine(color = ringColor, start = inner, end = outer, strokeWidth = if (index % 3 == 0) 5f else 3f)
        }

        val tip = Offset(
            x = center.x + (kotlin.math.cos(angleRadians) * arrowLength).toFloat(),
            y = center.y - (kotlin.math.sin(angleRadians) * arrowLength).toFloat(),
        )
        val left = Offset(
            x = center.x + (kotlin.math.cos(angleRadians + 2.55) * radius * 0.22f).toFloat(),
            y = center.y - (kotlin.math.sin(angleRadians + 2.55) * radius * 0.22f).toFloat(),
        )
        val right = Offset(
            x = center.x + (kotlin.math.cos(angleRadians - 2.55) * radius * 0.22f).toFloat(),
            y = center.y - (kotlin.math.sin(angleRadians - 2.55) * radius * 0.22f).toFloat(),
        )

        drawPath(
            path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(left.x, left.y)
                lineTo(center.x, center.y)
                lineTo(right.x, right.y)
                close()
            },
            color = accent,
        )
        drawCircle(color = centerDot, radius = 8f, center = center)
    }
}

enum class MainTab {
    Dashboard,
    History,
    Devices,
}

@Composable
private fun RouteHistorySection(
    routeHistory: List<RouteHistoryItem>,
    onRouteSelected: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Historial reciente",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (routeHistory.isEmpty()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Aun no hay rutas guardadas",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Inicia una sesion para comenzar a construir tu historial de senderismo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            routeHistory.forEach { route ->
                RouteHistoryCard(route = route, onClick = { onRouteSelected(route.sessionId) })
            }
        }
    }
}

@Composable
private fun RouteHistoryCard(route: RouteHistoryItem, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
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
                SessionBadge(
                    text = route.status.toStatusLabel(),
                    accent = if (route.endTime == null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HistoryMetricChip(
                    modifier = Modifier.weight(1f),
                    label = "Duracion",
                    value = route.durationText(),
                )
                HistoryMetricChip(
                    modifier = Modifier.weight(1f),
                    label = "Lecturas",
                    value = route.telemetryCount.toString(),
                )
                HistoryMetricChip(
                    modifier = Modifier.weight(1f),
                    label = "Puntos GPS",
                    value = route.locationCount.toString(),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HistoryMetricChip(
                    modifier = Modifier.weight(1f),
                    label = "Pulso final",
                    value = route.lastHeartRate?.let { "${it.toInt()} bpm" } ?: "--",
                )
                HistoryMetricChip(
                    modifier = Modifier.weight(1f),
                    label = "Altitud final",
                    value = route.lastAltitude?.let { "${it.toInt()} m" } ?: "--",
                )
            }

            Text(
                text = route.locationSummary() + "  ·  Toca para ver detalle",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteDetailSection(
    routeDetail: RouteDetailUiState,
    onClose: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Detalle de ruta",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Sesion ${routeDetail.sessionId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onClose,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Text("Cerrar")
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = routeDetail.startTime.toRouteDate(),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Estado ${routeDetail.status.toStatusLabel()} · ${routeDetail.durationText()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HistoryMetricChip(
                        modifier = Modifier.weight(1f),
                        label = "Pulso medio",
                        value = routeDetail.averageHeartRate?.let { "${it.toInt()} bpm" } ?: "--",
                    )
                    HistoryMetricChip(
                        modifier = Modifier.weight(1f),
                        label = "Pulso max",
                        value = routeDetail.maxHeartRate?.let { "${it.toInt()} bpm" } ?: "--",
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HistoryMetricChip(
                        modifier = Modifier.weight(1f),
                        label = "Altitud min",
                        value = routeDetail.minAltitude?.let { "${it.toInt()} m" } ?: "--",
                    )
                    HistoryMetricChip(
                        modifier = Modifier.weight(1f),
                        label = "Altitud max",
                        value = routeDetail.maxAltitude?.let { "${it.toInt()} m" } ?: "--",
                    )
                }
            }
        }

        TelemetryChartCard(
            title = "Frecuencia cardiaca",
            subtitle = "Ultimas lecturas guardadas en Room",
            values = routeDetail.heartRateSeries,
            accent = Color(0xFFD94A4A),
            unit = "bpm",
        )

        TelemetryChartCard(
            title = "Altitud",
            subtitle = "Perfil de ascenso y descenso reciente",
            values = routeDetail.altitudeSeries,
            accent = MaterialTheme.colorScheme.primary,
            unit = "m",
        )

        RouteMapCard(routePoints = routeDetail.routePoints)
    }
}

@Composable
private fun TelemetryChartCard(
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
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (values.size >= 2) {
                LineChart(
                    values = values,
                    accent = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                )
                Text(
                    text = "Min ${values.minOrNull()?.toInt() ?: 0} $unit · Max ${values.maxOrNull()?.toInt() ?: 0} $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Aun no hay suficientes datos para dibujar la grafica.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LineChart(
    values: List<Float>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: min
    val range = (max - min).takeIf { it > 0f } ?: 1f

    Canvas(modifier = modifier) {
        val horizontalStep = if (values.size > 1) size.width / (values.size - 1) else size.width
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * horizontalStep
            val yRatio = (value - min) / range
            val y = size.height - (yRatio * size.height)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = accent,
            style = Stroke(width = 6f),
        )
    }
}

@Composable
private fun RouteMapCard(routePoints: List<RouteMapPoint>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Recorrido GPS", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Vista simplificada del trazado guardado en la sesion.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (routePoints.size >= 2) {
                RoutePlot(
                    points = routePoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp)
                        .height(220.dp),
                )
                Text(
                    text = "Inicio ${routePoints.first().latitude.formatCoord()}, ${routePoints.first().longitude.formatCoord()} · Fin ${routePoints.last().latitude.formatCoord()}, ${routePoints.last().longitude.formatCoord()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Aun no hay suficientes puntos GPS para dibujar el recorrido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RoutePlot(
    points: List<RouteMapPoint>,
    modifier: Modifier = Modifier,
) {
    val routeColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }
    val latRange = (maxLat - minLat).takeIf { it > 0.0 } ?: 1.0
    val lonRange = (maxLon - minLon).takeIf { it > 0.0 } ?: 1.0

    Canvas(modifier = modifier.background(surfaceVariant, RoundedCornerShape(14.dp))) {
        val padding = 24f
        val drawWidth = size.width - (padding * 2)
        val drawHeight = size.height - (padding * 2)
        val path = Path()

        points.forEachIndexed { index, point ->
            val x = padding + (((point.longitude - minLon) / lonRange).toFloat() * drawWidth)
            val y = padding + (drawHeight - (((point.latitude - minLat) / latRange).toFloat() * drawHeight))
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = routeColor,
            style = Stroke(width = 8f),
        )

        val start = points.first()
        val end = points.last()
        val startOffset = Offset(
            x = padding + (((start.longitude - minLon) / lonRange).toFloat() * drawWidth),
            y = padding + (drawHeight - (((start.latitude - minLat) / latRange).toFloat() * drawHeight)),
        )
        val endOffset = Offset(
            x = padding + (((end.longitude - minLon) / lonRange).toFloat() * drawWidth),
            y = padding + (drawHeight - (((end.latitude - minLat) / latRange).toFloat() * drawHeight)),
        )

        drawCircle(color = Color(0xFF2E7D32), radius = 10f, center = startOffset)
        drawCircle(color = Color(0xFFF57C00), radius = 10f, center = endOffset)
    }
}

@Composable
private fun HistoryMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SessionStatusCard(
    sessionId: Long,
    isSessionActive: Boolean,
    connectionStatus: String,
    latestTelemetryConnectionStatus: String,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Estado de sesion",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (sessionId > 0L) "ID $sessionId" else "Sin sesion activa",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SessionBadge(
                    text = if (isSessionActive) "Recolectando" else "En espera",
                    accent = if (isSessionActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enlace reloj / telefono",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = connectionStatus,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Telemetria: $latestTelemetryConnectionStatus",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(10.dp)
                            .background(
                                color = if (isSessionActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionBadge(text: String, accent: Color) {
    Surface(
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = accent,
        )
    }
}

@Composable
private fun TelemetryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    supporting: String = "Ultima lectura guardada en Room",
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = accent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = accent,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionActionCard(
    isSessionActive: Boolean,
    onToggleSession: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Control de salida",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (isSessionActive) {
                    "La sesion esta activa y el reloj puede seguir almacenando telemetria."
                } else {
                    "Inicia una ruta para comenzar a recolectar datos del recorrido."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onToggleSession,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = if (isSessionActive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                },
            ) {
                Text(if (isSessionActive) "Detener ruta" else "Iniciar ruta")
            }
        }
    }
}

private fun Double.formatCoord(): String = String.format(Locale.US, "%.5f", this)

private fun Float.toHeadingText(): String {
    val normalized = ((this % 360f) + 360f) % 360f
    val directions = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val index = (((normalized + 22.5f) % 360f) / 45f).toInt()
    return "${directions[index]} · ${normalized.toInt()}°"
}

private fun Long.toRouteDate(): String {
    val formatter = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
    return formatter.format(Date(this))
}

private fun RouteHistoryItem.durationText(): String {
    val durationMillis = (endTime ?: System.currentTimeMillis()) - startTime
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis.coerceAtLeast(0L))
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
}

private fun RouteDetailUiState.durationText(): String {
    val durationMillis = (endTime ?: System.currentTimeMillis()) - startTime
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis.coerceAtLeast(0L))
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
}

private fun RouteHistoryItem.locationSummary(): String {
    return if (lastLatitude != null && lastLongitude != null) {
        "Ultima posicion ${lastLatitude.formatCoord()}, ${lastLongitude.formatCoord()}"
    } else {
        "Sin posicion guardada en esta ruta"
    }
}

private fun String.toStatusLabel(): String = when (this) {
    "ACTIVE" -> "Activa"
    "COMPLETED" -> "Completada"
    "PAUSED" -> "Pausada"
    else -> this
}
