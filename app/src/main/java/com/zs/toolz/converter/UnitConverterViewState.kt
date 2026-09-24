package com.zs.toolz.converter

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import com.zs.domain.math.UnifiedReal


/**
 * Defines a unit of measurement within a [Converter].
 *
 * Each [MeasureUnit] has a stable [id] and provides
 * conversion logic to and from the converter's base unit.
 *
 * @property id Unique identifier for this unit (e.g., "Temperature_Celsius").
 */
@Stable
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
 * A measure unit defined by a grouping and localized string resources.
 *
 * @property group Resource ID for the unit's category group.
 * @property title Resource ID for the unit's name.
 * @property symbol Resource ID for the unit's symbol.
 */
@Stable
interface SimpleMeasureUnit : MeasureUnit {
    @get:StringRes
    val group: Int

    @get:StringRes
    val title: Int

    /**
     * The display symbol for this unit (e.g., "m", "kg", "°C").
     */
    @get:StringRes
    val symbol: Int
}

/**
 * Factory function to create a [BasicMeasureUnit] that uses a linear conversion factor.
 *
 * The resulting unit converts to the base unit by multiplying by [factor],
 * and converts from the base unit by dividing by [factor].
 *
 * @param id Unique identifier for the unit.
 * @param group String resource ID for the category group.
 * @param title String resource ID for the unit name.
 * @param symbol String resource ID for the unit symbol.
 * @param factor The multiplicative factor relative to the base unit.
 */
@Stable
fun SimpleMeasureUnit(
    id: String,
    @StringRes group: Int,
    @StringRes title: Int,
    @StringRes symbol: Int,
    factor: UnifiedReal
): SimpleMeasureUnit = object : SimpleMeasureUnit {
    override val group: Int = group
    override val title: Int = title
    override val symbol: Int = symbol
    override val id: String = id

    override fun toBase(value: UnifiedReal): UnifiedReal = value.multiply(factor)
    override fun toUnit(value: UnifiedReal): UnifiedReal = value.divide(factor)
}

/**
 * A raw implementation of [MeasureUnit] that uses strings for metadata instead of resource IDs.
 *
 * @property id Unique identifier for this unit.
 * @property group The name of the category group this unit belongs to.
 * @property title The display name of the unit.
 * @property symbol The shorthand symbol for the unit.
 * @property factor The multiplicative factor relative to the base unit.
 */
@Stable
class RawMeasureUnit(
    override val id: String,
    val group: String,
    val title: String,
    val symbol: String,
    private val factor: UnifiedReal
): MeasureUnit {
    override fun toBase(value: UnifiedReal): UnifiedReal = value.multiply(factor)
    override fun toUnit(value: UnifiedReal): UnifiedReal = value.divide(factor)
}
