package com.zs.toolz.converter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.navigation3.runtime.NavKey
import com.zs.compose.theme.text.Text
import com.zs.toolz.common.Res
import kotlinx.serialization.Serializable
import androidx.compose.ui.unit.IntOffset as LabelIcon

@Serializable
object RouteUnitConverter: NavKey {
    const val KEY_AREA_CONVERTER = "_area"
    const val KEY_ANGLE_CONVERTER = "_angle"
    const val KEY_LENGTH_CONVERTER = "_length"
    const val KEY_MASS_CONVERTER = "_mass"
    const val KEY_VOLUME_CONVERTER = "_volume"
    const val KEY_TIME_CONVERTER = "_time"
    const val KEY_TEMPERATURE_CONVERTER = "_temperature"
    const val KEY_DATA_CONVERTER = "_data"


    /**
     * @return lable and icon for the converter as [LabelIcon]
     */
    fun converterAssetsForKey(key: String): LabelIcon {
        return LabelIcon(Res.string.angle, Res.drawable.ic_angle)
    }

    @Composable
    @NonRestartableComposable
    operator fun invoke(viewState: UnitConverterViewState){
        Text("Unit Converter")
    }
}