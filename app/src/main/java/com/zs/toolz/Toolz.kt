package com.zs.toolz

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.zs.compose.foundation.backdrop.layerBackdropProvider
import com.zs.compose.foundation.backdrop.rememberBackdropLayer
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Icon
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.MotionScheme
import com.zs.compose.theme.adaptive.NavigationSuiteScaffold
import com.zs.compose.theme.appbar.BottomNavigationItem
import com.zs.compose.theme.calculateWindowSizeClass
import com.zs.compose.theme.dynamicAccentColor
import com.zs.compose.theme.renderInSharedTransitionScopeOverlay
import com.zs.compose.theme.text.Label
import com.zs.toolz.common.LocalNavController
import com.zs.toolz.common.LocalSystemFacade
import com.zs.toolz.common.NavController
import com.zs.toolz.common.NightMode
import com.zs.toolz.common.Res
import com.zs.toolz.common.compose.NavigationBar
import com.zs.toolz.common.impl.UnitConverterViewModel
import com.zs.toolz.common.preference
import com.zs.toolz.common.vectorResource
import com.zs.toolz.converter.RouteUnitConverter
import org.koin.androidx.compose.koinViewModel
import com.zs.compose.theme.snackbar.SnackbarHostState as SnackbarController

private const val TAG = "Gallery"
private typealias NavGraphBuilder = (NavKey) -> NavEntry<NavKey>

// Use this build your nav-graph
// Define the navigation graph builder.
// This lambda maps each NavKey to its corresponding NavEntry (Composable screen).
private val navGraph: NavGraphBuilder = { key: NavKey ->
    when (key) {
        // --- App Intro Screen ---
        // If the route is AppIntro, show the AppIntro composable.
        RouteUnitConverter -> NavEntry(key) { RouteUnitConverter(koinViewModel<UnitConverterViewModel>()) }
        // --- Fallback ---
        // For any unhandled route, throw a TODO to indicate missing implementation.
        else -> TODO("${key} - Not Implemented yet!")
    }
}

@Composable
@NonRestartableComposable
context(activity: MainActivity)
fun Toolz(
    navController: NavController,
    controller: SnackbarController
) {
    // Environment & State Setup
    val clazz = calculateWindowSizeClass(activity = activity) // Screen size classification
    val entry = navController.active                          // Current navigation entry
    val backdrop = rememberBackdropLayer()

    // Resolve dark/light theme
    val isDarkTheme = run {
        val mode by preference(key = Res.key.night_mode_policy)
        when (mode) {
            NightMode.ENABLED -> true
            NightMode.DISABLED -> false
            NightMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        }
    }
    // Resolve accent color (dynamic if supported, fallback otherwise)
    val accent = run {
        val enabled by preference(Res.key.dynamic_colors)
        when {
            Res.app.isAtLeast(Build.VERSION_CODES.S) && enabled -> dynamicAccentColor(
                activity,
                isDarkTheme
            )

            isDarkTheme -> Res.app.color_accent_dark
            else -> Res.app.color_accent_light
        }
    }

    // Determine Navigation Bar Visibility
    val isNavBarRequired = entry is RouteUnitConverter
    // Navigation Bar Definition
    val navBar: @Composable () -> Unit = {
        NavigationBar(
            backdrop = backdrop,
            elevation = 8.dp,
            shape = AppTheme.shapes.xLarge,
            modifier = Modifier.renderInSharedTransitionScopeOverlay(0.3f),
            content = {
                BottomNavigationItem(
                    selected = false,
                    onClick = {},
                    icon = {
                        Icon(
                            vectorResource(Res.drawable.ic_straighten_outline),
                            contentDescription = null
                        )
                    },
                    label = { Label(textResource(Res.string.units)) }
                )

                BottomNavigationItem(
                    selected = false,
                    onClick = {},
                    icon = {
                        Icon(
                            vectorResource(Res.drawable.ic_tools_level_outline),
                            contentDescription = null
                        )
                    },
                    label = { Label(textResource(Res.string.units)) }
                )

                BottomNavigationItem(
                    selected = false,
                    onClick = {},
                    icon = {
                        Icon(
                            vectorResource(Res.drawable.ic_chat_outline),
                            contentDescription = null
                        )
                    },
                    label = { Label(textResource(Res.string.chatbot)) }
                )

                BottomNavigationItem(
                    selected = true,
                    onClick = {},
                    icon = {
                        Icon(
                            vectorResource(Res.drawable.ic_explore_outline),
                            contentDescription = null
                        )
                    },
                    label = { Label(textResource(Res.string.compass)) }
                )
            }
        )
    }
    // Main Content Scaffold
    val content: @Composable () -> Unit = {
        NavigationSuiteScaffold(
            vertical = true,
            containerColor = AppTheme.colors.background,
            progress = activity.inAppUpdateProgress,
            snackbarHostState = controller,
            hideNavigationBar = !isNavBarRequired,
            content = {
                NavDisplay(
                    backStack = navController.backstack,
                    onBack = navController::navigateUp,
                    modifier = Modifier.layerBackdropProvider(backdrop),
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = navGraph
                )
            },
            navBar = navBar
        )
    }
    // Apply Theme + Composition Locals
    AppTheme(
        isLight = !isDarkTheme,
        fontFamily = FontFamily.Default,
        motionScheme = MotionScheme.expressive(),
        accent = accent,
        content = {
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalSystemFacade provides activity,
                LocalWindowSize provides when {
                    !isNavBarRequired -> clazz
                    // portrait -> clazz.consume(56.dp) // Adjust for bottom nav
                    else -> clazz.consume(100.dp)   // Adjust for nav rail
                },
                content = content
            )
        }
    )
}