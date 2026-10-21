package com.zs.toolz.level

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.decorator.decorator
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.FloatingActionButton
import com.zs.compose.theme.Icon
import com.zs.compose.theme.adaptive.Scaffold
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.appbar.TopAppBar
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Label
import com.zs.toolz.common.Res
import com.zs.toolz.common.gap_large
import com.zs.toolz.common.gap_medium
import com.zs.toolz.common.gap_x_large
import com.zs.toolz.common.vectorResource
import com.zs.toolz.level.Tilt.Companion.by
import kotlinx.serialization.Serializable

private const val TAG = "RouteLevel"

@Serializable
object RouteLevel : NavKey {
    @Composable
    @NonRestartableComposable
    operator fun invoke() {
        val navigation = WindowInsets.content
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Label(
                            textResource(Res.string.scr_bubble_level_title),
                            maxLines = 2,
                            fontWeight = FontWeight.Light
                        )
                    },
                    navigationIcon = {
                        Icon(
                            vectorResource(Res.drawable.ic_tools_level_outline),
                            contentDescription = null,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    },
                    background = Background(
                        Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    AppTheme.colors.background(20.dp),
                                    AppTheme.colors.background(10.dp),
                                    AppTheme.colors.background(0.dp)
                                )
                            )
                        )
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {},
                    modifier = Modifier.windowInsetsPadding(navigation)
                ) {
                    Icon(vectorResource(Res.drawable.ic_gps_fixed), contentDescription = null)
                }
            },
            content = {
                val insets = WindowInsets.content
                Column(
                    modifier = Modifier.windowInsetsPadding(
                        navigation.union(insets).union(WindowInsets.systemBars)
                    ),
                    verticalArrangement = Arrangement.spacedBy(Res.space.x_large, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = {
                        val tilt = rememberDeviceTilt() by Tilt
                        val decorator = Modifier.decorator(
                            backgroundColor = AppTheme.colors.background(10.dp),
                            elevation = 21.dp,
                            shape = Res.shape.circle,
                            noiseAlpha = 0.5f
                        )
                        TubeLevel(
                            { tilt },
                            orientation = Orientation.Horizontal,
                            tint = AppTheme.colors.accent,
                            contentColor = AppTheme.colors.onBackground,
                            modifier = decorator
                        )
                        Row(horizontalArrangement = Res.layout.gap_x_large) {
                            SurfaceLevel(
                                { tilt },
                                tint = AppTheme.colors.accent,
                                contentColor = AppTheme.colors.onBackground,
                                modifier = decorator
                            )
                            TubeLevel(
                                { tilt },
                                orientation = Orientation.Vertical,
                                tint = AppTheme.colors.accent,
                                contentColor = AppTheme.colors.onBackground,
                                modifier = decorator
                            )
                        }
                    }
                )
            }
        )
    }
}