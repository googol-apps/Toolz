package com.zs.toolz.level

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.zs.compose.foundation.RedViolet
import com.zs.compose.theme.AppTheme
import com.zs.toolz.common.Res
import com.zs.toolz.common.orbitron
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * FILE OVERVIEW
 * -------------
 * Two composables, both pure Canvas drawing:
 *   - SurfaceLevel: round bull's-eye level (both axes).
 *   - TubeLevel:    vial level (one axis, horizontal or vertical).
 *
 * Shared pipeline:
 *   Tilt (pitch/roll in degrees) -> gravity offset (-1..1 per axis)
 *   -> liquid -> (reticle) -> ruler(s) -> bubble       (painted back to front)
 *
 * Tilt is read inside the draw lambda, so sensor updates only redraw;
 * they never trigger recomposition.
 */

// ───────────────────────────── Constants ─────────────────────────────

private const val DegToRad = 0.017453292f // Degrees -> radians

// Bubble radius as a fraction of the draw surface's longest side.
private const val BubbleRadiusFraction = 0.10f

// Default sizes. A tube swaps length/thickness when vertical.
private val DefaultSurfaceSize = 250.dp
private val DefaultTubeLength = 250.dp
private val DefaultTubeThickness = 64.dp

// Ruler geometry.
private const val RulerHalfSpan =
    2f                     // degrees of ticks drawn on each side of center
private val RulerMajorTickLength = 20.dp                 // whole-degree tick
private val RulerMediumTickLength = 0.6 * RulerMajorTickLength // half-degree tick
private val RulerMinorTickLength = 0.4 * RulerMajorTickLength  // tenth-degree tick
private val RulerDegreeSpacing =
    30.dp                   // distance between two whole degrees (10 ticks between)
private val RulerNumberMajorGap = 2.dp                   // gap between a major tick and its number
private val RulerTickStroke = 1.dp

// Ruler numbers: neighbours are drawn at this scale relative to the number at center.
private const val RulerFarNumberScale = 0.7f

// A number is fully visible up to FadeStart degrees away and gone at FadeEnd.
private const val RulerNumberFadeEnd = 1.75f
private const val RulerNumberFadeLength = 0.5f

// Thickness of the solid outline around the liquid.
private val LiquidWall = 1.dp

// ───────────────────────────── Tilt math ─────────────────────────────

/**
 * Converts the provider's tilt into a gravity offset, -1..1 per axis, (0, 0) = level.
 *
 * - `cos(pitch)` removes roll exaggeration when the phone is tipped forward/back.
 * - `sin(roll)` makes face-down read the same as face-up.
 * - Because values are sines, 1.0 corresponds to 90 degrees; [drawRuler] undoes this with `asin`.
 *
 * An unspecified tilt (no accelerometer) is treated as level.
 */
private fun gravityOf(provider: TiltProvider): Offset {
    val (pitch, roll, _) = provider().takeOrElse { Tilt.Zero }
    return Offset(
        x = cos(pitch * DegToRad) * sin(roll * DegToRad),
        y = sin(pitch * DegToRad)
    )
}

// ───────────────────────────── Liquid ─────────────────────────────

/**
 * Draws the dish liquid: a circle fitting the short side of the draw area.
 * No bubble, crosshair or ruler.
 *
 * @param tint color of the liquid.
 */
private fun DrawScope.drawLiquid(tint: Color) {
    val radius = size.minDimension / 2f
    // Wall width as a fraction of the radius (capped so it never dominates small sizes).
    val wallFraction = (LiquidWall.toPx() / radius).coerceAtMost(0.1f)

    // Radial gradient: clear center -> progressively tinted -> solid thin wall at the edge.
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to tint.copy(alpha = 0f),
                0.70f to tint.copy(alpha = 0.08f),
                0.85f to tint.copy(alpha = 0.11f),
                1f - wallFraction to tint.copy(alpha = 0.22f),
                1.00f to tint.copy(alpha = 0.32f),
            ),
            center = center,
            radius = radius
        ),
        radius = radius
    )
}

