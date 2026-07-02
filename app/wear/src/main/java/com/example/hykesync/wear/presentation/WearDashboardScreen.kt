package com.example.hykesync.wear.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import java.util.Locale

@Composable
fun WearDashboardScreen(
    sensorState: WearSensorState,
    permissionState: BodySensorPermissionState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        modifier = modifier.background(BackgroundColor),
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(horizontal = 8.dp),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 0),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "HikeSync",
                        color = ForestGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ruta en tiempo real",
                        color = SecondaryTextColor,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (permissionState == BodySensorPermissionState.Denied) {
                item {
                    AlertPill(
                        title = "Permiso cardiaco denegado",
                        message = "Activa Sensores corporales para ver el pulso en vivo.",
                    )
                }
            }

            item {
                MetricPill(
                    label = "Frecuencia cardiaca",
                    value = heartRateValue(sensorState, permissionState),
                    detail = heartRateDetail(sensorState, permissionState),
                    iconTint = HeartAccent,
                    icon = { Icon(Icons.Rounded.Favorite, contentDescription = null, tint = HeartAccent) },
                    isUnavailable = !sensorState.hasHeartRateSensor || permissionState == BodySensorPermissionState.Denied,
                )
            }

            item {
                MetricPill(
                    label = "Altitud",
                    value = altitudeValue(sensorState),
                    detail = altitudeDetail(sensorState),
                    iconTint = TerrainAccent,
                    icon = { Icon(Icons.Rounded.Terrain, contentDescription = null, tint = TerrainAccent) },
                    isUnavailable = !sensorState.hasPressureSensor,
                )
            }

            item {
                MetricPill(
                    label = "Rumbo",
                    value = orientationValue(sensorState),
                    detail = orientationDetail(sensorState),
                    iconTint = OrientationAccent,
                    icon = { Icon(Icons.Rounded.Explore, contentDescription = null, tint = OrientationAccent) },
                    isUnavailable = !sensorState.hasOrientationSensors,
                )
            }

            item {
                StatusPill(
                    isStreaming = sensorState.heartRate != null || sensorState.pressure != null || sensorState.azimuth != null,
                    permissionState = permissionState,
                    pendingSyncCount = sensorState.pendingSyncCount,
                    maxPendingSyncCount = sensorState.maxPendingSyncCount,
                    isCacheNearCapacity = sensorState.isCacheNearCapacity,
                )
            }
        }
    }
}

@Composable
private fun CompassRose(
    azimuth: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val ringRadius = radius * 0.82f
        val arrowLength = radius * 0.58f
        val angleRadians = Math.toRadians((-azimuth + 90f).toDouble())
        val arrowTip = Offset(
            x = center.x + (kotlin.math.cos(angleRadians) * arrowLength).toFloat(),
            y = center.y - (kotlin.math.sin(angleRadians) * arrowLength).toFloat(),
        )
        val leftWing = Offset(
            x = center.x + (kotlin.math.cos(angleRadians + 2.55) * radius * 0.22f).toFloat(),
            y = center.y - (kotlin.math.sin(angleRadians + 2.55) * radius * 0.22f).toFloat(),
        )
        val rightWing = Offset(
            x = center.x + (kotlin.math.cos(angleRadians - 2.55) * radius * 0.22f).toFloat(),
            y = center.y - (kotlin.math.sin(angleRadians - 2.55) * radius * 0.22f).toFloat(),
        )

        drawCircle(color = StatusPillColor, radius = radius)
        drawCircle(color = SecondaryTextColor.copy(alpha = 0.35f), radius = ringRadius, style = Stroke(width = 4f))

        repeat(12) { index ->
            val tickAngle = Math.toRadians((index * 30.0) - 90.0)
            val outer = Offset(
                x = center.x + (kotlin.math.cos(tickAngle) * ringRadius).toFloat(),
                y = center.y + (kotlin.math.sin(tickAngle) * ringRadius).toFloat(),
            )
            val inner = Offset(
                x = center.x + (kotlin.math.cos(tickAngle) * ringRadius * if (index % 3 == 0) 0.72f else 0.82f).toFloat(),
                y = center.y + (kotlin.math.sin(tickAngle) * ringRadius * if (index % 3 == 0) 0.72f else 0.82f).toFloat(),
            )
            drawLine(
                color = SecondaryTextColor.copy(alpha = 0.75f),
                start = inner,
                end = outer,
                strokeWidth = if (index % 3 == 0) 4f else 2f,
            )
        }

        val arrowPath = Path().apply {
            moveTo(arrowTip.x, arrowTip.y)
            lineTo(leftWing.x, leftWing.y)
            lineTo(center.x, center.y)
            lineTo(rightWing.x, rightWing.y)
            close()
        }
        drawPath(path = arrowPath, color = OrientationAccent)
        drawCircle(color = Color.White, radius = 6f, center = center)
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    detail: String,
    iconTint: Color,
    icon: @Composable () -> Unit,
    isUnavailable: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PillColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = MutedTextColor,
                fontSize = 11.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = if (isUnavailable) MutedTextColor else PrimaryTextColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detail,
                color = SecondaryTextColor,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun AlertPill(title: String, message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WarningAccent.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(WarningAccent),
        )
        Column {
            Text(text = title, color = PrimaryTextColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = message, color = MutedTextColor, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StatusPill(
    isStreaming: Boolean,
    permissionState: BodySensorPermissionState,
    pendingSyncCount: Int,
    maxPendingSyncCount: Int = 0,
    isCacheNearCapacity: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StatusPillColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = "Estado", color = MutedTextColor, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isStreaming) "Sensores activos" else "Esperando datos",
                color = PrimaryTextColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when (permissionState) {
                    BodySensorPermissionState.Granted -> "Permiso cardiaco activo"
                    BodySensorPermissionState.Denied -> "Pulso limitado por permiso"
                    BodySensorPermissionState.Unknown -> "Verificando permisos"
                },
                color = SecondaryTextColor,
                fontSize = 10.sp,
            )
            if (pendingSyncCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (maxPendingSyncCount > 0) {
                        "$pendingSyncCount/$maxPendingSyncCount lecturas pendientes"
                    } else {
                        "$pendingSyncCount lecturas pendientes"
                    },
                    color = if (isCacheNearCapacity) WarningAccent else TerrainAccent,
                    fontSize = 10.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isStreaming) LiveAccent else SecondaryTextColor),
        )
    }
}

