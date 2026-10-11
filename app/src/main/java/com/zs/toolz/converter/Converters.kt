package com.zs.toolz.converter


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.zs.compose.foundation.textResource
import com.zs.compose.foundation.thenIf
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ButtonDefaults
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Surface
import com.zs.compose.theme.text.Label
import com.zs.toolz.common.Res
import com.zs.toolz.common.gap_medium
import com.zs.toolz.common.vectorResource
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.unit.IntOffset as IconLable


@Composable
private fun Converter(
    visuals: IconLable,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedToggle: () -> Unit
) {
    // convert checked/unchecked to progress
    val progress by animateFloatAsState(
        if (checked) 1f else 0f,
        animationSpec = AppTheme.motionScheme.slowSpatialSpec()
    )
    //
    Surface(
        onClick = onCheckedToggle,
        modifier = modifier.height(74.dp),
        color = androidx.compose.ui.graphics.lerp(
            Color.Transparent,
            AppTheme.colors.accent.copy(ContentAlpha.indication),
            progress
        ),
        contentColor = androidx.compose.ui.graphics.lerp(
            AppTheme.colors.onBackground.copy(ContentAlpha.high),
            AppTheme.colors.accent,
            progress
        ),
        shape = AppTheme.shapes.medium,
        border = ButtonDefaults.outlinedBorder,
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Res.space.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(Res.space.medium),
                content = {
                    val label = textResource(visuals.y)
                    com.zs.compose.theme.Icon( // icon
                        vectorResource(visuals.x),
                        label.toString(),
                        modifier = Modifier.thenIf(checked) {
                            Modifier.size(lerp(22.dp, 30.dp, progress))
                        }
                    )

                    // label
                    Label(
                        label.toString(),
                        style = AppTheme.typography.label3,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                    )
                }
            )
        }
    )
}

@Composable
fun Converters(
    checked: String,
    onRequestCheck: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    LazyRow(
        modifier = modifier,
        state = state,
        horizontalArrangement = Res.layout.gap_medium,
        content = {
            // length
            item {
                Converter(
                    checked = checked == RouteUnitConverter.KEY_LENGTH_CONVERTER,
                    onCheckedToggle = { onRequestCheck(RouteUnitConverter.KEY_LENGTH_CONVERTER) },
                    visuals = IconLable(Res.drawable.ic_length, Res.string.length)
                )
            }

            // weight
            item {
                Converter(
                    checked = checked == RouteUnitConverter.KEY_MASS_CONVERTER,
                    onCheckedToggle = { onRequestCheck(RouteUnitConverter.KEY_MASS_CONVERTER) },
                    visuals = IconLable(Res.drawable.ic_weight_n_mass, Res.string.weight_and_mass)
                )
            }

            // time
            item {
                Converter(
                    checked = checked == RouteUnitConverter.KEY_TIME_CONVERTER,
                    onCheckedToggle = { onRequestCheck(RouteUnitConverter.KEY_TIME_CONVERTER) },
                    visuals = IconLable(Res.drawable.ic_time, Res.string.time)
                )
            }
            // temperatire
            item {
                Converter(
                    checked = checked == RouteUnitConverter.KEY_TEMPERATURE_CONVERTER,
                    onCheckedToggle = { onRequestCheck(RouteUnitConverter.KEY_TEMPERATURE_CONVERTER) },
                    visuals = IconLable(Res.drawable.ic_temperature, Res.string.temperature)
                )
            }

            // angle
            item {
                Converter(
                    checked = checked == RouteUnitConverter.KEY_ANGLE_CONVERTER,
                    onCheckedToggle = { onRequestCheck(RouteUnitConverter.KEY_ANGLE_CONVERTER) },
                    visuals = IconLable(Res.drawable.ic_angle, Res.string.angle)
                )
            }

            // area
            item {
                Converter(
                    checked = checked == RouteUnitConverter.KEY_AREA_CONVERTER,
                    onCheckedToggle = { onRequestCheck(RouteUnitConverter.KEY_AREA_CONVERTER) },
                    visuals = IconLable(Res.drawable.ic_area, Res.string.area)
                )
            }


            // pressure
            item {
                Converter(
                    checked = checked == RouteUnitConverter.KEY_PRESSURE_CONVERTER,
                    onCheckedToggle = { onRequestCheck(RouteUnitConverter.KEY_PRESSURE_CONVERTER) },
                    visuals = IconLable(Res.drawable.ic_pressure, Res.string.pressure)
                )
            }

            // energy
            item {
                Converter(
                    checked = checked == RouteUnitConverter.KEY_ENERGY_CONVERTER,
                    onCheckedToggle = { onRequestCheck(RouteUnitConverter.KEY_ENERGY_CONVERTER) },
                    visuals = IconLable(Res.drawable.ic_energy, Res.string.energy)
                )
            }

            // power
            item {
                Converter(
                    checked = checked == RouteUnitConverter.KEY_POWER_CONVERTER,
                    onCheckedToggle = { onRequestCheck(RouteUnitConverter.KEY_POWER_CONVERTER) },
                    visuals = IconLable(Res.drawable.ic_power, Res.string.power)
                )
            }

            // speed
            item {
                Converter(
                    checked = checked == RouteUnitConverter.KEY_SPEED_CONVERTER,
                    onCheckedToggle = { onRequestCheck(RouteUnitConverter.KEY_SPEED_CONVERTER) },
                    visuals = IconLable(Res.drawable.ic_motorcycle, Res.string.speed)
                )
            }
        }
    )

    LaunchedEffect(Unit) {
        delay(0.5.seconds)
        val index = when(checked){
            RouteUnitConverter.KEY_LENGTH_CONVERTER -> 0
            RouteUnitConverter.KEY_MASS_CONVERTER -> 1
            RouteUnitConverter.KEY_TIME_CONVERTER -> 2
            RouteUnitConverter.KEY_TEMPERATURE_CONVERTER -> 3
            RouteUnitConverter.KEY_ANGLE_CONVERTER -> 4
            RouteUnitConverter.KEY_AREA_CONVERTER -> 5
            RouteUnitConverter.KEY_PRESSURE_CONVERTER -> 6
            RouteUnitConverter.KEY_ENERGY_CONVERTER -> 7
            RouteUnitConverter.KEY_POWER_CONVERTER -> 8
            RouteUnitConverter.KEY_SPEED_CONVERTER -> 9
            else -> TODO("add new converter here!")
        }
        state.animateScrollToItem(index)
    }
}