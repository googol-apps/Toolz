package com.zs.toolz.converter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.rotateTransform
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.OutlinedTextField
import com.zs.compose.theme.text.TextField
import com.zs.toolz.common.Res
import com.zs.toolz.common.vectorResource


val NumberFormatTransformation = OutputTransformation {
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
val FIELD_MIN_HEIGHT = 94.dp


@Composable
context(_: UnitConverter)
fun ValueField(
    state: TextFieldState,
    expanded: Boolean,
    onRequestCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    //
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Res.dimen.small)
    ) {
        //Header
        Label(
            text = "FROM",
            style = AppTheme.typography.label3,
            modifier = Modifier.rotateTransform(false),
        )
        OutlinedTextField(
            state = state,
            readOnly = true,
            lineLimits = TextFieldLineLimits.SingleLine,
            enabled = !expanded,
            outputTransformation = NumberFormatTransformation,
            label = { Label("Length") },
            textStyle = AppTheme.typography.headline3.copy(
                fontWeight = FontWeight.SemiBold
            ),
            trailingIcon = {
                val rotate by animateFloatAsState(targetValue = if (expanded) 0f else 180f)
                IconButton (
                    onClick = { onRequestCollapse() },
                    icon = vectorResource(Res.drawable.ic_expand_more),
                    contentDescription = null,
                    modifier = Modifier.rotate(rotate)
                )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = FIELD_MIN_HEIGHT),
        )
    }

}

@Composable
context(_: UnitConverter)
fun ResultField(
    value: String,
    expanded: Boolean,
    onRequestCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    //
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Res.dimen.small)
    ) {
        //Header
        Label(
            text = "EQUALS TO",
            style = AppTheme.typography.label3,
            modifier = Modifier.rotateTransform(false),
        )
        val state = rememberTextFieldState(value)
        SideEffect(value) {
            state.edit {
                this.delete(0, state.text.length)
                this.insert(0, value)
            }
        }
        TextField(
            state =state,
            readOnly = true,
            lineLimits = TextFieldLineLimits.SingleLine,
            enabled = !expanded,
            outputTransformation = NumberFormatTransformation,
            label = { Label("Length") },
            textStyle = AppTheme.typography.headline3.copy(
                fontWeight = FontWeight.SemiBold
            ),
            trailingIcon = {
                val rotate by animateFloatAsState(targetValue = if (expanded) 0f else 180f)
                IconButton (
                    onClick = { onRequestCollapse() },
                    icon = vectorResource(Res.drawable.ic_expand_more),
                    contentDescription = null,
                    modifier = Modifier.rotate(rotate)
                )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = FIELD_MIN_HEIGHT),
        )
    }

}