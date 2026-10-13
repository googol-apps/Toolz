package com.zs.toolz.level

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zs.compose.theme.AppTheme
import com.zs.toolz.common.Res
import com.zs.toolz.common.orbitron
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.graphics.lerp as lerpColor

// ───────────────────────────────────────────────────────────────
// 📐 Bubble Level Reticle Constants
// These values define geometry, spacing, and colors for the UI.
// Organized by purpose: math helpers, ruler layout, bezel/dish look,
// and bubble state colors.
// ───────────────────────────────────────────────────────────────
//
// Math helpers
private const val DegToRad = 0.017453292f // Degrees → radians conversion factor
//
// Bubble geometry
private const val BubbleRadiusFraction = 0.15f // Bubble radius as fraction of dish radius
//
// Ruler layout
private const val RulerSpacingFraction = 0.2f  // Distance per degree along ruler axis (fraction of dish radius)
private const val RulerHalfSpan = 2.5f         // Degrees shown on each side of the reticle center
private const val RulerNumberDistanceFraction = 0.3f // Distance of ruler numbers from crosshair (fraction of radius)
private const val RularFarNumberScaleFraction = 0.7f // fontsize of far number around main number (fraction of actual fontsize)
//
// Bezel & dish appearance
private val BezelColors = listOf(Color(0xFFB0BEC5), Color(0xFF455A64)) // Metallic bezel gradient: lit → shaded
private val DishColors = listOf(Color(0xFFC8E6C9), Color(0xFF66BB6A))  // Liquid gradient: light green → deep green
//
// Bubble state colors
private val BubbleRed = Color(0xFFE53935)    // Outside halfway ring
private val BubbleYellow = Color(0xFFFFC107) // Inside halfway ring
private val BubbleGreen = Color(0xFF00C853)  // Inside target ring


// ---------------------------------------------------------------------------
// Ruler: line, tick marks and numbers
// ---------------------------------------------------------------------------

/**
 * Draws one ruler along a crosshair line, like a tape sliding under a pointer:
 * the line, the ticks, and the numbers.
 *
 * - [value] is the reading in degrees. It sits exactly at the middle of the crosshair;
 *   degree [value] + 1 is one [RulerSpacingFraction] of the radius further along
 *   (right, or down).
 * - Ticks: one every tenth of a degree, longer every half degree and longest at
 *   each whole degree. They fade out toward the ends of the ruler.
 * - Numbers: the whole degrees around the reading, beside the ticks (below the
 *   horizontal line, right of the vertical line). A number grows and turns bold as it
 *   slides to the middle, and shrinks and thins as it slides away. Farther out it
 *   fades, so there are about three numbers at a time.
 */