private fun heartRateValue(
    sensorState: WearSensorState,
    permissionState: BodySensorPermissionState,
): String = when {
    !sensorState.hasHeartRateSensor -> "No disponible"
    permissionState == BodySensorPermissionState.Denied -> "Permiso denegado"
    sensorState.heartRate != null -> "${sensorState.heartRate.toInt()} bpm"
    else -> "Buscando..."
}

private fun heartRateDetail(
    sensorState: WearSensorState,
    permissionState: BodySensorPermissionState,
): String = when {
    !sensorState.hasHeartRateSensor -> "Este reloj no tiene sensor cardiaco"
    permissionState == BodySensorPermissionState.Denied -> "Activa Sensores corporales en ajustes"
    sensorState.heartRate != null -> "Lectura en tiempo real"
    else -> "Esperando la primera lectura"
}

private fun altitudeValue(sensorState: WearSensorState): String = when {
    !sensorState.hasPressureSensor -> "No disponible"
    sensorState.altitude != null -> "${sensorState.altitude.toInt()} m"
    else -> "Buscando..."
}

private fun altitudeDetail(sensorState: WearSensorState): String = when {
    !sensorState.hasPressureSensor -> "Barometro no disponible en este reloj"
    sensorState.pressure != null -> "${sensorState.pressure.format(1)} hPa"
    else -> "Esperando lectura del barometro"
}

private fun orientationValue(sensorState: WearSensorState): String = when {
    !sensorState.hasOrientationSensors -> "No disponible"
    sensorState.azimuth != null -> "${sensorState.azimuth.toCardinal()} · ${sensorState.azimuth.toInt()}°"
    else -> "Buscando..."
}

private fun orientationDetail(sensorState: WearSensorState): String = when {
    !sensorState.hasOrientationSensors -> "Brujula no disponible en este reloj"
    sensorState.pitch != null && sensorState.roll != null -> {
        "Inclinacion ${sensorState.pitch.toInt()}° / Balanceo ${sensorState.roll.toInt()}°"
    }
    else -> "Esperando datos de orientacion"
}

private fun Float.format(decimals: Int): String = String.format(Locale.US, "%.${decimals}f", this)

private fun Float.toCardinal(): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val normalized = ((this % 360f) + 360f) % 360f
    val index = (((normalized + 22.5f) % 360f) / 45f).toInt()
    return directions[index]
}

private val BackgroundColor = Color(0xFFF4F7F2)
private val PillColor = Color(0xFFFFFFFF)
private val StatusPillColor = Color(0xFFE7EFE4)
private val MutedTextColor = Color(0xFF5C6B5E)
private val SecondaryTextColor = Color(0xFF6E7B6A)
private val PrimaryTextColor = Color(0xFF1A1C19)
private val ForestGreen = Color(0xFF2E7D32)
private val HeartAccent = Color(0xFFD94A4A)
private val TerrainAccent = Color(0xFF2E7D32)
private val OrientationAccent = Color(0xFFF57C00)
private val LiveAccent = Color(0xFF2E7D32)
private val WarningAccent = Color(0xFFF57C00)
