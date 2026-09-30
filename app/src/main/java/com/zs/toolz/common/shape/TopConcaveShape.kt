package com.zs.toolz.common.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection as ld

/**
 * A shape that represents a rectangle with a concave top.
 *
 * @param radius the radius of the concave curve at the top of the shape.
 */
class TopConcaveShape(private val radius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: ld, density: Density): Outline {
        val (w, h) = size
        val radiusPx = with(density) { radius.toPx() }

        val path = Path().apply {
            // Start from top left corner with a radius
            moveTo(radiusPx, radiusPx)
            quadraticTo(0f, radiusPx, 0f, 0f)

            // Draw left side
            lineTo(0f, h)
            lineTo(w, h)
            lineTo(w, 0f)

            // Draw top right corner with a radius
            quadraticTo(w, radiusPx, w - radiusPx, radiusPx)

            // Close the path
            lineTo(radiusPx, radiusPx)
        }
        return Outline.Generic(path)
    }
}