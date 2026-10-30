package com.zs.toolz.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.FloatingActionButton
import com.zs.compose.theme.Icon
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.adaptive.HorizontalTwoPaneStrategy
import com.zs.compose.theme.adaptive.SinglePaneStrategy
import com.zs.compose.theme.adaptive.TwoPane
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.appbar.TopAppBar
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Label
import com.zs.toolz.common.LocalSystemFacade
import com.zs.toolz.common.Res
import com.zs.toolz.common.vectorResource

object RouteSettings : NavKey {

    const val CONTENT_TYPE_HEADER = "header"
    val HeaderPadding = PaddingValues(vertical = Res.space.normal, horizontal = Res.space.small)

    @Composable
    operator fun invoke(viewState: SettingsViewState) {
        // Retrieve the current window size
        val (width, height) = LocalWindowSize.current
        // Determine the two-pane strategy based on window width range
        // when in mobile portrait; we don't show second pane;
        val strategy = when {
            width < height -> SinglePaneStrategy
            else -> HorizontalTwoPaneStrategy(0.55f) // Use horizontal layout with 55% split for large screens
        }
        // obtain the padding of BottomNavBar/NavRail
        val inAppNavBarInsets = WindowInsets.content
        val topAppBarScrollBehavior = AppBarDefaults.exitUntilCollapsedScrollBehavior()
        val colors = AppTheme.colors

        //
        BackHandler(enabled = viewState.isDirty) {
            viewState.discard()
        }

        // content
        TwoPane(
            strategy = strategy,
            spacing = 10.dp,
            floatingActionButton = {
                if (!viewState.isDirty)
                    return@TwoPane
                val facade = LocalSystemFacade.current
                FloatingActionButton(
                    onClick = { viewState.commit(facade) },
                    modifier = Modifier.windowInsetsPadding(inAppNavBarInsets.union(WindowInsets.navigationBars)),
                    content = {
                        Icon(vectorResource(Res.drawable.ic_save_outline), contentDescription = null)
                    }
                )
            },
            topBar = {
                TopAppBar(
                    title = {
                        Label(
                            textResource(Res.string.scr_settings_title),
                            maxLines = 2,
                            fontWeight = FontWeight.Light
                        )
                    },
                    navigationIcon = {
                        Icon(
                            vectorResource(Res.drawable.ic_settings_outline),
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
            primary = {}
        )
    }
}