package com.zs.toolz.converter

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.zs.domain.math.UnifiedReal

/**
 * Defines a unit of measurement within a [Converter].
 *
 * Each [MeasureUnit] has a stable [id] and provides
 * conversion logic to and from the converter's base unit.
 *
 * @property id Unique identifier for this unit (e.g., "Temperature_Celsius").
 */
interface MeasureUnit {
    val id: String

    /**
     * Converts [value], expressed in this unit, to the converter's base unit.
     *
     * @param value Value in this unit
     * @return Equivalent value in the base unit
     */
    fun toBase(value: UnifiedReal): UnifiedReal

    /**
     * Converts [value], expressed in the converter's base unit, to this unit.
     *
     * @param value Value in the base unit
     * @return Equivalent value in this unit
     */
    fun toUnit(value: UnifiedReal): UnifiedReal
}

/**
 * Basic implementation of [MeasureUnit] using a fixed conversion [factor].
 *
 * Provides metadata (title, symbol, group, icon, description) and
 * simple multiply/divide logic for converting values to and from
 * the converter's base unit.
 *
 * @property id Stable identifier for this unit (e.g., "Length_Centimeter").
 * @property factor Conversion multiplier relative to the base unit.
 * @property title String resource for the full display name (e.g., "Centimeter").
 * @property symbol String resource for the short symbol/abbreviation (e.g., "cm").
 * @property group String resource for the category/system this unit belongs to.
 * @property icon Optional drawable resource for an icon (default: none).
 * @property description Optional string resource for a description (default: none).
 */
class SimpleMeasureUnit(
    override val id: String,
    @StringRes val group: Int,
    @StringRes val title: Int,
    @StringRes val symbol: Int,
    val factor: UnifiedReal,
) : MeasureUnit {
    override fun toBase(value: UnifiedReal): UnifiedReal = value.multiply(factor)
    override fun toUnit(value: UnifiedReal): UnifiedReal = value.divide(factor)
}