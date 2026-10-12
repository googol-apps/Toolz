package com.zs.toolz.level

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin


// ---------------------------------------------------------------------------
// Behavior and look (file-private)
// ---------------------------------------------------------------------------

/**
 * Distance from the middle, as a share of the way to the wall, at or beyond which
 * the bubble is fully red.
 */
private const val NearTravel = 0.035f

/**
 * Distance from the middle, as a share of the way to the wall, at or below which
 * the bubble is fully green.
 */
private const val LevelTravel = 0.006f

/** Degrees to radians. */
private const val DegToRad = 0.017453292f

/** Bezel thickness as a share of the dish radius. */
private const val BezelFraction = 0.08f

/** Ruler: distance per degree along the line, as a share of the liquid radius. */
private const val RulerSpacingFraction = 0.3f

/** Ruler: how many degrees are shown on each side of the reading. */
private const val RulerHalfSpan = 2.5f

/** Ruler: distance of the numbers from the crosshair line, as a share of the radius. */
private const val NumberDistanceFraction = 0.2f

/** Ruler: text size of the number at the middle (big) and one degree away (small). */
private const val BigNumberFraction = 0.16f
private const val SmallNumberFraction = 0.11f

/** Target ring radius as a multiple of the bubble radius. */
private const val TargetRingFraction = 1.3f

/** Bubble radius as a share of the liquid radius. */
private const val BubbleRadiusFraction = 0.12f

/** The middle of the dish, as fractions. */
private val DishMiddle = Offset(0.5f, 0.5f)

/** Metal bezel gradient: lit side, then shaded side. */
private val BezelColors = listOf(Color(0xFFB0BEC5), Color(0xFF455A64))

/** Liquid gradient: light green in the middle, deeper green toward the rim. */
private val LiquidColors = listOf(Color(0xFFC8E6C9), Color(0xFF66BB6A))

private val LiquidMark = Color(0xFF1B5E20)     // half-way ring

/** Crosshair, target ring and rulers: always black. */
private val RulerColor = Color.Black

private val BubbleRed = Color(0xFFE53935)      // far from level
private val BubbleYellow = Color(0xFFFFC107)   // getting close
private val BubbleGreen = Color(0xFF00C853)    // level

/**
 * Draws a ruler along one crosshair line, like a tape sliding under a pointer.
 *
 * - [value] is the reading in degrees. The ruler is positioned so that this value
 *   sits exactly at the middle of the crosshair; a degree [value] + 1 is one
 *   [RulerSpacingFraction] of the radius further along (right, or down).
 * - Ticks: one every tenth of a degree, longer every half degree and longest at
 *   each whole degree. They fade out toward the ends of the ruler.
 * - Numbers: the whole degrees around the reading, placed beside the ticks (below
 *   the horizontal line, right of the vertical line). Each number's look depends on
 *   how far it is from the middle, so it changes smoothly while it slides:
 *     at the middle: full size and bold; one degree away: smaller and thin.
 *   Farther out it fades away, so there are about three numbers at a time.
 */
private fun DrawScope.drawRuler(
    textMeasurer: TextMeasurer,
    value: Float,
    vertical: Boolean,
    radius: Float,
    lineWidth: Float,
    color: Color
) {
    // Directions: "along" runs down the ruler, "across" points to the side that has
    // the ticks and numbers.
    val along = if (vertical) Offset(0f, 1f) else Offset(1f, 0f)
    val across = if (vertical) Offset(1f, 0f) else Offset(0f, 1f)
    val spacing = radius * RulerSpacingFraction // pixels per degree

    // Ticks, counted in tenths of a degree so the whole and half marks are exact.
    val firstTick = floor((value - RulerHalfSpan) * 10f).toInt()
    val lastTick = ceil((value + RulerHalfSpan) * 10f).toInt()
    for (tick in firstTick..lastTick) {
        val degrees = tick / 10f
        val length = radius * when {
            tick % 10 == 0 -> 0.06f
            tick % 5 == 0 -> 0.04f
            else -> 0.025f
        }
        // 1 at the middle of the ruler, 0 at its ends.
        val strength = (1f - abs(degrees - value) / RulerHalfSpan).coerceIn(0f, 1f)
        val start = center + along * ((degrees - value) * spacing)
        drawLine(
            color = color.copy(alpha = color.alpha * strength),
            start = start,
            end = start + across * length,
            strokeWidth = lineWidth / 2f
        )
    }

    // Numbers. Each one is measured once at the big size, in normal and in bold, and
    // then scaled while drawing, so growing and shrinking is smooth and cheap.
    val bigStyle = TextStyle(fontSize = (radius * BigNumberFraction).toSp())
    val boldStyle = bigStyle.copy(fontWeight = FontWeight.Bold)
    val smallScale = SmallNumberFraction / BigNumberFraction

    val nearest = value.roundToInt()
    for (degrees in nearest - 2..nearest + 2) {
        val distance = abs(degrees - value)

        // Visible: fully up to 1.25 degrees away, gone at 1.75.
        val visible = ((1.75f - distance) / 0.5f).coerceIn(0f, 1f)
        if (visible <= 0f) continue

        // Closeness to the middle: 1 at the middle, 0 one degree away or more. It
        // drives the size and the boldness, so a number changes gradually as it
        // slides from the side to the middle and back.
        val near = (1f - min(distance, 1f))
        val scale = smallScale + (1f - smallScale) * near

        val anchor = center +
                along * ((degrees - value) * spacing) +
                across * (radius * NumberDistanceFraction)

        val normal = textMeasurer.measure("$degrees", bigStyle)
        val bold = textMeasurer.measure("$degrees", boldStyle)
        val tint = color.alpha * visible * (0.65f + 0.35f * near)

        scale(scale = scale, pivot = anchor) {
            // The thin number is always drawn; the bold one fades in over it as the
            // number nears the middle, which makes it look like it thickens.
            drawText(
                textLayoutResult = normal,
                color = color.copy(alpha = tint),
                topLeft = anchor - Offset(normal.size.width / 2f, normal.size.height / 2f)
            )
            drawText(
                textLayoutResult = bold,
                color = color.copy(alpha = tint * near),
                topLeft = anchor - Offset(bold.size.width / 2f, bold.size.height / 2f)
            )
        }
    }
}

