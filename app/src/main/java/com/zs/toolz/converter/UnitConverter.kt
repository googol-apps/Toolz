package com.zs.toolz.converter

import androidx.compose.runtime.NonRestartableComposable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object UnitConverter: NavKey {
    @NonRestartableComposable
    operator fun invoke(viewState: UnitConverterViewState){

    }
}

