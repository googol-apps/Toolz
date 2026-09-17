package com.zs.domain.converter

import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.zs.domain.math.UnifiedReal

/**
 * Represents a single unit of measurement within a [UnitConverter].
 *
 * Each [MeasureUnit] defines metadata (identifier, name, symbol, group, icon, description)
 * and conversion logic to and from the converter's base unit.
 *
 * This design allows any two units in the same [UnitConverter] to be converted
 * via the shared base unit, avoiding pairwise conversion formulas.
 *
 * @property id Unique identifier for this unit within the converter system.
 *              Typically a stable string key (e.g., "Temperature_Celsius").
 *              Used for lookup, persistence, and serialization.
 * @property title Full display name of this unit, e.g. "Celsius".
 * @property symbol Short symbol/abbreviation of this unit, e.g. "°C".
 * @property group Category/system this unit belongs to, e.g. "SI", "Imperial".
 * @property icon Drawable resource representing this unit, or [ResourcesCompat.ID_NULL] if none.
 * @property description Optional description or usage note for this unit.
 *                       Useful for tooltips, accessibility, or user education.
 */
interface MeasureUnit {

    val id: String
    val title: String
    val symbol: String
    val group: String
    @get:DrawableRes val icon: Int
    val description: String?

    /**
     * Converts [value], expressed in this unit, to the converter's base unit.
     *
     * @param value The value expressed in this unit
     * @return The equivalent value expressed in the base unit
     */
    fun toBase(value: UnifiedReal): UnifiedReal

    /**
     * Converts [value], expressed in the converter's base unit, to this unit.
     *
     * @param value The value expressed in the base unit
     * @return The equivalent value expressed in this unit
     */
    fun toUnit(value: UnifiedReal): UnifiedReal
}

/**
 * Factory function to create a [MeasureUnit] instance.
 *
 * This provides a concise way to define units with metadata and
 * conversion behavior without writing boilerplate implementations.
 *
 * @param factor Conversion factor relative to the base unit.
 *               For example, if the base is "meter", then "centimeter" would have a factor of 0.01.
 * @return A new instance of [MeasureUnit] with the specified properties.
 * @see com.zs.domain.converter.MeasureUnit
 */
fun MeasureUnit(
    id: String,
    title: String,
    symbol: String,
    group: String,
    factor: UnifiedReal,
    @DrawableRes icon: Int = ResourcesCompat.ID_NULL,
    description: String? = null,
): MeasureUnit = object : MeasureUnit {
    override val id: String = id
    override val title: String = title
    override val symbol: String = symbol
    override val group: String = group
    override val icon: Int = icon
    override val description = description

    override fun toBase(value: UnifiedReal): UnifiedReal = value.multiply(factor)
    override  fun toUnit(value: UnifiedReal): UnifiedReal = value.divide(factor)
}