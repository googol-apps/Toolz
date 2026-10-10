@file:Suppress("PrivatePropertyName")

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
import androidx.compose.ui.text.withStyle
import com.zs.domain.math.UnifiedReal
import com.zs.preferences.stringPreferenceKey
import com.zs.toolz.converter.MeasureUnit
import com.zs.toolz.converter.RawMeasureUnit
import com.zs.toolz.converter.SimpleMeasureUnit
import com.zs.toolz.converter.UnitConverter
import com.zs.toolz.converter.UnitConverterViewState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
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
private const val MAX_ALLOWED_CHARS = 12          // max input length

class UnitConverterViewModel(val clipboard: ClipboardManager) : KoinViewModel(), UnitConverterViewState {

    // --- Preference Keys ---
    private val KEY_CONVERTER = stringPreferenceKey("${TAG}_converter")
    private val KEY_UNIT_FROM = stringPreferenceKey("${TAG}_unit_from")
    private val KEY_UNIT_TO = stringPreferenceKey("${TAG}_unit_to")
    private val KEY_VALUE = stringPreferenceKey("${TAG}_converter_value")

    // --- Backing State ---
    // Currently selected converter key (persisted in preferences)
    private val _converter = mutableStateOf(
        preferences[KEY_CONVERTER] ?: UnitConverter.KEY_ANGLE_CONVERTER
    )

    // Units available for the active converter
    override var units: List<MeasureUnit> = Units(_converter.value)

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

        // The unit pickers already update on screen, so keep the message short
        // and say what changed instead of a generic "Units Swapped!".
        // (symbol is on SimpleMeasureUnit, as used in the suggestions code.)
        // TODO- add others as well.
        val fromSymbol = (from as SimpleMeasureUnit).symbol
        val targetSymbol = (target as SimpleMeasureUnit).symbol
        showPlatformToast("Now converting $fromSymbol → $targetSymbol")
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

        // getText(...) resolves the localized unit name/symbol (as in the original).
        val fromLabel = from.symbol
        val toLabel = from.symbol

        // Clipboards hold plain text, so italic styling would be lost anyway.
        // Build a simple string instead of an AnnotatedString.
        val text = "$input $fromLabel = $latestResultText $toLabel"

        // Android ClipboardManager API (the one provided through Koin).
        // setPrimaryClip is not a suspend function, so no coroutine is needed.
        clipboard.setPrimaryClip(ClipData.newPlainText("Converted value", text))

        // Android 13+ shows its own "Copied" confirmation, so only toast on older versions.
        //if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        showPlatformToast("Copied: $text")
    }

    /**
     * The method to convert [value]from -> to
     */
    private fun convert(from: MeasureUnit, to: MeasureUnit, value: UnifiedReal): UnifiedReal {
        val inBase = from.toBase(value)
        return to.toUnit(inBase)
    }

    val MeasureUnit.symbol: String
        inline get() = when (this) {
            is SimpleMeasureUnit -> getText(symbol).toString()
            is RawMeasureUnit -> symbol
            else -> TODO("implement the type ${this::class.simpleName}")
        }

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

    /**
     * Applies a numpad key press to the text field state.
     *
     * Rules enforced here (since the numpad is the only input source):
     *  - The field is never empty: it falls back to "0".
     *  - At most one decimal point.
     *  - A leading "0" is replaced when the user types a digit (no "05").
     *  - Minus is a toggle and only ever lives at index 0.
     *
     * Note: TextFieldBuffer already shifts the selection when replace/insert/delete
     * are called. Explicit `selection = ...` lines are only strictly needed after
     * a whole-buffer replace. If the cursor ever lands one position off, remove
     * the redundant ones in the dot/minus branches.
     */
    override fun onKeyPress(key: Key) {
        value.edit {
            // Cursor is always at the end; there is no mid-text editing.
            selection = TextRange(length)

            when (key) {
                // ---------- BACKSPACE ----------
                Key.Backspace -> {
                    if (length > 0) delete(length - 1, length)
                    // Never leave the field empty or with a lone minus
                    if (length == 0 || asCharSequence().toString() == "-") replace(0, length, "0")
                }

                // ---------- DECIMAL POINT ----------
                Key.NumPadDot -> {
                    if ('.' in asCharSequence()) return@edit // one dot only
                    if (length >= MAX_ALLOWED_CHARS) {
                        showPlatformToast("Input length exceeded")
                        return@edit
                    }
                    append(".")
                }

                // ---------- SIGN TOGGLE (MINUS) ----------
                Key.NumPadSubtract -> {
                    if (asCharSequence().startsWith('-')) {
                        delete(0, 1) // removing a character can never exceed the max
                    } else {
                        if (length >= MAX_ALLOWED_CHARS) {
                            showPlatformToast("Input length exceeded")
                            return@edit
                        }
                        insert(0, "-")
                    }
                }

                // ---------- DIGITS (0-9) ----------
                else -> {
                    val digit = key.toDigit().toString()
                    when (asCharSequence().toString()) {
                        "0" -> replace(0, length, digit)  // "0" -> "5"; "0" then 0 stays "0"
                        "-0" -> replace(1, length, digit) // "-0" -> "-5"
                        else -> {
                            if (length >= MAX_ALLOWED_CHARS) {
                                showPlatformToast("Input length exceeded")
                                return@edit
                            }
                            append(digit)
                        }
                    }
                }
            }

            // Single place that owns the cursor: always at the end
            selection = TextRange(length)
        }
    }

    // Max number of "about equals" suggestions to show
    private val MAX_EXTRAS = 4

    // Optional minus, digits, optional dot with optional decimals: "5", "-5", "5.", "0.25"
    private val NUMBER_REGEX = Regex("""^-?\d+(\.\d*)?$""")

    // Formats suggestion values with grouping separators and up to 2 decimals
    private val EXTRAS_FORMATTER = DecimalFormat("###,###.##")

    @OptIn(FlowPreview::class)
    override val result: Flow<Pair<UnifiedReal, AnnotatedString>> =
    // The hash covers everything the result depends on (converter, units, text).
        // snapshotFlow re-runs when any of them changes and emits when the hash differs.
        snapshotFlow { Objects.hash(converter, from.id, target.id, value.text) }
            // Wait for a short pause in typing to avoid recalculating on every key press
            .debounce(DEBOUNCE_TIMEOUT)
            .transform { _ ->
                // The upstream value is only a trigger. Read the current text directly
                // so we always use the latest input.
                val text = value.text.toString()

                // --- Step 1: Parse ---
                // runCatching turns a parse failure into null.
                // Note: a mid-typing "5." may not parse; if it throws, parse text.trimEnd('.') instead.
                val real = runCatching { UnifiedReal(text) }.getOrNull()
                if (real == null) {
                    showPlatformToast("Invalid input")
                    return@transform // skip this input, keep the flow alive
                }

                // --- Step 2: Persist ---
                // Only input that parsed successfully is saved, so a bad value is never restored.
                preferences[KEY_VALUE] = text

                // --- Step 3: Main conversion (from -> target) ---
                val result = convert(from, target, real)

                // --- Step 4: "About equals" suggestions ---
                // Suggest the units whose value is closest to 1 on a log scale
                // (so 0.04 and 25 are equally far from 1). These are the most
                // human-readable numbers, e.g. 1 hour -> day, min, week, second.
                val more = buildAnnotatedString {
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
                }

                // Emit: main result + annotated string of suggestions
                emit(result to more)
            }
}