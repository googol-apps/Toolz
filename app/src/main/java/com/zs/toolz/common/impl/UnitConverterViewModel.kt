package com.zs.toolz.common.impl

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.AnnotatedString
import com.zs.domain.math.UnifiedReal
import com.zs.preferences.stringPreferenceKey
import com.zs.toolz.converter.MeasureUnit
import com.zs.toolz.converter.RouteUnitConverter
import com.zs.toolz.converter.UnitConverterViewState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.transform

// --- Constants ---
private const val TAG = "UnitConverterViewModel"  // log tag
private const val DEBOUNCE_TIMEOUT = 15L          // debounce delay for input typing
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
        preferences[KEY_CONVERTER] ?: RouteUnitConverter.KEY_ANGLE_CONVERTER
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

    // --- Reactive Result Flow ---
    @OptIn(FlowPreview::class)
    override val result: Flow<Pair<UnifiedReal, AnnotatedString>> = snapshotFlow {
        // Log current converter state for debugging
        Log.i(TAG, "converter: $converter, from: ${from.id}, to: ${target.id}")
        value.text.toString()
    }.debounce(DEBOUNCE_TIMEOUT) // avoid excessive recalculation while typing
        .transform { input ->
            // Normalize input:
            // - blank → DEFAULT_VALUE
            // - prefixed with default → drop prefix
            // - otherwise → keep as-is
            val modified = when {
                input.isBlank() -> DEFAULT_VALUE
                input[0] == DEFAULT_VALUE[0] -> input.drop(1)
                else -> input
            }

            // Validate input:
            when {
                modified.length > MAX_ALLOWED_CHARS -> error("Max allowed length reached.")
                modified.toDoubleOrNull() == null -> error("Provided input is invalid.")
            }

            // Persist sanitized value
            preferences[KEY_VALUE] = modified

            // Emit numeric result + placeholder annotation
            emit(UnifiedReal(modified) to AnnotatedString(""))
        }.catch {
            // Show error feedback instead of crashing
            showPlatformToast(it.message ?: "")
        }
}