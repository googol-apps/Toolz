package com.zs.toolz.level

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.RedViolet
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
//
// Coordinates: a "fraction" is an Offset whose x and y run from -1 to 1,
// where (0, 0) is the centre of the DrawScope and 1 is the dish radius.
// x: right is positive, y: down is positive.
// ───────────────────────────────────────────────────────────────
//
// Math helpers
private const val DegToRad = 0.017453292f // Degrees → radians conversion factor

//
// Dish
//
// Bubble geometry (all fractions of dish radius)
private const val BubbleRadiusFraction = 0.15f
private const val TargetRadiusFraction = BubbleRadiusFraction * 1.10f
private const val HalfwayRingFraction = 0.5f
private const val MaxBubbleFraction =
    1f - BubbleRadiusFraction // farthest the bubble centre may be from the middle

//
// Ruler layout
private const val RulerSpacingFraction =
    0.2f  // Distance per degree along ruler axis (fraction of dish radius)
private const val RulerHalfSpan = 2.5f         // Degrees shown on each side of the reticle center
private const val RulerNumberDistanceFraction =
    0.3f // Distance of ruler numbers from crosshair (fraction of radius)
private const val RularFarNumberScaleFraction =
    0.7f // Size of the far numbers relative to the main number

private val CrossHairLineWidth = 2.dp

// ---------------------------------------------------------------------------
// Dish
// ---------------------------------------------------------------------------
/** Liquid: lighter in the middle and deeper green toward the edge. */
private fun DrawScope.drawDish(tint: Color, radius: Float) {
    // Wall: 1dp solid tint at the very edge, over a faint tinted body that is
    // mostly clear in the middle
    val wall = 1.dp
    val wallPct = (wall.toPx() / radius).coerceAtMost(0.1f)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to Color.Transparent,
                0.70f to tint.copy(alpha = 0.08f),
                0.85f to tint.copy(alpha = 0.11f),
                1.0f - wallPct to tint.copy(alpha = 0.22f),
                1.0f to tint.copy(0.32f)
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
    )
}

// ---------------------------------------------------------------------------
// Ruler: line, tick marks and numbers
// ---------------------------------------------------------------------------

/**
 * Draws one ruler along a crosshair line, like a tape sliding under a pointer:
 * the ticks and the numbers.
 *
 * - [fraction] is the tilt along this axis, from -1 to 1 (the sine of the angle).
 *   It is converted to degrees here. That reading sits exactly at the middle of the
 *   crosshair; degree + 1 is one [RulerSpacingFraction] of [dishRadius] further along
 *   (right, or down).
 * - Ticks: one every tenth of a degree, longer every half degree and longest at
 *   each whole degree. They fade out toward the ends of the ruler.
 * - Numbers: the whole degrees around the reading, beside the ticks (below the
 *   horizontal line, right of the vertical line). A number grows and turns bold as it
 *   slides to the middle, and shrinks and thins as it slides away. Farther out it
 *   fades, so there are about three numbers at a time.
 */
fun DrawScope.drawRuler(
    textMeasurer: TextMeasurer,
    style: TextStyle,
    fraction: Float,
    vertical: Boolean,
    dishRadius: Float,
    color: Color
) {
    val dishRadius = size.maxDimension / 2f
    val value = asin(fraction.coerceIn(-1f, 1f)) / DegToRad // degrees

    // "along" runs down the ruler, "across" points to the side with ticks and numbers.
    val along = if (vertical) Offset(0f, 1f) else Offset(1f, 0f)
    val across = if (vertical) Offset(1f, 0f) else Offset(0f, 1f)
    val spacing = dishRadius * RulerSpacingFraction // pixels per degree
    val lineWidthPx = CrossHairLineWidth.toPx()

    // Ticks, counted in tenths of a degree so the whole and half marks are exact.
    val firstTick = floor((value - RulerHalfSpan) * 10f).toInt()
    val lastTick = ceil((value + RulerHalfSpan) * 10f).toInt()
    for (tick in firstTick..lastTick) {
        val degrees = tick / 10f
        val length = dishRadius * when {
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
            strokeWidth = lineWidthPx / 2f
        )
    }

    // Numbers. Each one is measured at the big size, in normal and in bold, and
    // then scaled while drawing, so growing and shrinking is smooth and cheap.
    val boldStyle = style.copy(fontWeight = FontWeight.Bold)
    val smallScale = RularFarNumberScaleFraction

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
                across * (dishRadius * RulerNumberDistanceFraction)

        val normal = textMeasurer.measure("$degrees", style)
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

// ---------------------------------------------------------------------------
// Bubble
// ---------------------------------------------------------------------------
/**
 * The bubble, drawn bottom to top: shadow, body, refraction, wall, shine.
 * [fraction] is the bubble centre as a fraction of [dishRadius] from the dish middle
 * (-1..1 on each axis), and [tint] is the bubble color (red, yellow or green).
 */
fun DrawScope.drawBubble(
    tint: Color,
    fraction: Offset,
    dishRadius: Float
) {
    val dishRadius = size.maxDimension / 2f
    val bubbleRadius = dishRadius * BubbleRadiusFraction
    val bubbleCenter = center + fraction * dishRadius
    val rim = lerpColor(tint, Color.Black, 0.35f)

    // Shadow: an air bubble is see-through, so it only casts a faint soft shadow
    // down and to the right. It slides outward as the bubble moves away from the middle.
    val shadowCenter = bubbleCenter +
            Offset(bubbleRadius * 0.12f, bubbleRadius * 0.18f) +
            fraction * (bubbleRadius * 0.3f)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.5f to Color.Black.copy(alpha = 0.14f),
                1f to Color.Transparent
            ),
            center = shadowCenter,
            radius = bubbleRadius * 1.1f
        ),
        radius = bubbleRadius * 1.1f,
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
//    drawCircle(
//        color = rim.copy(alpha = 0.7f),
//        radius = bubbleRadius,
//        center = bubbleCenter,
//        style = Stroke(1.5.dp.toPx())
//    )
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
            fraction * (bubbleRadius * 0.2f)
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

