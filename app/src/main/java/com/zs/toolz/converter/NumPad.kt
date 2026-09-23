package com.zs.toolz.converter

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridFlow
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.GridTrackSize.Companion.Fixed
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.columns
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.Slot
import com.zs.compose.foundation.decorator.decorator
import com.zs.compose.foundation.linearGradient
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Button
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.Surface
import com.zs.compose.theme.text.Label
import com.zs.toolz.common.Res
import com.zs.toolz.common.vectorResource

private val SquareModifier = Modifier.aspectRatio(1.0f)

@Composable
private fun NumButton(
    label: CharSequence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Slot(
        modifier = modifier
            .decorator(
                backgroundColor = AppTheme.colors.background(if (AppTheme.colors.isLight) 6.dp else 1.dp),
                //shape = AppTheme.shapes.small,
                noiseAlpha = 0.3f,
            )
            .clickable(onClick = onClick)
            .then(SquareModifier)
    ) {
        Label(label, style = AppTheme.typography.headline1)
    }
}

@OptIn(ExperimentalGridApi::class)
@Composable
fun NumPad(
    onKeyPress: (key: Key) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Grid(
        modifier = modifier,
        config = {
            gap(2.dp)
            repeat(4) {
                column(0.25f)
            }
        },
        content = {
            NumButton(
                label = textResource(Res.string.digit_1),
                onClick = { onKeyPress(Key.NumPad1) },
            )
            NumButton(
                label = textResource(Res.string.digit_2),
                onClick = { onKeyPress(Key.NumPad2) },
            )
            NumButton(
                label = textResource(Res.string.digit_3),
                onClick = { onKeyPress(Key.NumPad3) },
            )
            NumButton(
                label = textResource(Res.string.digit_4),
                onClick = { onKeyPress(Key.NumPad4) },
            )
            NumButton(
                label = textResource(Res.string.digit_5),
                onClick = { onKeyPress(Key.NumPad5) },
            )
            NumButton(
                label = textResource(Res.string.digit_6),
                onClick = { onKeyPress(Key.NumPad6) },
            )
            NumButton(
                label = textResource(Res.string.digit_7),
                onClick = { onKeyPress(Key.NumPad7) },
            )
            NumButton(
                label = textResource(Res.string.digit_8),
                onClick = { onKeyPress(Key.NumPad8) },
            )
            NumButton(
                label = textResource(Res.string.digit_9),
                onClick = { onKeyPress(Key.NumPad9) },
            )
            NumButton(
                label = ".",
                onClick = { onKeyPress(Key.NumPadDot) },
            )
            NumButton(
                label = textResource(Res.string.digit_0),
                onClick = { onKeyPress(Key.NumPad0) },
            )
            Button(onClick = { onKeyPress(Key.Backspace) }, shape = Res.shape.rectangle, modifier = SquareModifier) {
                Icon(
                    vectorResource(Res.drawable.ic_backspace_outline),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    )
}

