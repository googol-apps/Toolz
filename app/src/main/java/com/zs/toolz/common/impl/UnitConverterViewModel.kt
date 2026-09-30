package com.zs.toolz.common.impl

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.zs.domain.math.UnifiedReal
import com.zs.preferences.stringPreferenceKey
import com.zs.toolz.converter.MeasureUnit
import com.zs.toolz.converter.SimpleMeasureUnit
import com.zs.toolz.converter.UnitConverter
import com.zs.toolz.converter.UnitConverterViewState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.transform
import java.text.DecimalFormat

// --- Constants ---
private const val TAG = "UnitConverterViewModel"  // log tag
private const val DEBOUNCE_TIMEOUT = 15L         // debounce delay for input typing
private const val DEFAULT_VALUE = "0"             // default fallback value
private const val MAX_ALLOWED_CHARS = 12          // max input length

// --- Preference Keys ---
private val KEY_CONVERTER = stringPreferenceKey("${TAG}_converter")
private val KEY_UNIT_FROM = stringPreferenceKey("${TAG}_unit_from")
private val KEY_UNIT_TO = stringPreferenceKey("${TAG}_unit_to")
private val KEY_VALUE = stringPreferenceKey("${TAG}_converter_value")

class UnitConverterViewModel : KoinViewModel(), UnitConverterViewState {
    // --- Backing State ---
    // Currently selected converter key (persisted in preferences)
    private val _converter = mutableStateOf(
        preferences[KEY_CONVERTER] ?: UnitConverter.KEY_ANGLE_CONVERTER
    )

    // Units available for the active converter
    private var units: List<MeasureUnit> = Units(_converter.value)

    // Source unit (persisted, defaults to first unit of converter)
    private val _fromUnit = mutableStateOf(
        with(preferences) {
            val uuid = get(KEY_UNIT_FROM) ?: units[0].id
            units.find { it.id == uuid }!!
        })

    // Target unit (persisted, defaults to second unit of converter)
    private val _toUnit = mutableStateOf(
        with(preferences) {
            val uuid = get(KEY_UNIT_TO) ?: units[1].id
            units.find { it.id == uuid }!!
        })

    // --- Interface Implementation ---
    override var converter: String
        get() = _converter.value
        set(value) {
            // Update backing state
            _converter.value = value
            // Persist selection
            preferences[KEY_CONVERTER] = value
            // Refresh units for new converter
            units = Units(value)
            // Reset defaults
            from = units[0]
            target = units[1]
        }

    override var from: MeasureUnit
        get() = _fromUnit.value
        set(value) {
            _fromUnit.value = value
            preferences[KEY_UNIT_FROM] = value.id
        }

    override var target: MeasureUnit
        get() = _toUnit.value
        set(value) {
            _toUnit.value = value
            preferences[KEY_UNIT_TO] = value.id
        }

    // User input field state (persisted)
    override var value: TextFieldState = TextFieldState(
        preferences[KEY_VALUE] ?: DEFAULT_VALUE
    )

    // Swap source and target units
    override fun swap() {
        TODO("Not yet implemented")
    }

    // Copy result to clipboard
    override fun copy() {
        TODO("Not yet implemented")
    }

    /**
     * The method to convert [value]from -> to
     */
    private fun convert(from: MeasureUnit, to: MeasureUnit, value: UnifiedReal): UnifiedReal {
        val inBase = from.toBase(value)
        return to.toUnit(inBase)
    }

    // --- Reactive Result Flow ---
    private val formatter = DecimalFormat("###,###.##")

    @OptIn(FlowPreview::class)
    override val result: Flow<Pair<UnifiedReal, AnnotatedString>> =
        snapshotFlow {
            // Capture current converter state for logging/debugging
            Log.i(TAG, "converter: $converter, from: ${from.id}, to: ${target.id}")

            // Emit the raw text input from the text field
            value.text.toString()
        }
            // Prevent excessive recalculation while typing by waiting for a pause
            .debounce(DEBOUNCE_TIMEOUT)
            .transform { input ->
                // --- Step 1: Normalize input ---
                // Rules:
                // - If blank → use DEFAULT_VALUE
                // - If prefixed with default → drop prefix
                // - Otherwise → keep as-is
                val modified = when {
                    input.isBlank() -> DEFAULT_VALUE
                    input[0] == DEFAULT_VALUE[0] -> input.drop(1)
                    else -> input
                }

                // --- Step 2: Validate input ---
                // Reject if:
                // - Too long (exceeds MAX_ALLOWED_CHARS)
                // - Not a valid double
                when {
                    modified.length > MAX_ALLOWED_CHARS -> error("Max allowed length reached.")
                    modified.toDoubleOrNull() == null -> error("Provided input is invalid.")
                }

                // --- Step 3: Persist sanitized value ---
                preferences[KEY_VALUE] = modified

                // --- Step 4: Perform conversion ---
                val real = UnifiedReal(modified)
                val result = convert(from, target, real)

                // --- Step 5: Build annotated string with additional conversions ---
                // Define a lower limit threshold for display
                val limit = UnifiedReal("0.1")
                val more = buildAnnotatedString {
                    units.forEach { unit ->
                        val unitResult = convert(from, unit, real)
                        // Skip units where result is below threshold
                        if (unitResult < limit) return@forEach
                        // Append formatted conversion result for this unit
                        append("${(unit as SimpleMeasureUnit).symbol}: $unitResult\n")
                    }
                }

                // Emit the pair: numeric result + annotated string of conversions
                emit(result to more)
            }
            .catch {
                // On error, show a toast with the message instead of crashing
                showPlatformToast(it.message ?: "")
            }
}