/**
 * Draws the tube (vial) liquid. The fade runs across the tube's thickness
 * and is constant along its length.
 *
 * @param tint color of the liquid.
 * @param roundness 0 = square corners, 1 = full pill. Corner radius = [roundness] * half the thickness.
 */
private fun DrawScope.drawTubeLiquid(tint: Color, roundness: Float = 1f) {
    val wall = LiquidWall.toPx()
    // The tube's long side decides its direction.
    val vertical = size.width < size.height
    val corner = size.minDimension / 2f * roundness.coerceIn(0f, 1f)

    // Gradient stops across the thickness: edge (dense) -> clear center -> edge (dense).
    val across = arrayOf(
        0.000f to tint.copy(alpha = 0.22f),
        0.075f to tint.copy(alpha = 0.11f),
        0.150f to tint.copy(alpha = 0.08f),
        0.500f to tint.copy(alpha = 0f),
        0.850f to tint.copy(alpha = 0.08f),
        0.925f to tint.copy(alpha = 0.11f),
        1.000f to tint.copy(alpha = 0.22f),
    )

    // A gradient spans the shape's bounds, so run it along the SHORT side:
    // vertical tube -> horizontal gradient, horizontal tube -> vertical gradient.
    drawRoundRect(
        brush = if (vertical) Brush.horizontalGradient(colorStops = across)
        else Brush.verticalGradient(colorStops = across),
        cornerRadius = CornerRadius(corner)
    )

    // Wall: solid outline, inset by half the stroke so it stays inside the bounds.
    drawRoundRect(
        color = tint.copy(alpha = 0.32f),
        topLeft = Offset(wall / 2f, wall / 2f),
        size = Size(size.width - wall, size.height - wall),
        cornerRadius = CornerRadius((corner - wall / 2f).coerceAtLeast(0f)),
        style = Stroke(width = wall)
    )
}

// ───────────────────────────── Ruler ─────────────────────────────

/**
 * Draws a ruler of tick marks and degree numbers that scrolls with [tilt].
 * The current angle always sits at the center; ticks and numbers fade toward the ends.
 *
 * @param tilt gravity along the ruler's axis, -1..1 (0 = level). This is sin(angle), not degrees.
 * @param measurer measures the ruler numbers.
 * @param orientation whether the ruler runs vertically or horizontally.
 * @param nearTextStyle style of the number nearest the current tilt (bold).
 * @param farTextStyle style of the neighbouring numbers. Same font size as [nearTextStyle];
 * far numbers are scaled down while drawing.
 * @param color color of the ruler lines and numbers.
 */
