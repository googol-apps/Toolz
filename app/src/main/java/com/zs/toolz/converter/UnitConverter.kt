package com.zs.toolz.converter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Icon
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.TonalIconButton
import com.zs.compose.theme.adaptive.HorizontalTwoPaneStrategy
import com.zs.compose.theme.adaptive.TwoPane
import com.zs.compose.theme.adaptive.VerticalTwoPaneStrategy
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.appbar.TopAppBar
import com.zs.compose.theme.drawHorizontalDivider
import com.zs.compose.theme.text.Header
import com.zs.compose.theme.text.Label
import com.zs.domain.math.NumUtil
import com.zs.domain.math.UnifiedReal
import com.zs.toolz.common.Res
import com.zs.toolz.common.vectorResource
import kotlinx.serialization.Serializable
import androidx.compose.foundation.layout.WindowInsetsSides.Companion as WIS

private val FieldPadding = Modifier.padding(horizontal = Res.space.x_large)
private val CompactFieldPadding = Modifier.padding(horizontal = Res.space.small)

private val PortraitStrategy =  VerticalTwoPaneStrategy(0.60f)
private val LandscapeStrategy =  HorizontalTwoPaneStrategy(0.55f)
private val DefaultExtras = UnifiedReal.ZERO to AnnotatedString("")
private val AboutEqualsPadding = PaddingValues(start = Res.space.large, end = Res.space.large, top = Res.space.medium)

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

    const val MAX_ALLOWED_CHARS = 12 // max input length

    @Composable
    @NonRestartableComposable
    operator fun invoke(viewState: UnitConverterViewState) {
        val navBarInsets = WindowInsets.content
        // Retrieve the current window size
        // Determine the two-pane strategy based on window width range
        // when in mobile portrait; we don't show second pane;
        val (width, height) = LocalWindowSize.current
        val strategy = if (width < height) PortraitStrategy else LandscapeStrategy
        val portrait = strategy == PortraitStrategy
        TwoPane(
            spacing = 0.dp,
            strategy = strategy,
            secondary = {
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier.windowInsetsPadding(
                        navBarInsets.union(WindowInsets.navigationBars).only(
                            WIS.Bottom + WIS.End
                        ).run { if (!portrait) add(WindowInsets(bottom = Res.space.normal)) else this }
                    )
                        .padding(horizontal = if (portrait) Res.space.x_large else Res.space.small)
                        .fillMaxSize(),

                    content = {
                        NumPad(
                            onKeyPress = viewState::onKeyPress,
                            modifier = Modifier
                                .clip(AppTheme.shapes.xLarge)
                        )
                    }
                )
            },
            topBar = {
                TopAppBar(
                    background = Background(Color.Transparent),
                    modifier = Modifier
                        .drawHorizontalDivider(
                            thickness = 1.dp,
                            color = AppTheme.colors.onBackground,
                            indent = DpOffset(Res.space.x_large, Res.space.x_large)
                        )
                        .padding(bottom = 5.dp),
                    navigationIcon = {
                        Icon(
                            vectorResource(Res.drawable.ic_handyman),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    title = {
                        Label(
                            textResource(Res.string.scr_unit_converter_title),
                            fontWeight = FontWeight.Light,
                            maxLines = 2
                        )
                    },
                    actions = {
                        // copy to clipBoard
                        TonalIconButton(onClick = viewState::copy, shape = AppTheme.shapes.small) {
                            Icon(
                                vectorResource(Res.drawable.ic_assignment_outline),
                                contentDescription = null
                            )
                        }
                        Spacer(Modifier.width(Res.space.medium))
                        // swap units
                        TonalIconButton(onClick = viewState::swap) {
                            Icon(
                                vectorResource(Res.drawable.ic_swap_calls),
                                contentDescription = null
                            )
                        }
                        Spacer(Modifier.width(Res.space.medium))
                    }
                )
            },
            primary = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Res.space.small),
                    modifier = Modifier
                        .windowInsetsPadding(
                            WindowInsets.content.union(navBarInsets).only(WIS.Top + WIS.Start)
                        )
                        .padding(vertical = Res.space.normal )
                        .fillMaxSize(),
                    content = {
                        Converters(
                            viewState.converter,
                            onRequestCheck = { viewState.converter = it },
                            modifier = if (portrait) FieldPadding else CompactFieldPadding
                        )

                        ValueField(
                            viewState,
                            modifier = if (portrait) FieldPadding else CompactFieldPadding
                        )

                        var result by remember { mutableStateOf(DefaultExtras, neverEqualPolicy()) }
                        LaunchedEffect(Unit) {
                            viewState.result.collect {
                                result = it
                            }
                        }
                        val (conversion, more) = result
                        ResultField(
                            NumUtil.doubleToString(conversion.doubleValue(), MAX_ALLOWED_CHARS, 2)!!,
                            viewState,
                            modifier = if (portrait) FieldPadding else CompactFieldPadding,
                        )
                        if (!portrait) {
                            return@Column
                        }

                        Header(
                            stringResource(Res.string.about_equals),
                            style = AppTheme.typography.display3,
                            contentPadding = AboutEqualsPadding,
                        )

                        Label(
                            more,
                            style = AppTheme.typography.label3,
                            modifier = FieldPadding.then(Modifier.horizontalScroll(rememberScrollState()))
                        )
                    }
                )
            }
        )
    }
}