private fun DrawScope.drawRuler(
    textMeasurer: TextMeasurer,
    style: TextStyle,
    value: Float,
    vertical: Boolean,
    radius: Float,
    lineWidth: Float,
    color: Color
) {
    // "along" runs down the ruler, "across" points to the side with ticks and numbers.
    val along = if (vertical) Offset(0f, 1f) else Offset(1f, 0f)
    val across = if (vertical) Offset(1f, 0f) else Offset(0f, 1f)
    val spacing = radius * RulerSpacingFraction // pixels per degree

    // Line: the crosshair itself, edge to edge of the liquid.
    drawLine(
        color = color,
        start = center - along * radius,
        end = center + along * radius,
        strokeWidth = lineWidth
    )

    // Ticks, counted in tenths of a degree so the whole and half marks are exact.
    val firstTick = floor((value - RulerHalfSpan) * 10f).toInt()
    val lastTick = ceil((value + RulerHalfSpan) * 10f).toInt()
    for (tick in firstTick..lastTick) {
        val degrees = tick / 10f
        val length = radius * when {
            tick % 10 == 0 -> 0.14f // major tick
            tick % 5 == 0 -> 0.08f // medium tick
            else -> 0.06f // minor tick
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
    val fontSize = 28.sp
    val boldStle =style.copy(fontWeight = FontWeight.Bold)
    val fontSizePx = fontSize.toPx()
    val smallScale = fontSizePx * RularFarNumberScaleFraction / fontSizePx

    val nearest = value.roundToInt()
    for (degrees in nearest - 2..nearest + 2) {
        val distance = abs(degrees - value)

        // Visible: fully up to 1.25 degrees away, gone at 1.75.
        val visible = ((1.75f - distance) / 0.5f).coerceIn(0f, 1f)
        if (visible <= 0f) continue

        // Closeness to the middle: 1 at the middle, 0 one degree away or more. It
        // drives the size and the boldness.
        val near = 1f - min(distance, 1f)
        val scale = smallScale + (1f - smallScale) * near

        val anchor = center +
                along * ((degrees - value) * spacing) +
                across * (radius * RulerNumberDistanceFraction)

        val normal = textMeasurer.measure("$degrees", style)
        val bold = textMeasurer.measure("$degrees", boldStle)
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

// ---------------------------------------------------------------------------
// Bubble
// ---------------------------------------------------------------------------

/**
 * The bubble, drawn bottom to top: shadow, body, refraction, wall, shine.
 * [shift] is the offset scaled so 1 is the wall, [travel] is its length, and
 * [tint] is the bubble color (red, yellow or green).
 */
private fun DrawScope.drawBubble(
    bubbleRadius: Float,
    bubbleCenter: Offset,
    shift: Offset,
    travel: Float,
    tint: Color
) {
    val rim = lerpColor(tint, Color.Black, 0.35f)

    // Shadow: an air bubble is see-through, so it only casts a faint soft shadow
    // down and to the right. It slides out, grows and fades near the rim.
    val shadowRadius = bubbleRadius * (1f + 0.2f * travel)
    val shadowCenter = bubbleCenter +
            Offset(bubbleRadius * 0.12f, bubbleRadius * 0.18f) +
            shift * (bubbleRadius * 0.3f)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.5f to Color.Black.copy(alpha = 0.16f - 0.08f * travel),
                1f to Color.Transparent
            ),
            center = shadowCenter,
            radius = shadowRadius
        ),
        radius = shadowRadius,
        center = shadowCenter
    )

    // Body: mostly clear in the middle, a light stain of color around it, and a
    // denser band at the edge.
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = 0.28f),
                0.55f to tint.copy(alpha = 0.18f),
                0.85f to tint.copy(alpha = 0.45f),
            ),
            center = bubbleCenter,
            radius = bubbleRadius
        ),
        radius = bubbleRadius,
        center = bubbleCenter
    )

    // Refraction: a soft bright crescent on the lower right inside the wall.
    val crescentRadius = bubbleRadius * 0.78f
    drawArc(
        color = Color.White.copy(alpha = 0.30f),
        startAngle = 15f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(bubbleCenter.x - crescentRadius, bubbleCenter.y - crescentRadius),
        size = Size(crescentRadius * 2f, crescentRadius * 2f),
        style = Stroke(width = bubbleRadius * 0.12f, cap = StrokeCap.Round)
    )

    // Wall: a thin darker outline, plus a faint light ring just inside it.
    drawCircle(
        color = rim.copy(alpha = 0.7f),
        radius = bubbleRadius,
        center = bubbleCenter,
        style = Stroke(1.5.dp.toPx())
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.25f),
        radius = bubbleRadius - 1.5.dp.toPx(),
        center = bubbleCenter,
        style = Stroke(1.dp.toPx())
    )

    // Shine: starts toward the upper left of the bubble and slides toward the dish
    // center as the bubble moves away from it. Plus a tiny dot on the opposite side.
    val shineCenter = bubbleCenter +
            Offset(-bubbleRadius * 0.3f, -bubbleRadius * 0.35f) -
            shift * (bubbleRadius * 0.2f)
    val shineWidth = bubbleRadius * 0.45f
    val shineHeight = bubbleRadius * 0.24f
    drawOval(
        color = Color.White.copy(alpha = 0.75f),
        topLeft = Offset(shineCenter.x - shineWidth / 2f, shineCenter.y - shineHeight / 2f),
        size = Size(shineWidth, shineHeight)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.4f),
        radius = bubbleRadius * 0.07f,
        center = bubbleCenter + Offset(bubbleRadius * 0.42f, bubbleRadius * 0.5f)
    )
}

// ---------------------------------------------------------------------------
// Dish
// ---------------------------------------------------------------------------

/** Bezel: a metal ring, lit from the upper left. */
private fun DrawScope.drawBezel(dishRadius: Float) {
    drawCircle(
        brush = Brush.linearGradient(
            colors = BezelColors,
            start = Offset(center.x - dishRadius, center.y - dishRadius),
            end = Offset(center.x + dishRadius, center.y + dishRadius)
        ),
        radius = dishRadius
    )
}

/** Liquid: lighter in the middle and deeper green toward the edge. */
private fun DrawScope.drawDish(liquidRadius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = DishColors,
            center = center,
            radius = liquidRadius
        ),
        radius = liquidRadius
    )
    // depth of te liquid
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
}