private fun DrawScope.drawRuler(
    tilt: Float,
    measurer: TextMeasurer,
    orientation: Orientation,
    nearTextStyle: TextStyle,
    farTextStyle: TextStyle,
    color: Color,
) {
    val vertical = orientation == Orientation.Vertical

    // Unit vectors: `along` = direction the ruler runs, `across` = direction ticks point.
    val along = if (vertical) Offset(0f, 1f) else Offset(1f, 0f)
    val across = if (vertical) Offset(1f, 0f) else Offset(0f, 1f)

    // Convert dp constants to pixels once.
    val spacingPx = RulerDegreeSpacing.toPx()
    val majorPx = RulerMajorTickLength.toPx()
    val mediumPx = RulerMediumTickLength.toPx()
    val minorPx = RulerMinorTickLength.toPx()
    val strokePx = RulerTickStroke.toPx()
    val numberOffsetPx = majorPx + RulerNumberMajorGap.toPx()

    // Gravity is sin(angle), so asin recovers the angle in degrees.
    val value = asin(tilt.coerceIn(-1f, 1f)) / DegToRad

    // ── Ticks ──
    // Counted as integer tenths of a degree so whole/half marks are exact (no float drift).
    val firstTick = floor((value - RulerHalfSpan) * 10f).toInt()
    val lastTick = ceil((value + RulerHalfSpan) * 10f).toInt()
    for (tick in firstTick..lastTick) {
        val degrees = tick / 10f
        // Whole degree = major, half degree = medium, otherwise minor.
        val length = when {
            tick % 10 == 0 -> majorPx
            tick % 5 == 0 -> mediumPx
            else -> minorPx
        }
        // Opacity: 1 at the ruler center, 0 at its ends.
        val strength = (1f - abs(degrees - value) / RulerHalfSpan).coerceIn(0f, 1f)
        // Position: shifted from center by the distance to the current value, so the ruler scrolls.
        val start = center + along * ((degrees - value) * spacingPx)
        drawLine(
            color = color.copy(alpha = color.alpha * strength),
            start = start,
            end = start + across * length,
            strokeWidth = strokePx
        )
    }

    // ── Numbers ──
    // Only whole degrees near the current value are visible (typically far, near, far).
    val nearest = value.roundToInt()
    for (degrees in nearest - 2..nearest + 2) {
        val distance = abs(degrees - value)

        // Fully visible while close, fading out toward RulerNumberFadeEnd.
        val visible = ((RulerNumberFadeEnd - distance) / RulerNumberFadeLength).coerceIn(0f, 1f)
        if (visible <= 0f) continue

        // 1 when the number is at the center, 0 when 1 degree or more away.
        val nearness = 1f - min(distance, 1f)
        // Size interpolates between the far scale and full size.
        val numberScale = RulerFarNumberScale + (1f - RulerFarNumberScale) * nearness

        // Where the number's near edge sits (beyond the major tick plus a gap).
        val anchor = center +
                along * ((degrees - value) * spacingPx) +
                across * numberOffsetPx

        val label = degrees.toString()
        val regular = measurer.measure(label, farTextStyle)
        val alpha = color.alpha * visible * (0.65f + 0.35f * nearness)

        // Scaling around the anchor keeps the near edge fixed while the text grows/shrinks.
        scale(scale = numberScale, pivot = anchor) {
            // Near edge sits on the anchor so the gap to the ticks never depends on text size.
            fun topLeftFor(layout: TextLayoutResult): Offset =
                if (vertical) {
                    Offset(
                        anchor.x,
                        anchor.y - layout.size.height / 2f
                    )   // left edge on anchor, centered vertically
                } else {
                    Offset(
                        anchor.x - layout.size.width / 2f,
                        anchor.y
                    )    // top edge on anchor, centered horizontally
                }

            // Regular-weight text is always drawn; the bold text fades in on top as it nears center.
            drawText(
                textLayoutResult = regular,
                color = color.copy(alpha = alpha),
                topLeft = topLeftFor(regular)
            )
            if (nearness > 0f) {
                val bold = measurer.measure(label, nearTextStyle)
                drawText(
                    textLayoutResult = bold,
                    color = color.copy(alpha = alpha * nearness),
                    topLeft = topLeftFor(bold)
                )
            }
        }
    }
}

// ───────────────────────────── Bubble ─────────────────────────────

/**
 * Draws only the bubble (shadow, body, refraction, wall, shine) inside the current draw area.
 *
 * Works for any shape: the travel range comes from the draw area, so in a tube the thin
 * axis has no room and the bubble slides along the long axis only.
 *
 * @param tint color of the bubble.
 * @param tilt gravity offset, -1..1 per axis. (0, 0) = center of the draw area.
 */
