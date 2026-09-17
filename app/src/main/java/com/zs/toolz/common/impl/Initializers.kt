package com.zs.toolz.common.impl

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.zs.compose.theme.snackbar.SnackbarHostState
import com.zs.domain.analytics.Analytics
import com.zs.preferences.Preferences
import com.zs.toolz.common.AppConfig
import com.zs.toolz.common.Res
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private const val TAG = "Initializers"
private const val PREFERENCES_STORE_NAME = "preferences_db"

// AppInitializer configures core services and sets up Koin dependency injection at app startup.
// It ensures analytics, media handling, and DI modules are ready before the app runs.
class AppInitializer : Initializer<Unit> {

    // This initializer does not depend on any other initializers.
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    override fun create(context: Context) {
        // --- Core Service Initialization ---
        // Prepare essential services that the app relies on:
        //   • Analytics: sets up event tracking and reporting
        //   • MediaProvider: configures access to media files and streams
        Analytics.initialize(context)
        Preferences.initialize(context)

        val preferences = Preferences.getInstance()  // Initialize Preferences
        // Retrieve the current launch counter value, defaulting to 0 if not set
        // Read current launch counter, increment for cold start, and log the updated value
        val key = Res.key.app_launch_counter
        preferences[key] += preferences[key]
        Log.d(TAG, "Cold start counter: ${preferences[key]}")
        // --- Configuration Restore ---
        // Restore application configuration flags and settings from persisted preferences.
        Res.config = preferences[Res.key.app_config] ?: AppConfig()

        // --- Dependency Injection Setup (Koin) ---
        // Start the Koin DI framework with:
        //   • Android context for resource access
        //   • Application modules that define bindings and providers
        // This makes dependencies available throughout the app.
        startKoin {
            androidContext(context)
            modules(AppModules)
        }
    }
}

// TODO - Move this logic of initializing pref to Preference DataStore.
// Volatile ensures visibility of changes across threads
@Volatile
private var INSTANCE: Preferences? = null

/**
 * Initialize the Preferences singleton.
 *
 * This method sets up the INSTANCE if it hasn't been created yet.
 * It does not return the instance — use [getInstance] to access it.
 *
 * @param context Context used to obtain the Application reference.
 */
fun Preferences.Companion.initialize(context: Context) {
    // If INSTANCE already exists, do nothing.
    if (INSTANCE != null) return
    synchronized(this) {
        if (INSTANCE == null) {
            // Ensure we are working with an Application context.
            val appContext = context.applicationContext
            require(appContext is Application) {
                "Context must be an Application"
            }

            // Create and assign the singleton instance.
            INSTANCE = Preferences(appContext, PREFERENCES_STORE_NAME)
        }
    }
}

/**
 * Retrieve the Preferences instance.
 * Throws if [initialize] has not been called yet.
 */
fun Preferences.Companion.getInstance(): Preferences {
    return INSTANCE ?: throw IllegalStateException(
        "Preferences must be initialized before use"
    )
}

// AppModules defines the dependency injection graph for core application services.
// Each binding specifies how instances are created and shared across the app.
private val AppModules = module {

    // --- Preferences Store ---
    // Provide a single shared Preferences instance backed by the app's preference database.
    // This allows consistent access to persisted settings and state throughout the app.
    factory { Preferences.getInstance() }

    // --- Android Resources ---
    // Expose the Android Resources object via DI.
    // Useful for accessing strings, drawables, dimensions, and other resource values.
    factory { androidApplication().resources }

    // --- Snackbar Host State ---
    // Provide a single SnackbarHostState instance.
    // This manages the state of snackbars shown in Compose UI.
    singleOf(::SnackbarHostState)


    // --- ViewModels ---
    viewModelOf(::UnitConverterViewModel)
}