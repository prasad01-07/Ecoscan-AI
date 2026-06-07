package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import java.io.File
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ReportEntity
import com.example.ui.theme.*
import kotlin.math.sqrt

// --- STATS KPI COMPONENT ---
@Composable
fun DashboardStatsCard(
    title: String,
    value: String,
    icon: @Composable () -> Unit,
    colorAccent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = SpruceCardSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BarkNeutralGrey.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colorAccent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = LightSageText.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = value,
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// --- DYNAMIC RADIAL METER ---
@Composable
fun CircularAnalyticsGauge(
    score: Float, // 0.0 to 1.0 (or percentage)
    title: String,
    colorAccent: Color,
    modifier: Modifier = Modifier
) {
    val progressAnimation by animateFloatAsState(
        targetValue = score,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "radial"
    )

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(90.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background grey circle
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = BarkNeutralGrey.copy(alpha = 0.25f),
                    radius = size.minDimension / 2 - 8,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // Active glowing green arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = colorAccent,
                    startAngle = -90f,
                    sweepAngle = progressAnimation * 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(
                        width = size.width - 16.dp.toPx(),
                        height = size.height - 16.dp.toPx()
                    ),
                    topLeft = Offset(8.dp.toPx(), 8.dp.toPx())
                )
            }
            // Text center
            Text(
                text = "${(score * 100).toInt()}%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            color = LightSageText,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- BAR/LINE STATS GRAPH ---
@Composable
fun EcoTrendLineChart(
    dataPoints: List<Float>, // standard percentages or scores (6 elements)
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = SpruceCardSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BarkNeutralGrey.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Environmental Success Trends",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Overall city cleanup and recyclability score progress",
                fontSize = 11.sp,
                color = LightSageText.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (dataPoints.size - 1)

                    // Draw Background grid lines
                    for (i in 0..3) {
                        val gridY = height * (i / 3f)
                        drawLine(
                            color = BarkNeutralGrey.copy(alpha = 0.15f),
                            start = Offset(0f, gridY),
                            end = Offset(width, gridY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Build line coordinates
                    val points = dataPoints.mapIndexed { index, value ->
                        Offset(
                            x = index * spacing,
                            y = height - (value * height)
                        )
                    }

                    // Draw gradient brush area underneath line
                    val path = Path().apply {
                        moveTo(0f, height)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(width, height)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                EmeraldAccent.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw vector green connecting line
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = EmeraldAccent,
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw anchor glowing dots
                    points.forEach { point ->
                        drawCircle(
                            color = SpruceCardSurface,
                            radius = 6.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = EmeraldAccent,
                            radius = 4.dp.toPx(),
                            center = point
                        )
                    }
                }
            }

            // Labels Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = LightSageText.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// --- BEFORE / AFTER COMPACT COMPARE ---
@Composable
fun BeforeAfterSlider(
    beforePath: String,
    afterPath: String?,
    modifier: Modifier = Modifier
) {
    var ratio by remember { mutableFloatStateOf(0.5f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        border = BorderStroke(1.dp, BarkNeutralGrey.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left half / Background: Before (If mock, show nice background)
            EcoScanImage(
                imagePath = beforePath,
                contentDescription = "Before Setup",
                modifier = Modifier.fillMaxSize(),
                mockFallbackIcon = Icons.Default.DeleteOutline,
                isAlternativeSeed = false
            )

            // Right half: After (If present, slice based on separator slider position)
            if (afterPath != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f - ratio)
                        .align(Alignment.CenterEnd)
                        .clip(RectangleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .requiredWidth(350.dp) // Adjust based on parent size constraints
                    ) {
                        EcoScanImage(
                            imagePath = afterPath,
                            contentDescription = "After Setup",
                            modifier = Modifier.fillMaxSize(),
                            mockFallbackIcon = Icons.Default.Verified,
                            isAlternativeSeed = true
                        )
                    }
                }

                // Split Handle / Separator Line
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = 350.dp * ratio) // Adjust based on width mapping
                ) {
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                // Interactive slider anchor
                Slider(
                    value = ratio,
                    onValueChange = { ratio = it },
                    valueRange = 0.05f..0.95f,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldAccent,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                )
            } else {
                // If clean photo doesn't exist yet, show a nice label
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Awaiting Before / After cleanup logs",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// --- HELPER WRAPPER IMAGES ---
@Composable
fun EcoScanImage(
    imagePath: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    mockFallbackIcon: ImageVector = Icons.Default.Image,
    isAlternativeSeed: Boolean = false
) {
    val isMockSymbol = imagePath.startsWith("mock_") || !imagePath.contains("/")

    if (isMockSymbol) {
        // High polish preloaded botanical vector mock displays for simulated gallery images!
        Box(
            modifier = modifier
                .background(
                    Brush.verticalGradient(
                        colors = if (isAlternativeSeed) {
                            listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))
                        } else {
                            listOf(Color(0xFF37474F), Color(0xFF212121))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = if (isAlternativeSeed) Icons.Default.NaturePeople else mockFallbackIcon,
                    contentDescription = null,
                    tint = if (isAlternativeSeed) EmeraldAccent else Color.LightGray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isAlternativeSeed) "ECO-SPOT RESTORED" else "HAZARD ZONE RECORDED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = imagePath.replace("mock_", "").replace("_", " ").uppercase(),
                    fontSize = 9.sp,
                    color = LightSageText.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    } else {
        // Render real photos uploaded from physical devices
        AsyncImage(
            model = File(imagePath),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

// --- INTERACTIVE TACTICAL CANVAS MAP ---
@Composable
fun InteractiveTacticalCanvasMap(
    reports: List<ReportEntity>,
    onPinClicked: (ReportEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Zoom levels and panning state variables reacting to gesture drag & pinches
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    // Coordinates bounding limits for New York focus bounds
    val minLat = 40.6800
    val maxLat = 40.7500
    val minLng = -74.0500
    val maxLng = -73.9500

    val latHeight = maxLat - minLat
    val lngWidth = maxLng - minLng

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .border(1.dp, BarkNeutralGrey.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DeepForestBackground)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.8f, 5.0f)
                    panOffset = Offset(
                        x = panOffset.x + pan.x,
                        y = panOffset.y + pan.y
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // --- Draw background grid coordinates ---
            val gridCount = 6
            for (i in 0 until gridCount) {
                val gridX = width * (i / (gridCount - 1).toFloat())
                drawLine(
                    color = BarkNeutralGrey.copy(alpha = 0.12f),
                    start = Offset(gridX, 0f),
                    end = Offset(gridX, height),
                    strokeWidth = 0.5.dp.toPx()
                )
                val gridY = height * (i / (gridCount - 1).toFloat())
                drawLine(
                    color = BarkNeutralGrey.copy(alpha = 0.12f),
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 0.5.dp.toPx()
                )
            }

            // --- Draw East River / Hudson River water reservoirs ---
            val waterPath = Path().apply {
                // East River vector approx placement
                moveTo(width * 0.7f, 0f)
                cubicTo(
                    width * 0.72f, height * 0.4f,
                    width * 0.55f, height * 0.64f,
                    width * 0.35f, height
                )
                lineTo(width, height)
                lineTo(width, 0f)
                close()
            }
            drawPath(
                path = waterPath,
                color = Color(0xFF102835).copy(alpha = 0.7f) // Cyber oceanic dark blue
            )

            // --- Draw Broadway & Major Avenues ---
            drawLine(
                color = BarkNeutralGrey.copy(alpha = 0.22f),
                start = Offset(width * 0.42f, 0f),
                end = Offset(width * 0.15f, height),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = BarkNeutralGrey.copy(alpha = 0.22f),
                start = Offset(0f, height * 0.5f),
                end = Offset(width, height * 0.45f),
                strokeWidth = 1.5.dp.toPx() // 14th St crossway
            )

            // --- Draw Radial Glowing Pulses (Nearby Waste Hotspots bonus) ---
            // Pulse animation of hotspot circles
            drawCircle(
                color = EmeraldAccent.copy(alpha = 0.08f),
                radius = 50.dp.toPx() * zoomScale,
                center = Offset(width * 0.35f, height * 0.55f) + panOffset
            )
            drawCircle(
                color = EmeraldAccent.copy(alpha = 0.05f),
                radius = 85.dp.toPx() * zoomScale,
                center = Offset(width * 0.35f, height * 0.55f) + panOffset
            )

            // Dynamic cluster pins matching lat/lng coords
            reports.forEach { report ->
                // Map coordinates mathematically into pixel positions (0..width, 0..height)
                val relLng = (report.longitude - minLng) / lngWidth
                val relLat = (report.latitude - minLat) / latHeight

                val basePixelX = relLng * width
                val basePixelY = height - (relLat * height) // Invert Y coords

                val finalX = ((basePixelX * zoomScale) + panOffset.x).toFloat()
                val finalY = ((basePixelY * zoomScale) + panOffset.y).toFloat()

                // Confirm bounds safety inside viewport
                if (finalX in -20f..(width + 20f) && finalY in -20f..(height + 20f)) {
                    val colorCode = when (report.wasteType.lowercase()) {
                        "plastic" -> Color(0xFFFFEB3B) // Yellow Plastic alert
                        "e-waste" -> Color(0xFFF44336) // Red electric risk
                        "organic waste" -> Color(0xFF4CAF50) // Leaf green bio heap
                        "mixed waste" -> Color(0xFFFF9800) // Orange mixes
                        else -> Color(0xFF00BCD4) // Cyan metals
                    }

                    // Pulse outer aura circle for Severity status
                    val radiusPulse = if (report.severityLevel == "Critical" || report.severityLevel == "High") 14.dp.toPx() else 8.dp.toPx()
                    drawCircle(
                        color = colorCode.copy(alpha = 0.25f),
                        radius = radiusPulse * (zoomScale * 0.85f).coerceIn(0.5f, 2.0f),
                        center = Offset(finalX, finalY)
                    )

                    // Solid Core Pin
                    drawCircle(
                        color = colorCode,
                        radius = 5.dp.toPx() * (zoomScale * 0.85f).coerceIn(0.5f, 2.0f),
                        center = Offset(finalX, finalY)
                    )

                    // Draw a mini clean indicator if status is Cleaned
                    if (report.status == "Cleaned") {
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx() * (zoomScale * 0.85f).coerceIn(0.5f, 2.0f),
                            center = Offset(finalX, finalY)
                        )
                    }
                }
            }
        }

        // Invisible touch intercept overlays computed using bounding offsets
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()

            reports.forEach { report ->
                val relLng = (report.longitude - minLng) / lngWidth
                val relLat = (report.latitude - minLat) / latHeight

                val finalX = (((relLng * width) * zoomScale) + panOffset.x).toFloat()
                val finalY = (((height - (relLat * height)) * zoomScale) + panOffset.y).toFloat()

                val tapRadius = 24.dp.value

                Box(
                    modifier = Modifier
                        .size((tapRadius * 2).dp)
                        .offset(
                            x = (finalX / density - tapRadius).dp,
                            y = (finalY / density - tapRadius).dp
                        )
                        .background(Color.Transparent)
                        .clickable { onPinClicked(report) }
                )
            }
        }

        // Canvas Map control badges float overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(SpruceCardSurface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .clickable { zoomScale = (zoomScale + 0.3f).coerceAtMost(5.0f) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(SpruceCardSurface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .clickable { zoomScale = (zoomScale - 0.3f).coerceAtLeast(0.8f) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(SpruceCardSurface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .clickable {
                        zoomScale = 1.0f
                        panOffset = Offset.Zero
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        // Bounded stats card
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = SpruceCardSurface.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE ECO-MAP RADAR",
                    fontSize = 9.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun GoogleMapWebView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            android.webkit.WebView(context).apply {
                webViewClient = android.webkit.WebViewClient()
                settings.javaScriptEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
            }
        },
        update = { webView ->
            val url = "https://maps.google.com/maps?q=$latitude,$longitude&t=&z=15&ie=UTF8&iwloc=&output=embed"
            webView.loadUrl(url)
        },
        modifier = modifier
    )
}

