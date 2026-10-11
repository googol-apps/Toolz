@file:Suppress("PrivatePropertyName", "ConvertLongToDuration")

package com.zs.toolz.common.impl

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.zs.domain.math.NumUtil
import com.zs.domain.math.UnifiedReal
import com.zs.preferences.stringPreferenceKey
import com.zs.toolz.converter.MeasureUnit
import com.zs.toolz.converter.RawMeasureUnit
import com.zs.toolz.converter.SimpleMeasureUnit
import com.zs.toolz.converter.RouteUnitConverter
import com.zs.toolz.converter.UnitConverterViewState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.transform
import java.text.DecimalFormat
import java.util.Objects
import kotlin.math.abs
import kotlin.math.log10

// --- Constants ---
private const val TAG = "UnitConverterViewModel"  // log tag
private const val DEBOUNCE_TIMEOUT = 15L         // debounce delay for input typing
private const val DEFAULT_VALUE = "0"             // default fallback value
private const val MAX_EXTRAS = 4  // Max number of "about equals" suggestions to show


/**
 * Maps a numpad key to its digit value.
 * Only called for keys that are known to be digits (the numpad has no other
 * keys besides Backspace, Dot and Subtract, which are handled before this).
 * If hardware keyboard handling is added later, switch to a nullable version
 * so stray keys (Enter, Tab, letters) don't throw.
 */
