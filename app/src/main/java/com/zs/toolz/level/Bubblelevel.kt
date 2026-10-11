package com.zs.toolz.level

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * A bull's-eye bubble level for a phone lying flat: a glass dish of green liquid in
 * a metal bezel, with a glossy bubble that moves with the tilt. The bubble turns from
 * red through yellow to green, and the crosshair lights up, as it nears the center.
 * Its shine and shadow shift with its position on the dish.
 *
 * Uses [Tilt.roll] for the horizontal axis and [Tilt.pitch] for the vertical axis,
 * so it is meant for [Position.LANDING]. Pass a calibrated tilt if you calibrate.
 * The bubble glides to each new reading instead of jumping. If [tilt] is
 * unspecified, the bubble is hidden and the dish is dimmed.
 *
 * ```
 * val raw by rememberTilt()
 * BubbleLevel(tilt = Tilt(raw), modifier = Modifier.fillMaxWidth().padding(24.dp))
 * ```
 */
@Composable
fun BubbleLevel(
    tilt: Tilt,
    modifier: Modifier = Modifier,
) {
    // A partly invalid tilt (NaN or infinity) has no meaningful position, so treat
    // it like no bubble. The animation targets must be finite or they would break.
    val hasBubble = tilt.isSpecified && tilt.pitch.isFinite() && tilt.roll.isFinite()

    // Smoothing: the raw angles jitter with sensor noise. Animating toward each new
    // reading with a critically damped spring acts as a low-pass filter and gives the
    // bubble a liquid feel. The animated values are read only inside the draw block,
    // so each animation frame redraws without recomposing.
    val smoothRoll = animateFloatAsState(if (hasBubble) tilt.roll else 0f, BubbleSpring, label = "roll")
    val smoothPitch = animateFloatAsState(if (hasBubble) tilt.pitch else 0f, BubbleSpring, label = "pitch")

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val roll = smoothRoll.value
        val pitch = smoothPitch.value

        val outerRadius = size.minDimension / 2f
        val liquidRadius = outerRadius * (1f - BEZEL_RATIO)
        val bubbleRadius = liquidRadius * BUBBLE_RATIO
        val strokeWidth = 2.dp.toPx()

        // How far the bubble's center can travel from the dish center, in pixels, so
        // that the whole bubble stays inside the liquid.
        val travelRadius = liquidRadius - bubbleRadius - strokeWidth

        // Dim the liquid and its marks when there is no reading, so it reads as inactive.
        val fade = if (tilt.isSpecified) 1f else 0.4f

        // Closeness to level, from 0 to 1, measured on the smoothed angles so every
        // color change below is smooth too:
        //   0 at NEAR_ANGLE degrees off level or more (red bubble, dark reticle)
        //   1 at LEVEL_ANGLE degrees off level or less (green bubble, white reticle)
        // Reaching full closeness at LEVEL_ANGLE, not at exactly 0, keeps it reachable
        // despite sensor noise.
        val offLevel = hypot(roll, pitch)
        val closeness = ((NEAR_ANGLE - offLevel) / (NEAR_ANGLE - LEVEL_ANGLE)).coerceIn(0f, 1f)
        val reticle = lerpColor(ReticleFar, ReticleNear, closeness)

        // Bezel: a metal ring, lit from the upper left.
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(BezelLight, BezelDark),
                start = Offset(center.x - outerRadius, center.y - outerRadius),
                end = Offset(center.x + outerRadius, center.y + outerRadius)
            ),
            radius = outerRadius
        )

        // Liquid: lighter in the middle and deeper green toward the edge.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(LiquidCenter, LiquidEdge),
                center = center,
                radius = liquidRadius
            ),
            radius = liquidRadius,
            alpha = fade
        )

        // Depth: a soft shadow just inside the rim, as on a real domed vial.
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.75f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.3f)
                ),
                center = center,
                radius = liquidRadius
            ),
            radius = liquidRadius,
            alpha = fade
        )

        // Half-way ring, at half the bubble's travel (half of MAX_ANGLE): a fixed
        // reference for how far off you are.
        drawCircle(
            color = LiquidMark.copy(alpha = 0.35f * fade),
            radius = travelRadius / 2f,
            style = Stroke(strokeWidth)
        )

        // Crosshair and center target, in the reticle color. The bubble should sit
        // inside the target ring when level.
        drawLine(
            color = reticle,
            start = Offset(center.x - liquidRadius, center.y),
            end = Offset(center.x + liquidRadius, center.y),
            strokeWidth = strokeWidth,
            alpha = fade
        )
        drawLine(
            color = reticle,
            start = Offset(center.x, center.y - liquidRadius),
            end = Offset(center.x, center.y + liquidRadius),
            strokeWidth = strokeWidth,
            alpha = fade
        )
        drawCircle(
            color = reticle,
            radius = bubbleRadius * 1.1f,
            style = Stroke(strokeWidth),
            alpha = fade
        )

        if (hasBubble) {
            // The bubble moves toward the higher side, like a real spirit level:
            // right edge up (roll > 0) moves it right, top edge up (pitch < 0) moves it up.
            // On the canvas y grows downward, so pitch maps to y without a sign change.
            // If the bubble moves the wrong way on your device, flip the sign of that axis.
            //
            // Position is kept as a fraction of the bubble's travel: (0, 0) is the dish
            // center and a length of 1 is the edge. Degrees are converted to that
            // fraction once, using MAX_ANGLE, and the fraction is kept within the unit
            // circle. Only the last step turns it into pixels. Shine and shadow use the
            // same fraction.
            val unclamped = Offset(roll, pitch) / MAX_ANGLE
            val length = unclamped.getDistance()
            val shift = if (length > 1f) unclamped / length else unclamped
            val travel = shift.getDistance()
            val bubbleCenter = center + shift * travelRadius

            // Shadow: sits down and to the right of the bubble. As the bubble climbs
            // toward the rim the shadow slides further out, grows, softens and fades;
            // at the center it is tight and darker. It is drawn as a radial gradient,
            // so its edge is soft.
            val shadowRadius = bubbleRadius * (1f + 0.25f * travel)
            val shadowCenter = bubbleCenter +
                    Offset(bubbleRadius * 0.12f, bubbleRadius * 0.18f) +
                    shift * (bubbleRadius * 0.3f)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.5f to Color.Black.copy(alpha = 0.3f - 0.12f * travel),
                        1f to Color.Transparent
                    ),
                    center = shadowCenter,
                    radius = shadowRadius
                ),
                radius = shadowRadius,
                center = shadowCenter
            )

            // Shine: starts toward the upper left of the bubble and slides toward the
            // dish center as the bubble moves away from it, as a reflection on a curved
            // surface would. The shift is kept small so the highlight stays inside the
            // bubble. The body gradient's light spot follows the same point.
            val shineCenter = bubbleCenter +
                    Offset(-bubbleRadius * 0.2f, -bubbleRadius * 0.25f) -
                    shift * (bubbleRadius * 0.3f)

            // Body: red when far, yellow in between, green when level. The light and
            // dark tones are derived from the main color, so the glossy look holds at
            // every stage.
            val body = bubbleColor(closeness)
            val light = lerpColor(body, Color.White, 0.45f)
            val dark = lerpColor(body, Color.Black, 0.35f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(light, body, dark),
                    center = shineCenter,
                    radius = bubbleRadius * 1.5f
                ),
                radius = bubbleRadius,
                center = bubbleCenter
            )

            // Thin outline, so a green bubble stays distinct from the green liquid.
            drawCircle(
                color = dark.copy(alpha = 0.6f),
                radius = bubbleRadius,
                center = bubbleCenter,
                style = Stroke(1.dp.toPx())
            )

            // White shine highlight.
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = bubbleRadius * 0.25f,
                center = shineCenter
            )
        }

        // Glass glint: a faint curved highlight on the upper left of the dome, drawn
        // last so it sits over the bubble. Angles are in degrees clockwise from 3
        // o'clock, so 200..260 is the upper left.
        val glintRadius = liquidRadius * 0.88f
        drawArc(
            color = Color.White.copy(alpha = 0.35f * fade),
            startAngle = 200f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(center.x - glintRadius, center.y - glintRadius),
            size = Size(glintRadius * 2f, glintRadius * 2f),
            style = Stroke(width = outerRadius * 0.05f, cap = StrokeCap.Round)
        )
    }
}