/** Glass glint: a faint curved highlight on the upper left of the dome, drawn last. */
private fun DrawScope.drawGlass(liquidRadius: Float, dishRadius: Float) {
    val glintRadius = liquidRadius * 0.88f
    drawArc(
        color = Color.White.copy(alpha = 0.35f),
        startAngle = 205f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(center.x - glintRadius, center.y - glintRadius),
        size = Size(glintRadius * 2f, glintRadius * 2f),
        style = Stroke(width = dishRadius * 0.05f, cap = StrokeCap.Round)
    )
}

// ---------------------------------------------------------------------------
// BubbleLevel
// ---------------------------------------------------------------------------

/**
 * A bull's-eye bubble level for a phone lying flat: a glass dish of green liquid in
 * a metal bezel, with a glossy bubble that moves with the tilt.
 *
 * The bubble is red outside the CautionRing, yellow inside it, and green inside the
 * BullsEye. The crosshair, BullsEye and rulers are always black.
 *
 * Along each crosshair line runs a ruler that shows the tilt in degrees. It slides
 * under the middle of the crosshair as you tilt. Horizontal: right is positive.
 * Vertical: down is positive.
 *
 * It is meant for [Position.FLAT]. It draws exactly what it is given: there is no
 * animation here, so the jitter has to be filtered at the sensor (see smoothingTimeMs
 * in rememberTilt). An unspecified tilt is drawn as zero (level).
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
    style: TextStyle = AppTheme.typography.title1.copy(fontFamily = Res.font.orbitron)
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = 32)
    Canvas(modifier = modifier) {
        val t = tilt().takeOrElse { Tilt.Zero }

        // Sizes: everything follows from the radius of the dish.
        val radiusPx = size.minDimension / 2f
        val dishRadiusPx = radiusPx * (1f - 0.04f)
        val bubbleRadiusPx = dishRadiusPx * BubbleRadiusFraction
        val targetRadiusPx = bubbleRadiusPx * 1.10f
        val lineWidthPx = 2.dp.toPx()

        // Wall: the farthest the bubble's center may be from the middle.
        val boundaryRadiusPx = dishRadiusPx - bubbleRadiusPx - lineWidthPx
        val halfwayRingRadiusPx = boundaryRadiusPx * 0.5f

        // Pull: how strongly gravity pulls toward the right edge (pullX) and the top
        // edge (pullY), each from -1 to 1.
        //  - cos(pitch) removes the roll exaggeration when the phone is tipped forward/back.
        //  - sin(roll) makes face-down read the same as face-up.
        val pullX = cos(t.pitch * DegToRad) * sin(t.roll * DegToRad)
        val pullY = sin(t.pitch * DegToRad)

        // Offset of the bubble from the dish middle, in pixels, pulled back to the wall
        // along the same direction if it would go past it.
        val fraction = Offset(0.5f + pullX / 2f, 0.5f + pullY / 2f)
        var offset = (fraction - Offset(0.5f, 0.5f)) * (2f * boundaryRadiusPx)
        val distance = offset.getDistance()
        if (distance > boundaryRadiusPx) offset *= boundaryRadiusPx / distance

        // Shift: offset scaled so that 1 is the wall. Travel is its length (0..1).
        // Shine and shadow use them.
        val shift = offset / boundaryRadiusPx
        val travel = shift.getDistance()

        drawBezel(radiusPx)
        drawDish(dishRadiusPx)

        // halfway circle
        drawCircle(
            color = Color.Black.copy(alpha = 0.35f),
            radius = halfwayRingRadiusPx,
            style = Stroke(lineWidthPx)
        )

        // Each ruler draws its own crosshair line, tick marks and numbers.
        drawRuler(
            textMeasurer = textMeasurer,
            value = asin(pullX.coerceIn(-1f, 1f)) / DegToRad,
            vertical = false,
            radius = dishRadiusPx,
            lineWidth = lineWidthPx,
            color = Color.Black,
            style = style
        )
        drawRuler(
            textMeasurer = textMeasurer,
            value = asin(pullY.coerceIn(-1f, 1f)) / DegToRad,
            vertical = true,
            radius = dishRadiusPx,
            lineWidth = lineWidthPx,
            color = Color.Black,
            style = style
        )

        // Target
        drawCircle(
            color = Color.Black,
            radius = targetRadiusPx,
            style = Stroke(lineWidthPx)
        )

        // Tint: red outside the CautionRing, yellow inside it, green inside the BullsEye.
        val tint =  when {
            distance <= targetRadiusPx -> BubbleGreen
            distance <= halfwayRingRadiusPx -> BubbleYellow
            else -> BubbleRed
        }
        drawBubble(bubbleRadiusPx, center + offset, shift, travel, tint)
        drawGlass(dishRadiusPx, radiusPx)
    }
}
