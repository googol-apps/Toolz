package com.zs.toolz.converter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.navigation3.runtime.NavKey
import com.zs.compose.theme.text.Text
import kotlinx.serialization.Serializable

@Serializable
object UnitConverter: NavKey {

    @Composable
    @NonRestartableComposable
    operator fun invoke(viewState: UnitConverterViewState){
        Text("Unit Converter")
    }

}

