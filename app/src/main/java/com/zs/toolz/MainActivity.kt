@file:OptIn(ExperimentalFoundationApi::class)

package com.zs.toolz

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.zs.compose.foundation.getText2
import com.zs.compose.theme.snackbar.SnackbarDuration
import com.zs.domain.analytics.Analytics
import com.zs.domain.util.showPlatformToast
import com.zs.preferences.Preferences
import com.zs.toolz.common.NavController
import com.zs.toolz.common.Res
import com.zs.toolz.common.SystemFacade
import com.zs.toolz.common.versionCodeCompat
import com.zs.toolz.converter.UnitConverter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen as configSplashScreen
import com.zs.compose.theme.snackbar.SnackbarHostState as SnackbarController

class MainActivity : ComponentActivity(), SystemFacade {
    // TAG - for debugging purpose only
    private val TAG = "MainActivity"

    val preferences: Preferences by inject()
    val controller: SnackbarController by inject()
    private var navController: NavController? = null
    private val analytics: Analytics by inject()


    var inAppUpdateProgress by mutableFloatStateOf(Float.NaN)
        private set

    override fun showToast(message: String, duration: Int) = showPlatformToast(message, duration)
    override fun showToast(message: Int, duration: Int) = showPlatformToast(message, duration)
    override fun <T> getDeviceService(name: String): T = getSystemService(name) as T
    override fun launch(intent: Intent, options: Bundle?) = startActivity(intent, options)

    override fun showSnackbar(
        message: CharSequence,
        icon: ImageVector?,
        accent: Color,
        duration: SnackbarDuration,
    ) {
        lifecycleScope.launch {
            controller.showSnackbar(
                message = message,
                icon = icon,
                accent = accent,
                duration = duration
            )
        }
    }

    override fun showSnackbar(
        message: Int,
        icon: ImageVector?,
        accent: Color,
        duration: SnackbarDuration,
    ) = showSnackbar(
        resources.getText2(id = message),
        icon = icon,
        accent = accent,
        duration = duration
    )

    override fun initiateUpdateFlow(report: Boolean) {
        TODO("Not yet implemented")
    }

    override fun initiateReviewFlow() {
        TODO("Not yet implemented")
    }

    override fun initiatePurchaseFlow(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun restart(global: Boolean) {
        // Get the launch intent for the app's main activity
        val packageManager = packageManager
        val intent = packageManager.getLaunchIntentForPackage(packageName)

        // Ensure the intent is not null
        if (intent == null) {
            //analytics.logEvent()
            Log.e("AppRestart", "Unable to restart: Launch intent is null")
            return
        }

        // Get the main component for the restart task
        val componentName = intent.component
        if (componentName == null) {
            Log.e("AppRestart", "Unable to restart: Component name is null")
            return
        }
        // Create the main restart intent and start the activity
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        startActivity(mainIntent)
        // Terminate the current process to complete the restart
        if (global) Runtime.getRuntime().exit(0)
        finish()
    }

    override fun configSystemBars(
        isLightAppearance: Boolean,
        visible: Boolean,
        isBgTransparent: Boolean
    ) {
        // Obtain a controller to manage system bar visibility and appearance
        val controller = WindowCompat.getInsetsController(window, window.decorView)

        // Apply light/dark icon appearance for both navigation and status bars
        controller.isAppearanceLightNavigationBars = isLightAppearance
        controller.isAppearanceLightStatusBars = isLightAppearance

        // Configure background color or contrast enforcement depending on API level
        if (Build.VERSION.SDK_INT >= 29) {
            // On Android 10+, enforce contrast if background is not transparent
            window.setStatusBarContrastEnforced(!isBgTransparent)
            window.setNavigationBarContrastEnforced(!isBgTransparent)
        } else {
            // On older versions, fallback to setting a semi-transparent scrim or fully transparent color
            val color = if (isBgTransparent) Color.Transparent else Color(0x20000000)
            window.navigationBarColor = color.toArgb()
            window.statusBarColor = color.toArgb()
        }

        if (!visible) {
            // Hide system bars and allow them to be revealed temporarily by swipe
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            return
        }

        // Show system bars with default behavior
        controller.show(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Determine if this is a fresh launch (no saved state from rotation or process recreation)
        val isFreshLaunch = savedInstanceState == null
        if (isFreshLaunch) {
            // Show splash screen only on a fresh launch
            configSplashScreen()

            // TODO - ⚠️ NOTE: We currently rely on versionCode from PackageManager.
            // Monitor this carefully, as Play Console may raise issues related
            // to permission handling when querying app packages.
            lifecycleScope.launch {
                val info = packageManager.getPackageInfo(packageName, 0)

                // Compare stored version code with the current one.
                // If different, update preferences and notify user with release notes.
                if (info.versionCodeCompat != preferences[Res.key.app_version_code]) {
                    preferences[Res.key.app_version_code] = info.versionCodeCompat
                    // Show release notes snackbar to highlight new version changes.
                    showSnackbar(
                        message = Res.string.release_notes,
                        accent = Color.Unspecified,
                        icon = null,
                        duration = SnackbarDuration.Long
                    )

                    // Optionally log analytics event for release notes display.
                    // analytics.logEvent("release_notes")

                    return@launch
                }
            }
        }

        // Ensure window fits system windows (status bar, navigation bar).
        // This is usually configured in the app theme, but enforced here for consistency.
        WindowCompat.enableEdgeToEdge(window)

        // Create navigator controller.
        // Decide initial route based on intent, permissions, and authentication requirements.
        if (navController == null || isFreshLaunch) {
            // Decide initial route based on launch context, permissions, and authentication:
            // - Deep link or external intent → IntentViewer
            // - Missing required permissions → Onboarding
            // - Authentication required → ScreenLock
            // - Default case → Files browser
            navController = NavController(UnitConverter)
            // Set the main UI content with navigator and controller
            setContent { Toolz(navController!!, controller) }
        }
    }
}