private fun Key.toDigit(): Int {
    return when (this) {
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

class UnitConverterViewModel(val clipboard: ClipboardManager) : KoinViewModel(),
    UnitConverterViewState {
    // --- Preference Keys ---
    private val KEY_CONVERTER = stringPreferenceKey("${TAG}_converter")
    private val KEY_UNIT_FROM = stringPreferenceKey("${TAG}_unit_from")
    private val KEY_UNIT_TO = stringPreferenceKey("${TAG}_unit_to")
    private val KEY_VALUE = stringPreferenceKey("${TAG}_converter_value")

    private val NUMBER_REGEX =
        Regex("""^-?\d+(\.\d*)?$""")  // Optional minus, digits, optional dot with optional decimals: "5", "-5", "5.", "0.25"
    private val EXTRAS_FORMATTER =
        DecimalFormat("###,###.##") // Formats suggestion values with grouping separators and up to 2 decimals


    val MeasureUnit.symbol: String
        inline get() = when (this) {
            is SimpleMeasureUnit -> getText(symbol).toString()
            is RawMeasureUnit -> symbol
            else -> TODO("implement the type ${this::class.simpleName}")
        }

    // ===================== Backing state =====================
    // Active converter key (length, mass, ...). Restored from preferences, defaults to angle.
    private val _converter = mutableStateOf(
        preferences[KEY_CONVERTER] ?: RouteUnitConverter.KEY_ANGLE_CONVERTER
    )

    // Units of the active converter, in display order.
    override var units: List<MeasureUnit> = Units(_converter.value)

    // Source unit. Restored by id, falls back to the converter's first unit.
    private val _from = mutableStateOf(
        with(preferences) {
            val uuid = get(KEY_UNIT_FROM) ?: units[0].id
            units.find { it.id == uuid }!!
        }
    )

    // Target unit. Falls back to the second unit so from != target by default.
    private val _target = mutableStateOf(
        with(preferences) {
            val uuid = get(KEY_UNIT_TO) ?: units[1].id
            units.find { it.id == uuid }!!
        }
    )

    // User input field state (persisted)
    override var value: TextFieldState = TextFieldState(
        preferences[KEY_VALUE] ?: DEFAULT_VALUE
    )

    override var converter: String
        get() = _converter.value
        set(value) {
            // Load the new category's units first, so the defaults below come from
            // the new list. `units` is not snapshot state, so assigning it notifies no one.
            units = Units(value)

            // Apply all observable writes in ONE snapshot so observers (the result
            // flow's snapshotFlow, the UI) see them together and never a mixed state
            // such as the new converter with the old `from`/`target`.
            Snapshot.withMutableSnapshot {
                _converter.value = value    // update observable state
                from = units[0]             // reset to the new category's defaults
                target = units[1]
            }

            // Persist the converter choice. Done after the snapshot, so it only
            // runs once the in-memory state is consistent.
            preferences[KEY_CONVERTER] = value
        }

    // Source unit. Setter persists the id for the next launch.
    override var from: MeasureUnit
        get() = _from.value
        set(value) {
            _from.value = value
            preferences[KEY_UNIT_FROM] = value.id
        }

    // Target unit. Persisted like `from`.
    override var target: MeasureUnit
        get() = _target.value
        set(value) {
            _target.value = value
            preferences[KEY_UNIT_TO] = value.id
        }

    // Swap the source and target units (e.g. m -> km becomes km -> m).
    override fun swap() {
        // Nothing to do when both sides are the same unit.
        if (from == target) return

        // Apply both writes in one snapshot. Without this, observers such as the
        // result flow's snapshotFlow could see a half-swapped state
        // (from == target) between the two assignments.
        Snapshot.withMutableSnapshot {
            val previousFrom = from
            from = target            // setter also persists KEY_UNIT_FROM
            target = previousFrom    // setter also persists KEY_UNIT_TO
        }
        // Notify the UI.
        showPlatformToast("Source and target swapped.")
    }

    // Last result text shown to the user. Set in the result flow's transform
    // right after the conversion, e.g. `latestResultText = EXTRAS_FORMATTER.format(result.doubleValue())`.
    // Needed because `result` is a Flow and has no current value to read.
    @Volatile
    private var latestResultText: String = DEFAULT_VALUE

    // Copy the conversion as plain text, e.g. "1 m = 100 cm".
    override fun copy() {
        // Use the displayed input text, not the TextFieldState object itself.
        val input = value.text.toString()

        // Clipboards hold plain text, so italic styling would be lost anyway.
        // Build a simple string instead of an AnnotatedString.
        val text = "$input ${from.symbol} = $latestResultText ${target.symbol}"

        // Android ClipboardManager API (the one provided through Koin).
        // setPrimaryClip is not a suspend function, so no coroutine is needed.
        clipboard.setPrimaryClip(ClipData.newPlainText("Converted value", text))

        // Android 13+ shows its own "Copied" confirmation, so only toast on older versions.
        //if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        showPlatformToast("Copied: $text")
    }

    // The method to convert [value]from -> to
    private fun convert(from: MeasureUnit, to: MeasureUnit, value: UnifiedReal): UnifiedReal {
        val inBase = from.toBase(value)
        return to.toUnit(inBase)
    }

    //
    override fun onKeyPress(key: Key) {
        value.edit {
            // The numpad is the only input source, so the field is edited like a
            // calculator display: the cursor always sits at the end and there is
            // no mid-text editing. Collapsing the selection here also covers the
            // early `return@edit` paths below, where the text stays unchanged.
            selection = TextRange(length)

            when (key) {
                // ---------- BACKSPACE ----------
                // Rule: the field is never empty. Removing the last character can
                // leave "" or a lone "-", and neither is a valid number, so both
                // fall back to the default "0".
                Key.Backspace -> {
                    if (length > 0) delete(length - 1, length)
                    if (length == 0 || asCharSequence().toString() == "-") replace(0, length, "0")
                }

                // ---------- DECIMAL POINT ----------
                // Rule: at most one decimal point, and never past MAX_ALLOWED_CHARS.
                // The field is never empty or a lone "-", so a plain append is
                // enough: "0" -> "0." and "-0" -> "-0.".
                Key.NumPadDot -> {
                    if ('.' in asCharSequence()) return@edit // one dot only
                    if (length >= RouteUnitConverter.MAX_ALLOWED_CHARS) {
                        showPlatformToast("Input length exceeded")
                        return@edit
                    }
                    append(".")
                }

                // ---------- SIGN TOGGLE (MINUS) ----------
                // Rule: the minus is a toggle and only ever lives at index 0.
                // Removing it can never exceed the max length, but adding it can,
                // so only the add path checks the length.
                Key.NumPadSubtract -> {
                    if (asCharSequence().startsWith('-')) {
                        delete(0, 1) // removing a character can never exceed the max
                    } else {
                        if (length >= RouteUnitConverter.MAX_ALLOWED_CHARS) {
                            showPlatformToast("Input length exceeded")
                            return@edit
                        }
                        insert(0, "-")
                    }
                }

                // ---------- DIGITS (0-9) ----------
                // Rule: a leading zero is replaced when a digit is typed (no "05"
                // or "-05"). Only the plain append path can grow the text, so only
                // that path checks the max length.
                else -> {
                    val digit = key.toDigit().toString()
                    when (asCharSequence().toString()) {
                        "0" -> replace(0, length, digit)  // "0" -> "5"; "0" then 0 stays "0"
                        "-0" -> replace(1, length, digit) // "-0" -> "-5"
                        else -> {
                            if (length >= RouteUnitConverter.MAX_ALLOWED_CHARS) {
                                showPlatformToast("Input length exceeded")
                                return@edit
                            }
                            append(digit)
                        }
                    }
                }
            }

            // Single place that owns the cursor: always at the end.
            // TextFieldBuffer already shifts the selection when replace/insert/delete
            // are called, so no branch above sets it manually. Setting it once here
            // avoids double-shifting (for example after insert(0, "-")).
            selection = TextRange(length)
        }
    }

    @OptIn(FlowPreview::class)
    override val result = snapshotFlow { Objects.hash(converter, from.id, target.id, value.text) }
        .debounce(DEBOUNCE_TIMEOUT)
        .transform { _ ->
            // The upstream value is only a trigger. Read the current text directly
            // so the latest input is always used.
            val input = value.text.toString()

            // --- Step 1: Parse ---
            // runCatching turns a parse failure into null.
            // A mid-typing "5." may not parse; if it throws, parse inputText.trimEnd('.') instead.
            val real = runCatching { UnifiedReal(input) }.getOrNull()
            if (real == null) {
                showPlatformToast("Invalid input")
                return@transform // skip this input, keep the flow alive
            }

            // --- Step 2: Persist ---
            // Only input that parsed successfully is saved, so a bad value is never restored.
            preferences[KEY_VALUE] = input
            // --- Step 3: Main conversion (from -> target) ---
            val result = convert(from, target, real)
            // Display/copy text of the main result, used by copy().
            // Rounded to at most 2 decimals and MAX_ALLOWED_CHARS characters.
            latestResultText =
                NumUtil.doubleToString(result.doubleValue(), RouteUnitConverter.MAX_ALLOWED_CHARS, 2)!!
            // --- Step 4: "About equals" suggestions ---
            // Suggest the units whose value is closest to 1 on a log scale
            // (so 0.04 and 25 are equally far from 1). These are the most
            // human-readable numbers, e.g. 1 hour -> day, min, week, second.
            val extras = buildAnnotatedString {
                val units = units // local copy so it can't change mid-computation

                // 4a: Convert to every unit once. values[i] belongs to units[i].
                // distance[i] is how far that value is from 1 (smaller = more natural).
                // NaN marks "not a candidate".
                val values = DoubleArray(units.size)
                val distance = DoubleArray(units.size)
                for (i in units.indices) {
                    val unit = units[i]
                    val value = convert(from, unit, real).doubleValue()
                    values[i] = value

                    distance[i] =
                        if (unit == from || unit == target) Double.NaN // already shown as the main result
                        else if (!value.isFinite() || value == 0.0) Double.NaN // log10(0) is -infinity
                        else abs(log10(abs(value)))
                }

                // 4b: Pick the candidate with the smallest distance, MAX_EXTRAS times.
                // Ties go to the unit that comes first in the list.
                repeat(MAX_EXTRAS) {
                    var best = -1
                    for (i in distance.indices) {
                        if (distance[i].isNaN()) continue // not a candidate, or already picked
                        if (best == -1 || distance[i] < distance[best]) best = i
                    }
                    if (best == -1) return@repeat // no candidates left

                    // Append "<value> <symbol>", with the symbol in gray italics
                    append(EXTRAS_FORMATTER.format(values[best]))
                    append(" ")
                    withStyle(SpanStyle(Color.Gray, fontStyle = FontStyle.Italic)) {
                        append(units[best].symbol)
                        append("    ")
                    }

                    distance[best] = Double.NaN // mark as picked so the next pass skips it
                }
                // Nothing was appended, so there are no suggestions
                if (length == 0) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal, fontSize = 14.sp)) {
                        append("Try another value to see comparisons.")
                    }
                }
            }
            // Emit: main result + annotated string of suggestions
            emit(result to extras.ifBlank { AnnotatedString("") })
        }
}