/** Red when [closeness] is 0, yellow at 0.5, green at 1. */
private fun bubbleColor(closeness: Float): Color =
    if (closeness < 0.5f) {
        lerpColor(BubbleRed, BubbleYellow, closeness * 2f)
    } else {
        lerpColor(BubbleYellow, BubbleGreen, (closeness - 0.5f) * 2f)
    }

/**
 * A bull's-eye bubble level for a phone lying flat: a glass dish of green liquid in
 * a metal bezel, with a glossy bubble that moves with the tilt. The bubble turns from
 * red through yellow to green as it nears the center. The crosshair, the target ring
 * and the rulers are always black.
 * Its shine and shadow shift with its position on the dish.
 *
 * Along each crosshair line runs a ruler that shows the tilt in degrees: tick marks
 * every tenth of a degree (longer at every half and whole degree) and about three
 * numbers around the reading. The ruler slides under the middle of the crosshair as you
 * tilt, so the middle of the crosshair always points at the exact reading. A number
 * grows and turns bold as it slides toward the middle and shrinks and thins as it slides
 * away, so it looks like one number moving, not one number replacing another.
 * Horizontal: right is positive. Vertical: down is positive.
 *
 * It is meant for [Position.FLAT]. Pass a calibrated tilt if you calibrate. It draws
 * exactly what it is given: there is no animation here, so the jitter has to be
 * filtered at the sensor (see smoothingTimeMs in rememberTilt). An unspecified tilt is
 * drawn as zero (level); handle that case yourself if you want something else.
 *
 * How the bubble is placed, in simple words:
 * - Roll and pitch are turned into how strongly gravity pulls toward the right edge
 *   and toward the top edge, each from -1 to 1 (1 = standing on that edge). That is
 *   then a fraction of the dish (0..1):
 *     0.0 = start (left / top end), 0.5 = center, 1.0 = end (right / bottom end)
 * - A phone lying face down reads the same as face up, so it also shows level.
 * - The fraction becomes an offset from the middle of the dish, in pixels.
 * - If the offset would push the bubble out of the liquid, it is pulled back along
 *   the same direction so it stops at the wall.
 * - The bubble color follows how far the bubble is from the middle, as a
 *   share of the way to the wall (0 = middle, 1 = wall).
 *
 * ```
 * val raw = rememberTilt() // keep the State, do not read it here with `by`
 * BubbleLevel(
 *     tilt = { Tilt(raw.longValue) },
 *     modifier = Modifier.fillMaxWidth().padding(24.dp)
 * )
 * ```
 *
 * [tilt] is a lambda so that the state is read while drawing. Each sensor reading then
 * redraws only the canvas and does not recompose the screen around it.
 */
