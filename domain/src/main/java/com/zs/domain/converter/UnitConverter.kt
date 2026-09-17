package com.zs.domain.converter

import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.zs.domain.math.UnifiedReal

/**
 * Represents a converter that groups together related [MeasureUnit]s
 * and provides conversion logic between them via a shared base unit.
 *
 * Each [UnitConverter] defines metadata (key, title, icon, description)
 * and the set of supported units. Conversion between any two units
 * is achieved by normalizing values to the base unit and then mapping
 * them back to the target unit.
 *
 * @property key Unique identifier string for this converter,
 *               used for lookup, persistence, or serialization.
 * @property title Display name of this converter, e.g. "Temperature".
 * @property icon Optional Icon resource or [ResourcesCompat.ID_NULL],
 *                e.g. a thermometer icon.
 * @property description Optional explanatory text or description for this converter.
 * @property units Array of [MeasureUnit]s supported by this converter.
 */
interface UnitConverter {

    val key: String
    @get:DrawableRes val icon: Int

    val title: String
    val description: String?
    val units: Array<MeasureUnit>

    /**
     * Converts [value] from one [MeasureUnit] to another within this converter.
     *
     * Conversion is performed by first mapping the input value to the
     * converter's base unit via [MeasureUnit.toBase], then mapping
     * that base value into the target unit via [MeasureUnit.toUnit].
     *
     * @param from The source unit in which [value] is expressed.
     * @param to The target unit to which [value] should be converted.
     * @param value The numeric value expressed in the [from] unit.
     * @return The equivalent value expressed in the [to] unit.
     */
    fun convert(from: MeasureUnit, to: MeasureUnit, value: UnifiedReal): UnifiedReal {
        val inBase = from.toBase(value)
        return to.toUnit(inBase)
    }
}

/**
 * Factory function to create an instance of [UnitConverter].
 * @return A new instance of [UnitConverter] with the specified properties.
 * @see com.zs.domain.converter.UnitConverter
 */
fun UnitConverter(
    key: String,
    title: String,
    units: Array<MeasureUnit>,
    description: String? = null,
    @DrawableRes icon: Int = ResourcesCompat.ID_NULL,
): UnitConverter = object : UnitConverter {
    override val key: String = key
    override val title: String = title
    override val icon = icon
    override val description: String? = description
    override val units: Array<MeasureUnit> = units
}