fun DrawScope.drawBubble(tint: Color, tilt: Offset) {
    val halfWidth = size.width / 2f
    val halfHeight = size.height / 2f

    // Radius follows the longer side, capped at the thin side so it never outgrows a tube.
    val bubbleRadius = min(
        max(size.width, size.height) * BubbleRadiusFraction,
        min(halfWidth, halfHeight)
    )

    // Room the bubble center can travel on each axis before touching the wall.
    val limitX = max(0f, halfWidth - bubbleRadius)
    val limitY = max(0f, halfHeight - bubbleRadius)

    // Clamp to the unit circle (round rim for the dish; a locked axis stays a straight line).
    // Gravity is sin(angle), so mapping it directly onto the travel range puts the bubble
    // on the wall at exactly 90 degrees, not earlier.
    val distance = sqrt(tilt.x * tilt.x + tilt.y * tilt.y)
    val k = if (distance > 1f) 1f / distance else 1f
    // Normalized position (exactly 1 at the wall). Also drives the shadow and shine shifts.
    val unit = Offset(tilt.x * k, tilt.y * k)

    val bubbleCenter = center + Offset(unit.x * limitX, unit.y * limitY)

    // ── Shadow ──
    // Air is see-through, so only a faint soft shadow (down/right). It slides outward
    // as the bubble moves away from the middle.
    val shadowCenter = bubbleCenter +
            Offset(bubbleRadius * 0.12f, bubbleRadius * 0.18f) +
            unit * (bubbleRadius * 0.3f)
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

    // ── Body ──
    // Mostly clear center, light stain of tint, denser band at the edge.
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

    // ── Refraction ──
    // Soft bright crescent on the lower right, inside the wall.
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

    // ── Wall ──
    // Faint light ring just inside the edge.
    drawCircle(
        color = Color.White.copy(alpha = 0.25f),
        radius = bubbleRadius - 1.5.dp.toPx(),
        center = bubbleCenter,
        style = Stroke(1.dp.toPx())
    )

    // ── Shine ──
    // Highlight starts upper-left and slides toward the center as the bubble nears the wall.
    // A tiny dot on the opposite side completes the glass look.
    val shineCenter = bubbleCenter +
            Offset(-bubbleRadius * 0.3f, -bubbleRadius * 0.35f) -
            unit * (bubbleRadius * 0.2f)
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

// ───────────────────────────── Reticle ─────────────────────────────

/**
 * Draws the fixed reticle: halfway ring, crosshair lines and bullseye.
 * Static; it does not move with tilt.
 *
 * @param color color of the ring and crosshair lines.
 */
private fun DrawScope.drawReticle(color: Color) {
    val lineWidth = 1.dp.toPx()
    val dishRadius = size.minDimension / 2f

    // Halfway ring (dimmer than the crosshair).
    drawCircle(
        color = color.copy(alpha = color.alpha * 0.35f),
        radius = dishRadius * 0.5f,
        style = Stroke(lineWidth)
    )

    // Crosshair: horizontal and vertical lines spanning the liquid edge to edge.
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

    // Bullseye: ring slightly larger than the bubble (bubble radius + 0.2px) so a level
    // bubble sits just inside it. Bubble radius = fraction of the side = 2 * dishRadius * fraction.
    drawCircle(
        color = color,
        radius = 2 * dishRadius * BubbleRadiusFraction + 0.2f,
        style = Stroke(lineWidth)
    )
}

// ───────────────────────────── Public composables ─────────────────────────────

/** Supplies the current [Tilt]. Invoked during drawing, so reads don't trigger recomposition. */
typealias TiltProvider = () -> Tilt

/**
 * Bull's-eye bubble level showing tilt on both axes at once. Default size is 250 x 250 dp.
 *
 * @param provider supplies the current [Tilt].
 * @param modifier modifier applied to the level; a size set here overrides the default.
 * @param tint color added to the liquid and bubble.
 * @param contentColor color of the crosshair, ruler lines and ruler text.
 * @param rulerTextStyle text style of the ruler numbers.
 */
@Composable
fun SurfaceLevel(
    provider: TiltProvider,
    modifier: Modifier = Modifier,
    tint: Color = Color.RedViolet,
    contentColor: Color = tint,
    rulerTextStyle: TextStyle = AppTheme.typography.title1.copy(fontFamily = Res.font.orbitron)
) {
    // Bold style for the nearest ruler number; remembered so it isn't rebuilt every recomposition.
    val rulerBoldStyle =
        remember(rulerTextStyle) { rulerTextStyle.copy(fontWeight = FontWeight.Bold) }
    val rulerTextMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.size(DefaultSurfaceSize)) {
        // Read tilt in the draw phase so sensor updates redraw without recomposing.
        val gravity = gravityOf(provider)

        // Paint order, back to front: liquid -> reticle -> rulers -> bubble.
        drawLiquid(tint)
        drawReticle(contentColor)

        // Each ruler gets the gravity of its own axis (horizontal = x, vertical = y).
        drawRuler(
            tilt = gravity.x,
            measurer = rulerTextMeasurer,
            orientation = Orientation.Horizontal,
            nearTextStyle = rulerBoldStyle,
            farTextStyle = rulerTextStyle,
            color = contentColor
        )
        drawRuler(
            tilt = gravity.y,
            measurer = rulerTextMeasurer,
            orientation = Orientation.Vertical,
            nearTextStyle = rulerBoldStyle,
            farTextStyle = rulerTextStyle,
            color = contentColor
        )

        // Drawn last so the bubble sits on top of rulers and reticle.
        // drawBubble clamps the offset to the dish rim itself.
        drawBubble(tint, gravity)
    }
}

