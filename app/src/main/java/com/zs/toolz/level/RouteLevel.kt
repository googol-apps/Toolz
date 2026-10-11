package com.zs.toolz.level

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.text.Text
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

@Serializable
object RouteLevel: NavKey {

    @Composable
    @NonRestartableComposable
    operator fun invoke() {
        val tilt by rememberTilt()

        // Smooth the movement
        val smoothX by animateFloatAsState(tilt.first, spring(stiffness = 300f), label = "x")
        val smoothY by animateFloatAsState(tilt.second, spring(stiffness = 300f), label = "y")

        // Angle in degrees (approximate)
        val angleX = Math.toDegrees(kotlin.math.asin(smoothX.toDouble())).toFloat()
        val angleY = Math.toDegrees(kotlin.math.asin(smoothY.toDouble())).toFloat()
        val isLevel = hypot(angleX, angleY) < 1f

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(300.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerRadius = size.minDimension / 2
                val bubbleRadius = outerRadius * 0.15f
                val maxTravel = outerRadius - bubbleRadius

                // Outer circle and crosshair
                drawCircle(Color.Gray, outerRadius, center, style = Stroke(4f))
                drawCircle(Color.Gray, outerRadius * 0.3f, center, style = Stroke(2f))
                drawLine(Color.Gray, Offset(center.x - outerRadius, center.y),
                    Offset(center.x + outerRadius, center.y), 2f)
                drawLine(Color.Gray, Offset(center.x, center.y - outerRadius),
                    Offset(center.x, center.y + outerRadius), 2f)

                // Bubble moves OPPOSITE to the tilt direction, like a real bubble.
                // X: tilting right makes accel x negative, so use + smoothX.
                // Y: sign depends on your preference; test on device.
                val bubbleCenter = Offset(
                    center.x + (smoothX * maxTravel),
                    center.y - (smoothY * maxTravel)
                )

                drawCircle(
                    color = if (isLevel) Color(0xFF4CAF50) else Color(0xFFFFC107),
                    radius = bubbleRadius,
                    center = bubbleCenter
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "X: ${angleX.roundToInt()}°   Y: ${angleY.roundToInt()}°",
                style = AppTheme.typography.headline3
            )
            if (isLevel) Text("Level!", color = Color(0xFF4CAF50))
        }
    }
}