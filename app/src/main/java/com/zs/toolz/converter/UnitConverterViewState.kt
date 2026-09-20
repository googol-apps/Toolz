package com.zs.toolz.converter

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.AnnotatedString
import com.zs.domain.math.UnifiedReal
import kotlinx.coroutines.flow.Flow

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
}