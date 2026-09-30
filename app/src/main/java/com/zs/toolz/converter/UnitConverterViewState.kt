package com.zs.toolz.converter

import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.runtime.Stable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import com.zs.domain.math.UnifiedReal
import kotlinx.coroutines.flow.Flow


/**
 * Defines a unit of measurement within a [Converter].
 *
 * Each [MeasureUnit] has a stable [id] and provides
 * conversion logic to and from the converter's base unit.
 *
 * @property id Unique identifier for this unit (e.g., "Temperature_Celsius").
 */
@Stable
interface MeasureUnit {
    val id: String

    /**
     * Converts [value], expressed in this unit, to the converter's base unit.
     *
     * @param value Value in this unit
     * @return Equivalent value in the base unit
     */
    fun toBase(value: UnifiedReal): UnifiedReal

    /**
     * Converts [value], expressed in the converter's base unit, to this unit.
     *
     * @param value Value in the base unit
     * @return Equivalent value in this unit
     */
    fun toUnit(value: UnifiedReal): UnifiedReal
}

/**
 * A measure unit defined by a grouping and localized string resources.
 *
 * @property group Resource ID for the unit's category group.
 * @property title Resource ID for the unit's name.
 * @property symbol Resource ID for the unit's symbol.
 */
@Stable
interface SimpleMeasureUnit : MeasureUnit {
    @get:StringRes
    val group: Int

    @get:StringRes
    val title: Int

    /**
     * The display symbol for this unit (e.g., "m", "kg", "°C").
     */
    @get:StringRes
    val symbol: Int
}

/**
 * Factory function to create a [BasicMeasureUnit] that uses a linear conversion factor.
 *
 * The resulting unit converts to the base unit by multiplying by [factor],
 * and converts from the base unit by dividing by [factor].
 *
 * @param id Unique identifier for the unit.
 * @param group String resource ID for the category group.
 * @param title String resource ID for the unit name.
 * @param symbol String resource ID for the unit symbol.
 * @param factor The multiplicative factor relative to the base unit.
 */
@Stable
fun SimpleMeasureUnit(
    id: String,
    @StringRes group: Int,
    @StringRes title: Int,
    @StringRes symbol: Int,
    factor: UnifiedReal
): SimpleMeasureUnit = object : SimpleMeasureUnit {
    override val group: Int = group
    override val title: Int = title
    override val symbol: Int = symbol
    override val id: String = id

    override fun toBase(value: UnifiedReal): UnifiedReal = value.multiply(factor)
    override fun toUnit(value: UnifiedReal): UnifiedReal = value.divide(factor)
}

/**
 * A raw implementation of [MeasureUnit] that uses strings for metadata instead of resource IDs.
 *
 * @property id Unique identifier for this unit.
 * @property group The name of the category group this unit belongs to.
 * @property title The display name of the unit.
 * @property symbol The shorthand symbol for the unit.
 * @property factor The multiplicative factor relative to the base unit.
 */
@Stable
class RawMeasureUnit(
    override val id: String,
    val group: String,
    val title: String,
    val symbol: String,
    private val factor: UnifiedReal
): MeasureUnit {
    override fun toBase(value: UnifiedReal): UnifiedReal = value.multiply(factor)
    override fun toUnit(value: UnifiedReal): UnifiedReal = value.divide(factor)
}

/**
 * Defines the UI state for a unit converter.
 *
 * Holds metadata about the active converter, user input,
 * selected units, and computed results.
 *
 * @property converter Key of the currently selected converter.
 * @property value User-entered input field state.
 * @property from Source measurement unit backed by state.
 * @property target Target measurement unit backed by state.
 * @property result Flow emitting the computed result and its representation in other units.
 */
interface UnitConverterViewState {
    var converter: String
    var value: TextFieldState
    var from: MeasureUnit
    var target: MeasureUnit

    val result: Flow<Pair<UnifiedReal, AnnotatedString>>

    /** Swap the source and target units and recalculate. */
    fun swap()

    /** Copy the formatted result to the system clipboard. */
    fun copy()

    fun Key.toDigit(): Int {
       return when(this){
            Key.NumPad0 -> 0
            Key.NumPad1 -> 1
            Key.NumPad2 -> 2
            Key.NumPad3 -> 3
            Key.NumPad4 -> 4
            Key.NumPad5 -> 5
            Key.NumPad6 -> 6
            Key.NumPad7 -> 7
            Key.NumPad8 -> 8
            Key.NumPad9 -> 9
            else -> error("Invalid key pressed")
        }
    }

    fun onkeyPress(key: Key){
        value.edit {
            when{
                key.keyCode in Key.NumPad0.keyCode..Key.NumPad9.keyCode -> {
                    val current = asCharSequence().toString()
                    val digit = key.toDigit().toChar()

                    when {
                        current == "0" && digit == '0' -> return@edit // avoid "00"
                        current == "0" -> {
                            replace(0, length, digit.toString())
                            selection = TextRange(1)
                        }
                        else -> {
                            val insertAt = selection.min
                            replace(selection.min, selection.max, digit.toString())
                            selection = TextRange(insertAt + 1)
                        }
                    }
                }
                key == Key.Backspace -> {
                    if (selection.min != selection.max) {
                        // Case 1: user has selected a range → clear it
                        replace(selection.min, selection.max, "")
                    } else if (selection.min > 0) {
                        // Case 2: no selection → delete the character before the cursor
                        delete(selection.min - 1, selection.min)
                    }

                    // If buffer is now empty, enforce "0" as fallback
                    if (length == 0) {
                        replace(0, 0, "0")
                        selection = TextRange(1) // place cursor after the inserted 0
                    } else {
                        // Otherwise, keep cursor within bounds
                        selection = TextRange(selection.min.coerceAtMost(length))
                    }
                }
                key == Key.NumPadDot -> {
                    val current = asCharSequence()
                    if ('.' in current) return

                    if (current.isEmpty() || current.toString() == "-") {
                        val prefix = if (current.toString() == "-") "-0." else "0."
                        replace(0, length, prefix)
                        selection = TextRange(length)
                    } else {
                        val insertAt = selection.min
                        replace(selection.min, selection.max, ".")
                        selection = TextRange(insertAt + 1)
                    }
                }
                key == Key.NumPadSubtract -> {
                    if (asCharSequence().startsWith('-')) {
                        delete(0, 1)
                        selection = TextRange((selection.min - 1).coerceAtLeast(0))
                    } else {
                        insert(0, "-")
                        selection = TextRange(selection.min + 1)
                    }
                }
            }
        }
    }
}