/** The fixed reticle: the halfway ring, the crosshair lines and the BullsEye. */
private fun DrawScope.drawReticle(
    color: Color,
    dishRadius: Float
) {
    val lineWidth = CrossHairLineWidth.toPx()

    // Halfway ring
    drawCircle(
        color = color.copy(alpha = color.alpha * 0.35f),
        radius = dishRadius * HalfwayRingFraction,
        style = Stroke(lineWidth)
    )

    // Crosshair lines, edge to edge of the liquid.
    drawLine(
        color = color,
        start = Offset(center.x - dishRadius, center.y),
        end = Offset(center.x + dishRadius, center.y),
        strokeWidth = lineWidth
    )
    drawLine(
        color = color,
        start = Offset(center.x, center.y - dishRadius),
        end = Offset(center.x, center.y + dishRadius),
        strokeWidth = lineWidth
    )

    // BullsEye
    drawCircle(
        color = color,
        radius = dishRadius * TargetRadiusFraction,
        style = Stroke(lineWidth)
    )
}

/**
 * A circular bubble level drawn on a [Canvas]: a dish with a reticle, a ruler along
 * each axis, and a bubble that drifts toward the low side of the device.
 *
 * Everything is sized from the dish radius, so the level fills the smaller side of
 * its bounds. The bubble stays inside the dish wall, while the rulers show the raw
 * axis values.
 *
 * @param tilt supplies the current pitch and roll in degrees. It is a lambda so
 * sensor updates only redraw instead of recomposing; invalid values fall back to
 * [Tilt.Zero].
 * @param modifier modifier applied to the canvas.
 * @param color accent used for the dish, reticle and rulers.
 * @param style text style for the ruler numbers.
 */
@Composable
fun BubbleLevel(
    tilt: () -> Tilt,
    modifier: Modifier = Modifier,
    color: Color = Color.RedViolet,
    style: TextStyle = AppTheme.typography.title1.copy(fontFamily = Res.font.orbitron)
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = 32)
    Canvas(modifier = modifier.size(250.dp, 250.dp)) {
        val t = tilt().takeOrElse { Tilt.Zero }

        // Everything follows from the radius of the dish.
        val dishRadius = size.minDimension / 2f

        // Gravity pull as a fraction of the dish, -1..1 on each axis, (0, 0) at the middle.
        //  - cos(pitch) removes the roll exaggeration when the phone is tipped forward/back.
        //  - sin(roll) makes face-down read the same as face-up.
        val pull = Offset(
            x = cos(t.pitch * DegToRad) * sin(t.roll * DegToRad),
            y = sin(t.pitch * DegToRad)
        )

        // The bubble fraction: the pull, held inside the wall (the bubble touches the
        // dish edge, it never crosses it).
        val distance = pull.getDistance()
        val fraction =
            if (distance > MaxBubbleFraction) pull * (MaxBubbleFraction / distance) else pull

        drawDish(color, dishRadius)
        drawReticle(color.copy(0.3f), dishRadius)

        // Each ruler draws its own ticks and numbers from the raw axis value.
        drawRuler(
            textMeasurer = textMeasurer,
            style = style,
            fraction = pull.x,
            vertical = false,
            dishRadius = dishRadius,
            color = color
        )
        drawRuler(
            textMeasurer = textMeasurer,
            style = style,
            fraction = pull.y,
            vertical = true,
            dishRadius = dishRadius,
            color = color
        )

        drawBubble(color, fraction, dishRadius)
    }
}


