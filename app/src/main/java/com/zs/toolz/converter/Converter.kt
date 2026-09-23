package com.zs.toolz.converter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
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
import com.zs.compose.theme.Surface
import com.zs.compose.theme.text.Label
import com.zs.toolz.common.Res
import com.zs.toolz.common.vectorResource
import androidx.compose.ui.unit.IntOffset as IconLable

@Composable
private fun Converter(
    visuals: IconLable,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedToogle: () -> Unit
) {
    val motion = AppTheme.motionScheme
    val progress by animateFloatAsState(if (checked) 1f else 0f, animationSpec = motion.slowSpatialSpec())
    Surface(
        onClick = onCheckedToogle,
        modifier = modifier.size(95.dp, 73.dp),
        color = androidx.compose.ui.graphics.lerp(
            Color.Transparent,
            AppTheme.colors.accent,
            progress
        ),
        contentColor = androidx.compose.ui.graphics.lerp(
            AppTheme.colors.onBackground,
            AppTheme.colors.onAccent,
            progress
        ),
        shape = AppTheme.shapes.medium,
        border = if (checked) null else ButtonDefaults.outlinedBorder
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Res.dimen.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = Res.dimen.medium, vertical = Res.dimen.x_small)
        ) {
            val lable = textResource(visuals.y)
            com.zs.compose.theme.Icon(
                vectorResource(visuals.x),
                lable.toString(),
                modifier = Modifier.thenIf(checked){
                    Modifier.size(lerp(22.dp, 26.dp, progress))
                }
            )
            Label(
                lable.toString(),
                style = AppTheme.typography.label3,
                fontWeight = FontWeight(androidx.compose.ui.util.lerp(400, 600, progress))
            )
        }
    }
}

@Composable
fun Converters(
    checked: String,
    onRequestCheck: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Res.dimen.medium)
    )  {
        // length
        item {
            Converter(
                checked = checked == UnitConverter.KEY_LENGTH_CONVERTER,
                onCheckedToogle = { onRequestCheck(UnitConverter.KEY_LENGTH_CONVERTER) },
                visuals = IconLable(Res.drawable.ic_length, Res.string.length)
            )
        }

        // weight
        item {
            Converter(
                checked = checked == UnitConverter.KEY_MASS_CONVERTER,
                onCheckedToogle = { onRequestCheck(UnitConverter.KEY_MASS_CONVERTER) },
                visuals = IconLable(Res.drawable.ic_weight_n_mass, Res.string.weight_and_mass)
            )
        }

        // time
        item {
            Converter(
                checked = checked == UnitConverter.KEY_TIME_CONVERTER,
                onCheckedToogle = { onRequestCheck(UnitConverter.KEY_TIME_CONVERTER) },
                visuals = IconLable(Res.drawable.ic_time, Res.string.time)
            )
        }
        // temperatire
        item {
            Converter(
                checked = checked == UnitConverter.KEY_TEMPERATURE_CONVERTER,
                onCheckedToogle = { onRequestCheck(UnitConverter.KEY_TEMPERATURE_CONVERTER) },
                visuals = IconLable(Res.drawable.ic_temperature, Res.string.temperature)
            )
        }

        // angle
        item {
            Converter(
                checked = checked == UnitConverter.KEY_ANGLE_CONVERTER,
                onCheckedToogle = { onRequestCheck(UnitConverter.KEY_ANGLE_CONVERTER) },
                visuals = IconLable(Res.drawable.ic_angle, Res.string.angle)
            )
        }

        // area
        item {
            Converter(
                checked = checked == UnitConverter.KEY_AREA_CONVERTER,
                onCheckedToogle = { onRequestCheck(UnitConverter.KEY_AREA_CONVERTER) },
                visuals = IconLable(Res.drawable.ic_area, Res.string.area)
            )
        }


        // pressure
        item {
            Converter(
                checked = checked == UnitConverter.KEY_PRESSURE_CONVERTER,
                onCheckedToogle = { onRequestCheck(UnitConverter.KEY_PRESSURE_CONVERTER) },
                visuals = IconLable(Res.drawable.ic_pressure, Res.string.pressure)
            )
        }

        // energy
        item {
            Converter(
                checked = checked == UnitConverter.KEY_ENERGY_CONVERTER,
                onCheckedToogle = { onRequestCheck(UnitConverter.KEY_ENERGY_CONVERTER) },
                visuals = IconLable(Res.drawable.ic_energy, Res.string.energy)
            )
        }

        // power
        item {
            Converter(
                checked = checked == UnitConverter.KEY_POWER_CONVERTER,
                onCheckedToogle = { onRequestCheck(UnitConverter.KEY_POWER_CONVERTER) },
                visuals = IconLable(Res.drawable.ic_power, Res.string.power)
            )
        }

        // speed
        item {
            Converter(
                checked = checked == UnitConverter.KEY_SPEED_CONVERTER,
                onCheckedToogle = { onRequestCheck(UnitConverter.KEY_SPEED_CONVERTER) },
                visuals = IconLable(Res.drawable.ic_motorcycle, Res.string.speed)
            )
        }
    }
}