/**
 * Tube (vial) bubble level showing tilt on a single axis.
 * Default size is 250 x 64 dp, swapped when vertical.
 *
 * @param provider supplies the current [Tilt].
 * @param modifier modifier applied to the level; a size set here overrides the default.
 * @param orientation [Orientation.Horizontal] (measures roll) or [Orientation.Vertical] (measures pitch).
 * @param tint color added to the liquid and bubble.
 * @param contentColor color of the ruler lines and ruler text.
 * @param rulerTextStyle text style of the ruler numbers.
 */
@Composable
fun TubeLevel(
    provider: TiltProvider,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    tint: Color = Color.RedViolet,
    contentColor: Color = tint,
    rulerTextStyle: TextStyle = AppTheme.typography.title1.copy(fontFamily = Res.font.orbitron)
) {
    // Bold style for the nearest ruler number; remembered so it isn't rebuilt every recomposition.
    val rulerBoldStyle =
        remember(rulerTextStyle) { rulerTextStyle.copy(fontWeight = FontWeight.Bold) }
    val rulerTextMeasurer = rememberTextMeasurer()
    val vertical = orientation == Orientation.Vertical
    // Not named `size`: that would shadow DrawScope.size inside the Canvas lambda.
    val defaultSize = if (vertical) DpSize(DefaultTubeThickness, DefaultTubeLength)
    else DpSize(DefaultTubeLength, DefaultTubeThickness)

    Canvas(modifier = modifier.size(defaultSize)) {
        // Read tilt in the draw phase so sensor updates redraw without recomposing.
        val gravity = gravityOf(provider)

        // A tube measures only along its own axis: x for horizontal, y for vertical.
        val axisGravity = if (vertical) gravity.y else gravity.x

        // Paint order, back to front: liquid -> ruler -> bubble.
        drawTubeLiquid(tint)

        // drawRuler draws through the canvas center with ticks pointing right (vertical) or
        // down (horizontal). Translating back by the center moves the ruler line to the start
        // edge (left for vertical, top for horizontal) so ticks and numbers sit inside the tube.
        translate(left = if (vertical) -center.x else 0f, top = if (vertical) 0f else -center.y) {
            drawRuler(
                tilt = axisGravity,
                measurer = rulerTextMeasurer,
                orientation = orientation,
                nearTextStyle = rulerBoldStyle,
                farTextStyle = rulerTextStyle,
                color = contentColor
            )
        }

        // Drawn last so the bubble sits on top of the ruler. Only the tube's axis is fed in;
        // the other axis is 0 so the bubble stays centered across the thickness.
        drawBubble(tint, if (vertical) Offset(0f, axisGravity) else Offset(axisGravity, 0f))
    }
}