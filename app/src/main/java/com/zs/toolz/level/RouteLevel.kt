package com.zs.toolz.level

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Button
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.Text
import com.zs.preferences.Preferences
import com.zs.preferences.longPreferenceKey
import com.zs.toolz.common.preference
import com.zs.toolz.level.Tilt.Companion.of
import org.koin.compose.koinInject
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

private val KEY_CALIBRATION = longPreferenceKey("level_calibration", 0)
private const val TAG = "RouteLevel"
@Serializable
object RouteLevel: NavKey {

    @Composable
    @NonRestartableComposable
    operator fun invoke() {
        val rawTilt by rememberTilt(filterWindowMs = 500)
        val tilt = rawTilt of Tilt
        val preferences: Preferences =  koinInject()
        val rawCalibrated by preference(KEY_CALIBRATION)
        val (pitch, roll, spin) = rawCalibrated of Tilt
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.content)) {

            val calibrated = tilt.copy(pitch = tilt.pitch - pitch, roll = tilt.roll - roll, spin = tilt.spin - spin)
            Log.d(TAG, "invoke: tilt: $tilt $calibrated")
            BubbleLevel({ tilt }, modifier = Modifier.fillMaxSize())
            Button(onClick = { preferences[KEY_CALIBRATION] =  rawTilt}) {
                Label("Calibrate")
            }
        }
    }
}