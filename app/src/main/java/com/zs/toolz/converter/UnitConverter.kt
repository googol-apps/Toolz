package com.zs.toolz.converter

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.backdrop.haze.BlurConfig
import com.zs.compose.foundation.backdrop.haze.hazeEffect
import com.zs.compose.foundation.backdrop.rememberScreenBackdrop
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Icon
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.adaptive.HorizontalTwoPaneStrategy
import com.zs.compose.theme.adaptive.TwoPane
import com.zs.compose.theme.adaptive.VerticalTwoPaneStrategy
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.appbar.TopAppBar
import com.zs.compose.theme.text.Label
import com.zs.domain.math.UnifiedReal
import com.zs.toolz.common.Res
import com.zs.toolz.common.vectorResource
import kotlinx.serialization.Serializable
import androidx.compose.ui.unit.IntOffset as LabelIcon

private val Default = UnifiedReal.ZERO to  AnnotatedString("")

@Serializable
object UnitConverter : NavKey {
    const val KEY_AREA_CONVERTER = "_area"
    const val KEY_ANGLE_CONVERTER = "_angle"
    const val KEY_LENGTH_CONVERTER = "_length"
    const val KEY_MASS_CONVERTER = "_mass"
    const val KEY_VOLUME_CONVERTER = "_volume"
    const val KEY_TIME_CONVERTER = "_time"
    const val KEY_TEMPERATURE_CONVERTER = "_temperature"
    const val KEY_PRESSURE_CONVERTER = "_pressure"
    const val KEY_ENERGY_CONVERTER = "_energy"
    const val KEY_POWER_CONVERTER = "_power"
    const val KEY_SPEED_CONVERTER = "_speed"
    const val KEY_DATA_CONVERTER = "_data"

    /**
     * @return lable and icon for the converter as [LabelIcon]
     */
    fun converterAssetsForKey(key: String): LabelIcon {
        return LabelIcon(Res.string.angle, Res.drawable.ic_angle)
    }


    @Composable
    @NonRestartableComposable
    operator fun invoke(viewState: UnitConverterViewState) {
        val window = WindowInsets.content
        val backdrop = rememberScreenBackdrop(
            painter = rememberAsyncImagePainter(
                "https://as2.ftcdn.net/jpg/07/15/46/61/1000_F_715466186_xpLznUuqiA5ASGG9DcxaxPcdkN6RfTl3.jpg",

                onState = {
                    if (it is AsyncImagePainter.State.Error)
                    Log.d("UnitConverter", "invoke: ${it.result.throwable.stackTraceToString()}")
                }
            )
        )

        // Retrieve the current window size
        val (width, height) = LocalWindowSize.current
        // Determine the two-pane strategy based on window width range
        // when in mobile portrait; we don't show second pane;
        val strategy = when {
            width < height -> VerticalTwoPaneStrategy(0.60f)
            else -> HorizontalTwoPaneStrategy(0.6f) // Use horizontal layout with 55% split for large screens
        }
        TwoPane(
            topBar = {
                TopAppBar(
                    title = {
                        Label(
                            textResource(Res.string.scr_unit_converter_title),
                            fontWeight = FontWeight.Light,
                            maxLines = 2
                        )
                    },
                    background = Background(Color.Transparent),
                    navigationIcon = {
                        Icon(
                            vectorResource(Res.drawable.ic_handyman),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
            },
            primary = {
                Column(modifier = Modifier
                    .windowInsetsPadding(WindowInsets.content.only(WindowInsetsSides.Top))
                    .padding(Res.dimen.normal).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Res.dimen.small)
                ) {
                    Converters(
                        viewState.converter,
                        onRequestCheck = {viewState.converter = it},
                        modifier = Modifier
                    )

                    ValueField(
                        viewState.value,
                        expanded = false,
                        onRequestCollapse = {},
                    )
                    val result by viewState.result.collectAsState(Default)
                    val (conversion, more) = result
                    ResultField(
                        conversion.toStringTruncated(12),
                        expanded = false,
                        onRequestCollapse = {},
                    )
                }
            },
            secondary = {
                NumPad(
                    onKeyPress = viewState.onKeyPress,
                    modifier = Modifier
                        .windowInsetsPadding(window.union(WindowInsets.navigationBars)).padding(horizontal = Res.dimen.normal)
                        .clip(AppTheme.shapes.large)
                )
            },
            containerColor = Color.Transparent,
            strategy = strategy,
            modifier = Modifier.hazeEffect(
                backdrop =backdrop,
                Color.Transparent,
                blurConfig = BlurConfig(0.3f, 25f),
                vibrancy = 1.4f,
                noiseAmount = 0.3f,
                tint = AppTheme.colors.background.copy(0.94f),
            )
        )
    }
}