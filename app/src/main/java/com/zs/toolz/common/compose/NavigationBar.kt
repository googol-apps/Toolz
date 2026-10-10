package com.zs.toolz.common.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.decorator.decorator
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.toolz.common.Res
import com.zs.toolz.common.shape.EndConcaveShape
import com.zs.toolz.common.shape.TopConcaveShape

private val SideBarBorder = BorderStroke(
    1.dp,
    Brush.horizontalGradient(
        listOf(
            Color.Transparent,
            Color.Transparent,
            Color.Gray.copy(0.24f),
            Color.Transparent,
        )
    )
)
private val BottomBarBorder =  BorderStroke(
    1.dp,
    Brush.verticalGradient(
        listOf(
            Color.Gray,
            Color.Transparent,
            Color.Transparent,
            Color.Transparent,
        )
    )
)

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
                        noiseAlpha = 0.4f,
                        border = SideBarBorder
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
                        noiseAlpha = 0.4f,
                        border = BottomBarBorder
                    )
                    .windowInsetsPadding(insets)
                    .padding(top = Res.space.normal)
                    .wrapContentHeight()
                    .defaultMinSize(Dp.Infinity, 0.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            )
        }
    }
}