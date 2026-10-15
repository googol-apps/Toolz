package com.zs.toolz.level

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoundEffect
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.decorator.decorator
import com.zs.compose.foundation.unit.EdgeInsets
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Button
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.text.Label
import com.zs.preferences.Preferences
import com.zs.preferences.longPreferenceKey
import com.zs.toolz.common.Res
import com.zs.toolz.common.preference
import com.zs.toolz.level.Tilt.Companion.of
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import kotlin.math.hypot

/** Plays a short click sound and a haptic tick. Call [release] when done. */
class LevelFeedback(context: Context) {
    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= 31)
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        else context.getSystemService(Vibrator::class.java)

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    // Short (<1s) sound in res/raw/level_click.ogg
    private val soundId = soundPool.load(context, Res.raw.interface_device_click, 1)
    private var loaded = false

    init {
        soundPool.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
    }

    fun play() {
        if (loaded) soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        val effect = if (Build.VERSION.SDK_INT >= 29)
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        else
            VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    fun release() = soundPool.release()
}

@Composable
fun rememberLevelFeedback(): LevelFeedback {
    val context = LocalContext.current.applicationContext
    val feedback = remember { LevelFeedback(context) }
    DisposableEffect(feedback) { onDispose { feedback.release() } }
    return feedback
}

// Configuration constants for the precision engine
private const val PERFECT_LEVEL_DEG = 0.2
private const val MAX_TRACKED_DEG = 15.0 // The extreme point considered 0% alignment accuracy

private val KEY_CALIBRATION = longPreferenceKey("level_calibration", 0)
private const val TAG = "RouteLevel"

@Serializable
object RouteLevel: NavKey {

    @Composable
    @NonRestartableComposable
    operator fun invoke() {

        val rawTilt by rememberDeviceTilt(filterWindowMs = 500)
        val tilt = rawTilt of Tilt
        val preferences: Preferences = koinInject()
        val rawCalibrated by preference(KEY_CALIBRATION)
        val raw = rawCalibrated of Tilt

        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.content)) {
            val feedback = rememberLevelFeedback()

            // Combined distance from level on both axes, in degrees. NaN when there's no sensor.
            val deviation by rememberUpdatedState(
                if (tilt.isSpecified) hypot(tilt.pitch, tilt.roll) else Float.NaN
            )
            LocalSoundEffect.current

            // Percentage-driven Geiger audio loop engine
            LaunchedEffect(feedback) {
                var hitPerfectLock = false

                while (true) {
                    val d = deviation.toDouble()

                    if (d.isNaN()) {
                        delay(200)
                        continue
                    }

                    // 1. Calculate accuracy percentage (0.0% to 100.0%)
                    // Perfect level (<=0.2) scales up to 1.0 (100%). Extreme tilt (>=15.0) scales down to 0.0 (0%).
                    val alignmentProgress = (1.0 - (d / MAX_TRACKED_DEG)).coerceIn(0.0, 1.0)
                    val alignmentPercentage = alignmentProgress * 100.0

                    when {
                        // Perfect level configuration
                        d <= PERFECT_LEVEL_DEG -> {
                            if (!hitPerfectLock) {
                                hitPerfectLock = true
                                feedback.play()
                            }
                            delay(200)
                        }

                        // Play sound ONLY if alignment accuracy enters 50% or higher
                        alignmentPercentage >= 50.0 -> {
                            hitPerfectLock = false
                            feedback.play()

                            // Dynamic delay mapping based strictly on the current alignment range:
                            // 50% alignment accuracy -> 500ms delay between clicks (slow)
                            // 100% alignment accuracy -> 60ms delay between clicks (very rapid)
                            val speedProgress = (alignmentPercentage - 50.0) / (100.0 - 50.0)
                            val dynamicDelay = 500 - (speedProgress * (500 - 60))

                            delay(dynamicDelay.toLong().coerceIn(60, 500))
                        }

                        // Completely mute if the phone alignment drops below 50%
                        else -> {
                            hitPerfectLock = false
                            delay(150)
                        }
                    }
                }
            }

            val calibrated = tilt - raw
            Log.d(TAG, "invoke: tilt: $tilt $calibrated")
            Column (modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                TubeLevel({ tilt }, orientation = Orientation.Horizontal, modifier = Modifier.decorator(
                    backgroundColor = AppTheme.colors.background(10.dp),
                    elevation = 21.dp,
                    shape = CircleShape,
                    edgeInsets = EdgeInsets(horizontal = 20.dp, vertical = 20.dp),
                    noiseAlpha = 0.5f
                ))

                Row() {
                    SurfaceLevel({ tilt }, tint = Color.Black, contentColor = AppTheme.colors.accent, modifier = Modifier.decorator(
                        backgroundColor = AppTheme.colors.background(10.dp),
                        elevation = 21.dp,
                        shape = CircleShape,
                        edgeInsets = EdgeInsets(horizontal = 20.dp, vertical = 20.dp),
                        noiseAlpha = 0.5f
                    ))

                    TubeLevel({ tilt }, orientation = Orientation.Vertical, tint = AppTheme.colors.accent, contentColor = Color.Black, modifier = Modifier.decorator(
                        backgroundColor = AppTheme.colors.background(10.dp),
                        elevation = 21.dp,
                        shape = CircleShape,
                        edgeInsets = EdgeInsets(horizontal = 20.dp, vertical = 20.dp),
                        noiseAlpha = 0.5f
                    ))
                }
            }
            Button(onClick = { preferences[KEY_CALIBRATION] = rawTilt}) {
                Label("Calibrate")
            }
        }
    }
}
