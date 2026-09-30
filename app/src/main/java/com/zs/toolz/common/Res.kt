@file:Suppress("ClassName", "EnumEntryName", "ConstPropertyName")
@file:OptIn(ExperimentalSerializationApi::class)

package com.zs.toolz.common

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.zs.compose.foundation.OliveYellow
import com.zs.preferences.IntSaver
import com.zs.preferences.StringSaver
import com.zs.preferences.booleanPreferenceKey
import com.zs.preferences.intPreferenceKey
import com.zs.preferences.longPreferenceKey
import com.zs.preferences.stringPreferenceKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf

// Enforce rule - all const in here are are snake_case.
@Suppress("SameParameterValue")
private fun Intent(action: String, data: Uri) = Intent(action).apply { this.data = data }

/**
 * A generic [Saver] implementation for any [Enum] type.
 *
 * This saver persists enum values by storing their [Enum.name] as a String,
 * and restores them by looking up the corresponding enum constant.
 *
 * Usage:
 * ```
 * val saver = enumSaver<MyEnum>()
 * val state = rememberSaveable(stateSaver = saver) { mutableStateOf(MyEnum.FIRST) }
 * ```
 *
 * This avoids writing custom savers for each enum class.
 */
@Suppress("FunctionName")
private inline fun <reified T : Enum<T>> OrdinalEnumSaver(): IntSaver<T> = object : IntSaver<T> {
    // Save enum as its name string
    override fun save(value: T): Int = value.ordinal

    // Restore enum constant by ordinal lookup
    override fun restore(value: Int): T =
        enumValues<T>()[value]
}

// --- Color saver utility ---
// Used to persist and restore Color values in preferences.
private val ColorSaver = object : IntSaver<Color> {
    override fun restore(value: Int): Color = Color(value)
    override fun save(value: Color): Int = value.toArgb()
}

/**
 * The standard [GoogleFont.Provider] utilized to asynchronously download
 * and cache fonts securely via Google Play Services.
 */
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = Res.array.com_google_android_gms_fonts_certs
)

/**
 * Creates a [FontFamily] from the given Google Font name.
 *
 * @param name The name of theGoogle Font to use.
 * @return A [FontFamily] object
 */
@Stable
private fun FontFamily(name: String): FontFamily {
    // Create a GoogleFont object from the given name.
    val font = GoogleFont(name)
    // Create a FontFamily object with four different font weights.
    return FontFamily(
        Font(fontProvider = provider, googleFont = font, weight = FontWeight.Light),
        Font(fontProvider = provider, googleFont = font, weight = FontWeight.Medium),
        Font(fontProvider = provider, googleFont = font, weight = FontWeight.Normal),
        Font(fontProvider = provider, googleFont = font, weight = FontWeight.Bold),
    )
}

// Pre-configured global FontFamily instances.
private val OutfitFontFamily = FontFamily("Outfit")
private val RobotoFontFamily = FontFamily("Roboto")
private val DancingScriptFontFamily = FontFamily("Dancing Script")
private val GeomFontFamily = FontFamily("Geom")
private val Orbitron = FontFamily("Orbitron")

// Extension properties providing seamless access to the app's standard typefaces.
val FontFamily.Companion.outfit get() = OutfitFontFamily
val FontFamily.Companion.geom get() = GeomFontFamily
val FontFamily.Companion.dancing_script get() = DancingScriptFontFamily
val FontFamily.Companion.roboto get() = RobotoFontFamily
val FontFamily.Companion.default inline get() = Default
val FontFamily.Companion.orbitron get() = Orbitron


// Pre-configured global spacing arrangements using common spacing dimensions.
private val LargeArrangement = Arrangement.spacedBy(Res.space.large)
private val SmallArrangement = Arrangement.spacedBy(Res.space.small)
private val xSmallArrangement = Arrangement.spacedBy(Res.space.x_small)
private val mediumArrangement = Arrangement.spacedBy(Res.space.medium)

// Extension properties for quickly applying spacing arrangements in Compose layouts.
val Arrangement.gap_large get() =  LargeArrangement
val Arrangement.gap_small get() =  SmallArrangement
val Arrangement.gap_x_small get() =  xSmallArrangement
val Arrangement.gap_medium get() =  mediumArrangement


/**
 * Immutable configuration model for application-wide behavior flags and UI preferences.
 *
 * This configuration is serialized using [kotlinx.serialization] to support persistence
 * and transfer across components. Each property defines a specific feature toggle or
 * adjustment that influences runtime behavior and user experience.
 *
 * @property isBackgroundBlurEnabled Strategy flag to enable background blur effects.
 *                                    Defaults to `true` on Android 12 (S) and above.
 * @property isTrashCanEnabled Determines if the trash can feature for deleted media is enabled.
 *                             When enabled, deleted items are moved to a temporary bin.
 * @property fontScale Specifies the font scaling factor for the application.
 *                     A value of `-1f` indicates system default scaling.
 * @property isSplashAnimWaitEnabled If true, waits for the splash screen animation
 *                                   to finish before proceeding to the main UI.
 */
@Stable
@Serializable
data class AppConfig(
    @JvmField val isBackgroundBlurEnabled: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    @JvmField val isTrashCanEnabled: Boolean = true,
    @JvmField val fontScale: Float = -1f,
    @JvmField val isSplashAnimWaitEnabled: Boolean = false,
)

/**
 * Common access point for app-level constants and resources.
 *
 * Mirrors the naming convention of Android's [R] class but provides a more
 * flexible, centralized extension. Inspired by Kotlin Multiplatform patterns,
 * this reduces direct dependency on the generated [R] class, which is often
 * cumbersome to access during typing.
 * @see string
 * @see drawable
 * @see app
 * @see shape
 * @see action
 * @see space
 */
