package com.zs.toolz.converter

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.rotateTransform
import com.zs.compose.foundation.thenIf
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.menu.DropdownMenuItem
import com.zs.compose.theme.menu.ExposedDropdownMenuAnchorType
import com.zs.compose.theme.menu.ExposedDropdownMenuBox
import com.zs.compose.theme.menu.ExposedDropdownMenuBoxScope
import com.zs.compose.theme.text.Header
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.OutlinedTextField
import com.zs.compose.theme.text.Text
import com.zs.compose.theme.text.TextField
import com.zs.compose.theme.text.TextFieldDefaults
import com.zs.toolz.common.Res
import com.zs.toolz.common.gap_small
import com.zs.toolz.common.orbitron

@Composable
private inline operator fun MeasureUnit.component1() = when (this) {
    is SimpleMeasureUnit -> stringResource(symbol)
    is RawMeasureUnit -> symbol
    else -> TODO("implement the type ${this::class.simpleName}")
}

@Composable
private inline operator fun MeasureUnit.component2() = when (this) {
    is SimpleMeasureUnit -> stringResource(title)
    is RawMeasureUnit -> title
    else -> TODO("implement the type ${this::class.simpleName}")
}

private val MeasureUnit.group: String
    @Composable inline get() = when (this) {
        is SimpleMeasureUnit -> stringResource(group)
        is RawMeasureUnit -> group
        else -> TODO("implement the type ${this::class.simpleName}")
    }

private val NumberFormatTransformation = OutputTransformation {
    val text = asCharSequence()

    // Start offset: skip a leading sign, if present.
    val signOffset = if (text.isNotEmpty() && (text[0] == '+' || text[0] == '-')) 1 else 0

    // End offset: whole part stops at the first '.' or 'e'/'E', or at the end of text.
    val dotIndex = text.indexOf('.')
    val expIndex = text.indexOfAny(charArrayOf('e', 'E'))
    val wholeEnd = when {
        dotIndex >= 0 -> dotIndex
        expIndex >= 0 -> expIndex
        else -> text.length
    }

    // Insert separators from the rightmost group inward, within [signOffset, wholeEnd).
    var k = 1
    while (true) {
        val pos = wholeEnd - 3 * k
        if (pos <= signOffset) break
        insert(pos, ",")
        k++
    }
}
val FIELD_MIN_HEIGHT = 84.dp

@Composable
private fun Unit(
    value: MeasureUnit,
    checked: Boolean,
    onToggleRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    DropdownMenuItem(
        onClick = onToggleRequest,
        enabled = !checked,
        modifier = modifier.thenIf(checked) {
            drawBehind {
                drawRoundRect(
                    colors.onBackground.copy(ContentAlpha.indication),
                    size = size.copy(size.width - 2 * Res.space.medium.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    topLeft = Offset(Res.space.small.toPx(), 0f)
                )
            }
        },
        content = {
            val (symbol, title) = value
            val color = if (checked) colors.accent else LocalContentColor.current
            Text(
                text = symbol,
                style = AppTheme.typography.title3,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                modifier = Modifier.padding(start = Res.space.normal),
                style = AppTheme.typography.body3,
                color = color,
            )
        }
    )
}

@Composable
private inline fun ExpandableUnitsMenu(
    expanded: Boolean,
    checked: MeasureUnit,
    values: List<MeasureUnit>, // the available units
    noinline onToggleRequest: (unit: MeasureUnit?) -> Unit, // null, just close else check and close
    crossinline field: @Composable ExposedDropdownMenuBoxScope.() -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { /*expanded = it*/ },
        content = {
            field()
            val state = rememberScrollState()
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onToggleRequest(null) },
                scrollState = state,
                modifier = Modifier
                    .exposedDropdownSize(true)
                    .heightIn(max = 360.dp),
                content = {
                    val accent = AppTheme.colors.accent
                    for (i in values.indices) {
                        val prev = values.getOrNull(i - 1)
                        val unit = values[i]
                        if (prev?.group != unit.group)
                            Header(
                                text = unit.group,
                                style = AppTheme.typography.title1,
                                drawDivider = true,
                                contentPadding = HeaderPadding
                            )
                        Unit(
                            value = unit,
                            checked = unit == checked,
                            onToggleRequest = { onToggleRequest(unit) }
                        )
                    }
                }
            )
        }
    )
}

private val HeaderPadding = PaddingValues(horizontal = Res.space.normal, vertical = Res.space.small)

@Composable
fun ValueField(
    viewState: UnitConverterViewState,
    modifier: Modifier = Modifier
) {
    val selected = viewState.from
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Res.layout.gap_small
    ) {
        // Header
        Label(
            text = stringResource(Res.string.from).uppercase(),
            style = AppTheme.typography.label3,
            modifier = Modifier.rotateTransform(false),
        )

        // value Field
        var isFocused by remember { mutableStateOf(false) }
        val manager = LocalFocusManager.current
        BackHandler(isFocused) {
            manager.clearFocus(true)
        }

        ExpandableUnitsMenu(
            expanded = isFocused,
            checked = selected,
            onToggleRequest = { if (it != null) viewState.from = it;manager.clearFocus(true) },
            values = viewState.units,
            field = {
                OutlinedTextField(
                    state = viewState.value,
                    readOnly = true,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    outputTransformation = NumberFormatTransformation,
                    label = { Label(stringResource((selected as SimpleMeasureUnit).title)) },
                    shape = AppTheme.shapes.medium,
                    textStyle = AppTheme.typography.headline1.copy(
                        fontFamily = Res.font.orbitron
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        unfocusedBorderColor = AppTheme.colors.accent
                    ),
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        .defaultMinSize(minHeight = FIELD_MIN_HEIGHT, minWidth = Dp.Infinity)
                    ,
                )
            },
        )
    }
}

@Composable
fun ResultField(
    result: String,
    viewState: UnitConverterViewState,
    modifier: Modifier = Modifier
) {
    val selected = viewState.target
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Res.layout.gap_small
    ) {
        // Header
        Label(
            text = stringResource(Res.string.equals_to).uppercase(),
            style = AppTheme.typography.label3,
            modifier = Modifier.rotateTransform(false),
        )

        // value Field
        var isFocused by remember { mutableStateOf(false) }
        val manager = LocalFocusManager.current
        BackHandler(isFocused) {
            manager.clearFocus(true)
        }

        ExpandableUnitsMenu(
            expanded = isFocused,
            checked = selected,
            onToggleRequest = { if (it != null) viewState.target = it;manager.clearFocus(true) },
            values = viewState.units,
            field = {
                val state = rememberTextFieldState(result)
                TextField(
                    state = state,
                    readOnly = true,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    outputTransformation = NumberFormatTransformation,
                    label = { Label(stringResource((selected as SimpleMeasureUnit).title)) },
                    shape = AppTheme.shapes.medium,
                    textStyle = AppTheme.typography.headline1.copy(
                        fontFamily = Res.font.orbitron
                    ),
                    colors = TextFieldDefaults.textFieldColors(),
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        .defaultMinSize(minHeight = FIELD_MIN_HEIGHT, minWidth = Dp.Infinity)
                    ,
                )
                SideEffect(result) {
                    state.setTextAndPlaceCursorAtEnd(result)
                }
            },
        )
    }
}