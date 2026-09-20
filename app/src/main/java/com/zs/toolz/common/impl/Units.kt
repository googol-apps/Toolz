package com.zs.toolz.common.impl

import com.zs.domain.math.BoundedRational
import com.zs.domain.math.UnifiedReal
import com.zs.toolz.common.Res
import com.zs.toolz.converter.RouteUnitConverter
import com.zs.toolz.converter.SimpleMeasureUnit

private const val TAG = "Angle"

private val AngelConverts
    get() = listOf(
        SimpleMeasureUnit(
            "_angle_degree",
            Res.string.system_international,
            Res.string.degrees,
            Res.string.code_degrees,
            UnifiedReal(BoundedRational(1, 1))
        ), SimpleMeasureUnit(
            "_angle_radian",
            Res.string.system_international,
            Res.string.radian,
            Res.string.code_radian,
            UnifiedReal(BoundedRational(572957795130823L, 10000000000000L))
        ),
        SimpleMeasureUnit(
            "_angle_gradian",
            Res.string.system_international,
            Res.string.gradian,
            Res.string.code_gradian,
            UnifiedReal(BoundedRational(9, 10))
        )
    )

/**
 * @return array of units for [converter]
 */
fun Units(converter: String): List<SimpleMeasureUnit> {
    return when (converter) {
        RouteUnitConverter.KEY_ANGLE_CONVERTER -> AngelConverts
        else -> TODO("Unknown converter $converter")
    }
}