private fun DrawScope.drawTube(color: Color, vertical: Boolean) {
    val thickness = if (vertical) size.width else size.height
    val length = if (vertical) size.height else size.width
    val r = thickness / 2f
    val wallPct = (1.dp.toPx() / r).coerceAtMost(0.1f)

    // Same profile as drawDish (t = 0 center -> 1 edge)
    val profile = arrayOf(
        0.00f to Color.Transparent,
        0.70f to color.copy(alpha = 0.08f),
        0.85f to color.copy(alpha = 0.11f),
        1f - wallPct to color.copy(alpha = 0.22f),
        1.00f to color.copy(alpha = 0.32f),
    )
    val cross = (profile.reversed().map { (0.5f - it.first / 2f) to it.second } +
            profile.drop(1).map { (0.5f + it.first / 2f) to it.second }).toTypedArray()

    // Straight middle: gradient across the tube
    drawRect(
        brush = if (vertical) Brush.horizontalGradient(colorStops = cross, endX = thickness)
        else Brush.verticalGradient(colorStops = cross, endY = thickness),
        topLeft = if (vertical) Offset(0f, r) else Offset(r, 0f),
        size = if (vertical) Size(thickness, length - 2 * r) else Size(length - 2 * r, thickness),
    )

    // Rounded ends: the dish gradient, clipped to the outer half of each circle
    for (along in listOf(r, length - r)) {
        val c = if (vertical) Offset(r, along) else Offset(along, r)
        val outerStart = if (along == r) 0f else along
        val outerEnd = if (along == r) along else length
        clipRect(
            left = if (vertical) 0f else outerStart,
            top = if (vertical) outerStart else 0f,
            right = if (vertical) thickness else outerEnd,
            bottom = if (vertical) outerEnd else thickness,
        ) {
            drawCircle(Brush.radialGradient(colorStops = profile, center = c, radius = r), r, c)
        }
    }
}


@Composable
fun HorizontalBubbleLevel(
    tilt: () -> Tilt,
    modifier: Modifier = Modifier,
    color: Color = Color.RedViolet,
    style: TextStyle = AppTheme.typography.title1.copy(fontFamily = Res.font.orbitron)
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = 32)
    Canvas(modifier = modifier.size( 250.dp,  64.dp)) {
        val t = tilt().takeOrElse { Tilt.Zero }
        // Gravity pull as a fraction of the dish, -1..1 on each axis, (0, 0) at the middle.
        //  - cos(pitch) removes the roll exaggeration when the phone is tipped forward/back.
        //  - sin(roll) makes face-down read the same as face-up.
        val pull = Offset(
            x = cos(t.pitch * DegToRad) * sin(t.roll * DegToRad),
            y = sin(t.pitch * DegToRad)
        )

        // The bubble fraction: the pull, held inside the wall (the bubble touches the
        // dish edge, it never crosses it).
        val distance = pull.getDistance()
        val fraction =
            if (distance > MaxBubbleFraction) pull * (MaxBubbleFraction / distance) else pull

        drawTube(color, false)

        // Each ruler draws its own ticks and numbers from the raw axis value.
        val topInset = 3.dp.toPx() // 0 to sit flush on the edge

        translate(top = - center.y) {
            drawRuler(
                textMeasurer = textMeasurer,
                style = style,
                fraction = pull.x,
                vertical = false,
                dishRadius = size.width/2f,
                color = color
            )
        }

        drawBubble(color, fraction.copy(y = 0f), size.width /2)

    }
}


@Composable
fun VerticalBubbleLevel(
    tilt: () -> Tilt,
    modifier: Modifier = Modifier,
    color: Color = Color.RedViolet,
    style: TextStyle = AppTheme.typography.title1.copy(fontFamily = Res.font.orbitron)
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = 32)
    Canvas(modifier = modifier.size( 64.dp,  250.dp)) {
        val t = tilt().takeOrElse { Tilt.Zero }
        // Gravity pull as a fraction of the dish, -1..1 on each axis, (0, 0) at the middle.
        //  - cos(pitch) removes the roll exaggeration when the phone is tipped forward/back.
        //  - sin(roll) makes face-down read the same as face-up.
        val pull = Offset(
            x = cos(t.pitch * DegToRad) * sin(t.roll * DegToRad),
            y = sin(t.pitch * DegToRad)
        )

        // The bubble fraction: the pull, held inside the wall (the bubble touches the
        // dish edge, it never crosses it).
        val distance = pull.getDistance()
        val fraction =
            if (distance > MaxBubbleFraction) pull * (MaxBubbleFraction / distance) else pull

        drawTube(color, true)

        // Each ruler draws its own ticks and numbers from the raw axis value.
        val topInset = 3.dp.toPx() // 0 to sit flush on the edge

        translate(left = -center.x) {
            drawRuler(
                textMeasurer = textMeasurer,
                style = style,
                fraction = pull.y,
                vertical = true,
                dishRadius = size.height /2f,
                color = color
            )
        }


        drawBubble(color, fraction.copy(x = 0f), size.height /2)

    }
}