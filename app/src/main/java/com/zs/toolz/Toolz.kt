package com.zs.toolz

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
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
import com.zs.toolz.converter.UnitConverter
import com.zs.toolz.settings.Settings
import org.koin.androidx.compose.koinViewModel
import com.zs.compose.theme.snackbar.SnackbarHostState as SnackbarController

private const val TAG = "Gallery"

// Use this build your nav-graph
// Define the navigation graph builder.
// This lambda maps each NavKey to its corresponding NavEntry (Composable screen).
private val NavGraphBuilder = { key: NavKey ->
    when (key) {
        // --- App Intro Screen ---
        // If the route is AppIntro, show the AppIntro composable.
        UnitConverter -> NavEntry(key) { UnitConverter(koinViewModel<UnitConverterViewModel>()) }
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

    // Resolve dark/light theme
    val isDarkTheme = run {
        val policy by preference(key = Res.key.night_mode_policy)
        when (policy) {
            NightMode.ENABLED -> true
            NightMode.DISABLED -> false
            NightMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        }
    }
    // Resolve accent color (dynamic if supported, fallback otherwise)
    val accent = run {
        val enabled by preference(Res.key.dynamic_colors)
        when {
            Res.app.isAtLeast(Build.VERSION_CODES.S) && enabled ->
                dynamicAccentColor(activity, isDarkTheme)

            isDarkTheme -> Res.app.color_accent_dark
            else -> Res.app.color_accent_light
        }
    }
    // Determine Navigation Bar Visibility
    val isNavBarRequired = entry is UnitConverter
    // Navigation Bar Definition
    val navBar: @Composable () -> Unit = {
        NavigationBar(
            vertical = clazz.width > clazz.height,
            elevation = 8.dp,
            modifier = Modifier.renderInSharedTransitionScopeOverlay(0.3f),
            content = {
                // Unit Converter
                BottomNavigationItem(
                    selected = entry is UnitConverter,
                    onClick = { navController.navigate(UnitConverter) },
                    label = { Label(textResource(Res.string.units)) },
                    icon = {
                        Icon(
                            vectorResource(Res.drawable.ic_straighten_outline),
                            contentDescription = null
                        )
                    },
                )

                // Level
                BottomNavigationItem(
                    selected = false,
                    onClick = { navController.navigate(Settings) },
                    label = { Label(textResource(Res.string.level)) },
                    icon = {
                        Icon(
                            vectorResource(Res.drawable.ic_tools_level_outline),
                            contentDescription = null
                        )
                    },
                )

                // Settings
                BottomNavigationItem(
                    selected = entry is Settings,
                    onClick = { navController.navigate(Settings) },
                    label = { Label(textResource(Res.string.settings)) },
                    icon = {
                        Icon(
                            vectorResource(Res.drawable.ic_settings_outline),
                            contentDescription = null
                        )
                    },
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
            hideNavigationBar = false,
            navBar = navBar,
            content = {
                NavDisplay(
                    backStack = navController.backstack,
                    onBack = navController::navigateUp,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = NavGraphBuilder
                )
            }
        )
    }
    // Apply Theme + Composition Locals
    AppTheme(
        isLight = !isDarkTheme,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        motionScheme = MotionScheme.expressive(),
        accent = accent,
        content = {
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalSystemFacade provides activity,
                LocalWindowSize provides when {
                    !isNavBarRequired -> clazz
                    clazz.height > clazz.width -> clazz.consume(56.dp) // Adjust for bottom nav
                    else -> clazz.consume(100.dp)   // Adjust for nav rail
                },
                content = content
            )
        }
    )
    SideEffect(isDarkTheme) {
        activity.configSystemBars(!isDarkTheme, true, true)
    }
}