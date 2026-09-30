package com.zs.toolz.common.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.decorator.decorator
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.toolz.common.Res
import com.zs.toolz.common.shape.EndConcaveShape
import com.zs.toolz.common.shape.TopConcaveShape

@Composable
fun NavigationBar(
    vertical: Boolean,
    modifier: Modifier = Modifier,
    insets: WindowInsets = if (vertical) AppBarDefaults.sideBarWindowInsets else AppBarDefaults.bottomAppBarWindowInsets,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier, contentAlignment = if (vertical) Alignment.BottomCenter else Alignment.TopStart) {
        when {
            vertical ->  Column(
                content = { content() }, modifier = Modifier
                    .pointerInput(Unit, {})
                    .decorator(
                        AppTheme.colors.accent,
                        elevation = 4.dp,
                        shape = EndConcaveShape(16.dp),
                        noiseAlpha = 0.4f
                    )
                    .windowInsetsPadding(insets)
                    .padding(vertical = Res.space.normal, horizontal = Res.space.x_small)
                    .defaultMinSize(94.dp, Dp.Infinity),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            )

            else -> Row(
                content = { content() }, modifier = Modifier
                    .pointerInput(Unit, {})
                    .decorator(
                        AppTheme.colors.accent,
                        elevation = 4.dp,
                        shape = TopConcaveShape(16.dp),
                        noiseAlpha = 0.4f
                    )
                    .windowInsetsPadding(insets)
                    .padding(horizontal = Res.space.normal, vertical = Res.space.x_small)
                    .defaultMinSize(Dp.Infinity, 74.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            )
        }
    }
}