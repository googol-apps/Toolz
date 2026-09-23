package com.zs.toolz.common.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.decorator.decorator
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.toolz.common.Res

@Composable
fun NavigationBar(
    vertical: Boolean,
    modifier: Modifier = Modifier,
    insets: WindowInsets = if (vertical) AppBarDefaults.sideBarWindowInsets else AppBarDefaults.bottomAppBarWindowInsets,
    elevation: Dp = 4.dp,
    shape: Shape = if (vertical) Res.shape.rectangle else Res.shape.circle,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = if (vertical) Alignment.TopStart else Alignment.Center,
        modifier = modifier
    ) {
        when {
            vertical -> Column(
                content = { content() },
                modifier = Modifier
                    .pointerInput(Unit, {})
                    .decorator(
                        AppTheme.colors.background(2.dp),
                        elevation = elevation,
                        shape = shape
                    )
                    .windowInsetsPadding(insets)
                    .size(100.dp, Dp.Infinity)
            )

            else -> Row(
                content = { content() }, modifier = Modifier
                    .pointerInput(Unit, {})
                    .windowInsetsPadding(insets)
                    .then(if (vertical) Modifier else Modifier.padding(horizontal = Res.dimen.normal))
                    .decorator(
                        AppTheme.colors.background(2.dp),
                        elevation = elevation,
                        shape = shape
                    )
                    .padding(horizontal = Res.dimen.normal, vertical = Res.dimen.x_small)
                    .size(Dp.Infinity, 56.dp)
            )
        }
    }
}