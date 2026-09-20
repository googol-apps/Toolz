package com.zs.toolz.common.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.backdrop.Backdrop
import com.zs.compose.foundation.backdrop.haze.BlurConfig
import com.zs.compose.foundation.backdrop.haze.hazeEffect
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.toolz.common.Res

val edgeHighlight = BorderStroke(
    2.dp ,
    Brush.linearGradient(
        colorStops = arrayOf(
            0.0f  to Color.White.copy(alpha = 0.55f ),
            0.45f to Color.White.copy(alpha = 0.08f),
            0.55f to Color.Transparent,
            1.0f  to Color.DarkGray.copy(alpha = 0.55f ),
        ),
        start = Offset.Zero,                // top-left corner
        end = Offset(100f, 100f)            // diagonal spread
    )
)


@Composable
fun NavigationBar(
    content: @Composable RowScope.() -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    insets: WindowInsets = AppBarDefaults.bottomAppBarWindowInsets,
    elevation: Dp = 0.dp,
    shape: Shape = Res.shape.rectangle
) {
    Box(contentAlignment = Alignment.CenterEnd, modifier = modifier) {
        Row(
            modifier = modifier
                .windowInsetsPadding(insets)
                .padding(horizontal = Res.dimen.normal)
                //.acrylic(elevation= elevation, shape = shape,AppTheme.colors.background, AppTheme.colors.accent)
                .hazeEffect(
                    backdrop,
                    AppTheme.colors.background,
                    blurConfig = BlurConfig(0.5f, 20f),
                    tint = AppTheme.colors.background(1.dp),
                    elevation,
                    noiseAmount = 0.3f,
                    edgeHighlight = edgeHighlight,
                    vibrancy = 1.4f,
                    shape = shape
                )
                .height(56.dp),
            content = content
        )
    }
}