object Res {

    // Typealiases for direct access to Android resources (R.string, R.drawable, etc.)
    typealias string = com.zs.toolz.R.string
    typealias drawable = com.zs.toolz.R.drawable
    typealias array = com.zs.toolz.R.array
    typealias font = FontFamily.Companion
    typealias layout = Arrangement
    // typealias raw = com.zs.toolz.R.raw
    // typealias plurals = com.zs.toolz.R.plurals

    /**
     * Manifest-related constants and intents.
     *
     * Provides URIs, package names, default colors, and permission lists
     * required for app configuration and external navigation.
     */
    object app {
        // Play Store URIs
        const val market_uri_prefix = "market://details?id="
        const val market_web_url_prefix = "http://play.google.com/store/apps/details?id="
        const val market_package = "com.android.vending"

        // Predefined external intents
        val intent_privacy_policy = Intent(
            Intent.ACTION_VIEW,
            "https://docs.google.com/document/d/1D9wswWSrt65ol7h3HLKhk31OVTqDtN4uLJ73_Rk9hT8/edit?usp=sharing".toUri()
        )
        val intent_github_issues =
            Intent(Intent.ACTION_VIEW, "https://github.com/googol-apps/Toolz/issues".toUri())
        val intent_telegram =
            Intent(Intent.ACTION_VIEW, "https://t.me/audiofy_support".toUri())
        val intent_github =
            Intent(Intent.ACTION_VIEW, "https://github.com/googol-apps/Toolz".toUri())
        val intent_join_beta = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/apps/testing/com.zs.toolz/join".toUri()
        )

        // --- Default Accent Colors ---
        val color_accent_light = Color(0xFF904A42)
        val color_accent_dark = Color.OliveYellow

        /**
         * Permissions required by the app.
         *
         * Dynamically built based on Android version to ensure compatibility
         * with scoped storage and legacy storage access.
         */
        @SuppressLint("BuildListAdds")
        val permissions = buildList {
            // Scoped storage permissions for Android 13 (Tiramisu) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                this += android.Manifest.permission.ACCESS_MEDIA_LOCATION
                this += android.Manifest.permission.READ_MEDIA_VIDEO
                this += android.Manifest.permission.READ_MEDIA_IMAGES
            }
            // Visual media permission for Android 14 (Upside Down Cake) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                this += android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            // Legacy storage permissions for Android 10 (Q) and below
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
                this += android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                this += android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }

        /**
         * Utility to check if the current SDK version is at least [api].
         */
        @ChecksSdkIntAtLeast(parameter = 0)
        fun isAtLeast(api: Int) = Build.VERSION.SDK_INT >= api
    }

    /**
     * Preference keys used throughout the app.
     *
     * Provides strongly typed keys for storing and retrieving app settings.
     */
    object key {
        val night_mode_policy =
            intPreferenceKey("_night_mode", NightMode.FOLLOW_SYSTEM, OrdinalEnumSaver())
        val transparent_system_bars = booleanPreferenceKey(
            "_transparent_system_bars",
            defaultValue = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        )
        val app_launch_counter = intPreferenceKey("_launch_counter", 0)
        val dynamic_colors =
            booleanPreferenceKey("_dynamic_colors", app.isAtLeast(Build.VERSION_CODES.S))
        val dark_accent_color =
            intPreferenceKey("_dark_accent_color", app.color_accent_dark, ColorSaver)
        val light_accent_color =
            intPreferenceKey("_light_accent_color", app.color_accent_light, ColorSaver)
        val app_version_code = longPreferenceKey("_app_version_code", -1)
        val app_config = stringPreferenceKey(
            "_app_config",
            object : StringSaver<AppConfig> {
                override fun restore(value: String): AppConfig {
                    // ⬇️ Step 1: Decode the Base64 string back into raw bytes
                    val arr = Base64.decode(value, Base64.DEFAULT)

                    // ⬇️ Step 2: Deserialize the bytes into an AppConfig instance using ProtoBuf
                    return ProtoBuf.decodeFromByteArray(AppConfig.serializer(), arr)
                }

                override fun save(value: AppConfig): String {
                    // ⬇️ Step 1: Serialize the AppConfig instance into raw bytes using ProtoBuf
                    val encoded = ProtoBuf.encodeToByteArray(AppConfig.serializer(), value)

                    // ⬇️ Step 2: Encode the bytes into a Base64 string for safe storage in preferences
                    val arr = Base64.encodeToString(encoded, Base64.DEFAULT)

                    // ⬇️ Step 3: Return the string so DataStore/Prefs can persist it
                    return arr
                }
            }
        )
    }

    /**
     * Standardized dimension scale for consistent UI spacing, margins, and padding.
     *
     * @property x_small Smallest default padding constraint (4.dp).
     * @property small Minor padding block (8.dp).
     * @property medium Standard mid-level spacing boundary (12.dp).
     * @property normal Default large container padding (16.dp).
     * @property large Prominent separation between major layout groupings (22.dp).
     * @property x_large Extreme spacing for hero-level offsets (32.dp).
     */
    object space {
        val x_small: Dp = 4.dp
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val normal: Dp = 16.dp
        val large: Dp = 22.dp
        val x_large: Dp = 32.dp
    }

    /**
     * Common access to Compose shapes.
     */
    object shape {
        // Used to style individual items within a column section.
        val section = RoundedCornerShape(20.dp)
        val section_top = RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp)
        val section_middle = RectangleShape
        val section_bottom = RoundedCornerShape(0.dp, 0.dp, 20.dp, 20.dp)

        val circle = CircleShape
        val rectangle = RectangleShape
    }

    /**
     * Common access app level configs.
     */
    var config = AppConfig()
}