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
 * Basic implementation of [MeasureUnit] using a fixed conversion [factor].
 *
 * Provides metadata (title, symbol, group, icon, description) and
 * simple multiply/divide logic for converting values to and from
 * the converter's base unit.
 *
 * @property id Stable identifier for this unit (e.g., "Length_Centimeter").
 * @property factor Conversion multiplier relative to the base unit.
 * @property title String resource for the full display name (e.g., "Centimeter").
 * @property symbol String resource for the short symbol/abbreviation (e.g., "cm").
 * @property group String resource for the category/system this unit belongs to.
 * @property icon Optional drawable resource for an icon (default: none).
 * @property description Optional string resource for a description (default: none).
 */
@Stable
class SimpleMeasureUnit(
    override val id: String,
    @StringRes val group: Int,
    @StringRes val title: Int,
    @StringRes val symbol: Int,
    val factor: UnifiedReal,
) : MeasureUnit {
    override fun toBase(value: UnifiedReal): UnifiedReal = value.multiply(factor)
    override fun toUnit(value: UnifiedReal): UnifiedReal = value.divide(factor)
}


private fun TextFieldBuffer.handleDigit(digit: Char) {
    val current = asCharSequence()
    when {
        current.toString() == "0" && digit == '0' -> return // avoid "00"
        current.toString() == "0" -> {
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

private fun TextFieldBuffer.handleDot() {
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

private fun TextFieldBuffer.handleSign() {
    if (asCharSequence().startsWith('-')) {
        delete(0, 1)
        selection = TextRange((selection.min - 1).coerceAtLeast(0))
    } else {
        insert(0, "-")
        selection = TextRange(selection.min + 1)
    }
}

private fun TextFieldBuffer.handleBackspace() {
    if (selection.min != selection.max) {
        replace(selection.min, selection.max, "")
    } else if (selection.min > 0) {
        delete(selection.min - 1, selection.min)
    }
    selection = TextRange(selection.min.coerceAtMost(length))
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

    val onKeyPress get() =  { key: Key ->
        value.edit {
            when (key) {
                Key.Zero, Key.NumPad0 -> handleDigit('0')
                Key.One, Key.NumPad1 -> handleDigit('1')
                Key.Two, Key.NumPad2 -> handleDigit('2')
                Key.Three, Key.NumPad3 -> handleDigit('3')
                Key.Four, Key.NumPad4 -> handleDigit('4')
                Key.Five, Key.NumPad5 -> handleDigit('5')
                Key.Six, Key.NumPad6 -> handleDigit('6')
                Key.Seven, Key.NumPad7 -> handleDigit('7')
                Key.Eight, Key.NumPad8 -> handleDigit('8')
                Key.Nine, Key.NumPad9 -> handleDigit('9')
                Key.Period, Key.NumPadDot -> handleDot()
                Key.Minus, Key.NumPadSubtract -> handleSign()
                Key.Backspace -> handleBackspace()
                else -> Unit
            }
        }
    }
}