@Composable
fun BubbleLevel(
    tilt: () -> Tilt,
    modifier: Modifier = Modifier,
) {
    // Measures the ruler numbers. Remembered, so it keeps its cache between frames;
    // the cache is sized for the few numbers (normal and bold) drawn on both rulers.
    val textMeasurer = rememberTextMeasurer(cacheSize = 32)

    Canvas(modifier = modifier.aspectRatio(1f)) {
        // Reading the state here, in the draw block, keeps sensor updates to the draw
        // phase: no recomposition.
        // An unspecified tilt is treated as zero, so nothing here can become NaN.
        val t = tilt().takeOrElse { Tilt.Zero }

        // Sizes: the dish is the biggest circle that fits; the rest follows from it.
        val dishRadius = size.minDimension / 2f
        val liquidRadius = dishRadius * (1f - BezelFraction)
        val bubbleRadius = liquidRadius * BubbleRadiusFraction
        val lineWidth = 2.dp.toPx()

        // Wall: the farthest the bubble's center may be from the middle, so the whole
        // bubble (and its outline) stays inside the liquid.
        val maxOffset = liquidRadius - bubbleRadius - lineWidth

        // Pull: how strongly gravity pulls toward the right edge (pullX) and toward the
        // top edge (pullY), each from -1 to 1 (1 = standing on that edge).
        //  - Roll alone would not do: it is measured across the x-z plane, so it gets
        //    exaggerated whenever the phone is also tipped forward or back. Multiplying
        //    sin(roll) by cos(pitch) removes that.
        //  - sin(roll) is the same for roll 10 and roll 170, so a phone lying face down
        //    reads the same as face up, instead of jumping to the edge at roll 180.
        val pullX = cos(t.pitch * DegToRad) * sin(t.roll * DegToRad)
        val pullY = sin(t.pitch * DegToRad)

        // Fraction: where the pull sits inside the dish, as one Offset (x, y) with
        // 0.0 = start, 0.5 = center, 1.0 = end. The right edge up gives a positive
        // pullX, so the bubble goes right. The top edge up gives a negative pitch and
        // pullY, so the bubble goes up the screen (y grows downward, so no sign change
        // is needed). If the bubble moves the wrong way on your device, flip that axis.
        val fraction = Offset(0.5f + pullX / 2f, 0.5f + pullY / 2f)

        // Offset: how far the bubble is from the middle of the dish, in pixels. If it
        // is too far out, it is shrunk along the same direction to stop at the wall.
        var offset = (fraction - DishMiddle) * (2f * maxOffset)
        val distance = offset.getDistance()
        if (distance > maxOffset) offset *= maxOffset / distance

        // Shift: the same offset scaled so that 1 is the wall. Travel is its length:
        // 0 in the middle, 1 at the wall. Shine, shadow and color use it.
        val shift = offset / maxOffset
        val travel = shift.getDistance()

        // Closeness: from 0 to 1, measured on the filtered tilt so every color change
        // below is smooth too:
        //   0 at NearTravel or farther from the middle (red bubble)
        //   1 at LevelTravel or closer to the middle (green bubble)
        // Reaching full closeness at LevelTravel, not at exactly 0, keeps it reachable
        // despite sensor noise.
        val closeness = ((NearTravel - travel) / (NearTravel - LevelTravel)).coerceIn(0f, 1f)

        // Bezel: a metal ring, lit from the upper left.
        drawCircle(
            brush = Brush.linearGradient(
                colors = BezelColors,
                start = Offset(center.x - dishRadius, center.y - dishRadius),
                end = Offset(center.x + dishRadius, center.y + dishRadius)
            ),
            radius = dishRadius
        )

        // Liquid: lighter in the middle and deeper green toward the edge.
        drawCircle(
            brush = Brush.radialGradient(
                colors = LiquidColors,
                center = center,
                radius = liquidRadius
            ),
            radius = liquidRadius
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
            radius = liquidRadius
        )

        // Half-way ring, at half the bubble's travel: a fixed reference for how far
        // off you are.
        drawCircle(
            color = LiquidMark.copy(alpha = 0.35f),
            radius = maxOffset / 2f,
            style = Stroke(lineWidth)
        )

        // Crosshair and center target, always black. The bubble should sit inside the
        // target ring when level.
        drawLine(
            color = RulerColor,
            start = Offset(center.x - liquidRadius, center.y),
            end = Offset(center.x + liquidRadius, center.y),
            strokeWidth = lineWidth
        )
        drawLine(
            color = RulerColor,
            start = Offset(center.x, center.y - liquidRadius),
            end = Offset(center.x, center.y + liquidRadius),
            strokeWidth = lineWidth
        )
        drawCircle(
            color = RulerColor,
            radius = bubbleRadius * TargetRingFraction,
            style = Stroke(lineWidth)
        )

        // Rulers: the tilt in degrees along each crosshair line. They are drawn before
        // the bubble so the bubble stays on top. The degrees come from the same pull
        // that places the bubble, so the numbers always agree with it.
        drawRuler(
            textMeasurer = textMeasurer,
            value = asin(pullX.coerceIn(-1f, 1f)) / DegToRad,
            vertical = false,
            radius = liquidRadius,
            lineWidth = lineWidth,
            color = RulerColor
        )
        drawRuler(
            textMeasurer = textMeasurer,
            value = asin(pullY.coerceIn(-1f, 1f)) / DegToRad,
            vertical = true,
            radius = liquidRadius,
            lineWidth = lineWidth,
            color = RulerColor
        )

        val bubbleCenter = center + offset

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

        // Glass glint: a faint curved highlight on the upper left of the dome, drawn
        // last so it sits over the bubble. Angles are in degrees clockwise from 3
        // o'clock, so 200..260 is the upper left.
        val glintRadius = liquidRadius * 0.88f
        drawArc(
            color = Color.White.copy(alpha = 0.35f),
            startAngle = 200f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(center.x - glintRadius, center.y - glintRadius),
            size = Size(glintRadius * 2f, glintRadius * 2f),
            style = Stroke(width = dishRadius * 0.05f, cap = StrokeCap.Round)
        )
    }
}