/** Red when [closeness] is 0, yellow at 0.5, green at 1. */
private fun bubbleColor(closeness: Float): Color =
    if (closeness < 0.5f) {
        lerpColor(BubbleRed, BubbleYellow, closeness * 2f)
    } else {
        lerpColor(BubbleYellow, BubbleGreen, (closeness - 0.5f) * 2f)
    }

// ---------------------------------------------------------------------------
// Behavior and look (file-private)
// ---------------------------------------------------------------------------

/** Tilt in degrees at which the bubble reaches the edge of its travel. */
private const val MAX_ANGLE = 10f

/** Tilt in degrees at or beyond which the bubble is fully red and the reticle dark. */
private const val NEAR_ANGLE = 3f

/** Tilt in degrees at or below which the bubble is fully green and the reticle white. */
private const val LEVEL_ANGLE = 0.5f

/** Bezel thickness as a share of the outer radius. */
private const val BEZEL_RATIO = 0.08f

/** Bubble radius as a share of the liquid radius. */
private const val BUBBLE_RATIO = 0.12f

/**
 * How quickly the bubble follows the tilt. Lower is smoother and slower to react,
 * higher is snappier and shows more jitter. Damping is critical, so it never overshoots.
 */
private const val SMOOTHING_STIFFNESS = 150f

private val BubbleSpring =
    spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = SMOOTHING_STIFFNESS)

private val BezelLight = Color(0xFFB0BEC5)     // metal, lit side
private val BezelDark = Color(0xFF455A64)      // metal, shaded side

private val LiquidCenter = Color(0xFFC8E6C9)   // light green in the middle
private val LiquidEdge = Color(0xFF66BB6A)     // deeper green toward the rim
private val LiquidMark = Color(0xFF1B5E20)     // half-way ring

private val ReticleFar = Color(0xFF1B5E20).copy(alpha = 0.55f) // faint dark green
private val ReticleNear = Color.White                          // lit when centered

private val BubbleRed = Color(0xFFE53935)      // far from level
private val BubbleYellow = Color(0xFFFFC107)   // getting close
private val BubbleGreen = Color(0xFF00C853)    // level