package com.zs.toolz.level

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * How the phone is held, a zero-allocation value class with named constants, `Unspecified`, `isSpecified` and `takeOrElse`.
 *
 * Pick one with `Position.from(tilt)`. It decides which angle the level reads and
 * how the UI is rotated.
 */
@Immutable
@JvmInline
value class Position private constructor(private val id: Int) {

    @Stable
    val isUnspecified: Boolean get() = this == Unspecified

    /** This if specified, otherwise the result of [block]. */
    @Stable inline fun takeOrElse(block: () -> Position): Position = if (!isUnspecified) this else block()

    override fun toString(): String {
        val name = when (this) {
            LANDING -> "LANDING"
            TOP -> "TOP"
            RIGHT -> "RIGHT"
            BOTTOM -> "BOTTOM"
            LEFT -> "LEFT"
            else -> "Unspecified"
        }
        return "Position.$name"
    }

    /**
     * @property LANDING Flat on a surface.
     * @property TOP Upright, top edge up.
     * @property RIGHT On its right edge.
     * @property BOTTOM Upright, upside down.
     * @property LEFT On its left edge.
     * @property Unspecified No position yet, e.g. no tilt reading. Check with [isSpecified].
     */
    companion object {
        val LANDING = Position(0)
        val TOP = Position(1)
        val RIGHT = Position(2)
        val BOTTOM = Position(3)
        val LEFT = Position(4)
        val Unspecified = Position(-1)
    }
}