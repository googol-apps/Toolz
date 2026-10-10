package com.zs.toolz.common.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.zs.compose.foundation.backdrop.Backdrop
import com.zs.compose.foundation.backdrop.haze.hazeEffect
import com.zs.compose.foundation.backdrop.haze.legacyHazeEffect

fun Modifier.compactHazeEffect(
    backdrop: Backdrop,
    surface: Color
) =
    legacyHazeEffect(
        backdrop = backdrop,
        surface = surface
    )
