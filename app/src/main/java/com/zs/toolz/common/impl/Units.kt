package com.zs.toolz.common.impl

import com.zs.domain.math.BoundedRational
import com.zs.domain.math.UnifiedReal
import com.zs.toolz.common.Res
import com.zs.toolz.converter.UnitConverter
import java.math.BigInteger
import com.zs.toolz.converter.SimpleMeasureUnit as Unit

private const val TAG = "Units"

private val AngelUnits
    get() = listOf(
        Unit(
            "_angle_degree",
            Res.string.system_international,
            Res.string.degrees,
            Res.string.code_degrees,
            UnifiedReal(BoundedRational(1, 1))
        ), Unit(
            "_angle_radian",
            Res.string.system_international,
            Res.string.radian,
            Res.string.code_radian,
            UnifiedReal(BoundedRational(572957795130823L, 10000000000000L))
        ),
        Unit(
            "_angle_gradian",
            Res.string.system_international,
            Res.string.gradian,
            Res.string.code_gradian,
            UnifiedReal(BoundedRational(9, 10))
        )
    )

//Base Unit: Square Metre
private val AreaUnits
    get() = listOf(

        Unit(
            "_area_sq_millimetre",
            Res.string.system_international,
            Res.string.sq_millimetres,
            Res.string.code_sq_millimetres,
            UnifiedReal(BoundedRational(1, 1000000))
        ),
        Unit(
            "_area_sq_centimetre",
            Res.string.system_international,
            Res.string.sq_centimetres,
            Res.string.code_sq_centimetres,
            UnifiedReal(BoundedRational(1, 10000))
        ),
        Unit(
            "_area_sq_metre",
            Res.string.system_international,
            Res.string.sq_metres,
            Res.string.code_sq_metres,
            UnifiedReal(BoundedRational(1, 1))
        ),

        Unit(
            "_area_sq_hectare",
            Res.string.system_international,
            Res.string.hectare,
            Res.string.code_hectare,
            UnifiedReal(BoundedRational(10000, 1))
        ),
        Unit(
            "_area_sq_kilometre",
            Res.string.system_international,
            Res.string.sq_kilometre,
            Res.string.code_sq_kilometre,
            UnifiedReal(BoundedRational(1000000, 1))
        ),

        Unit(
            "_area_sq_inch",
            Res.string.imperial_system,
            Res.string.sq_inche,
            Res.string.code_sq_inche,
            UnifiedReal(BoundedRational(64516, 100000000))
        ),
        Unit(
            "_area_sq_foot",
            Res.string.imperial_system,
            Res.string.sq_foot,
            Res.string.code_sq_foot,
            UnifiedReal(BoundedRational(92903, 1000000))
        ),

        Unit(
            "_area_sq_yard",
            Res.string.imperial_system,
            Res.string.sq_yard,
            Res.string.code_sq_yard,
            UnifiedReal(BoundedRational(836127, 1000000))
        ), Unit(
            "_area_acre",
            Res.string.imperial_system,
            Res.string.acre,
            Res.string.code_acre,
            UnifiedReal(BoundedRational(40468564224L, 10000000L))
        ),
        Unit(
            "_area_sq_mile",
            Res.string.imperial_system,
            Res.string.sq_mile,
            Res.string.code_sq_mile,
            UnifiedReal(BoundedRational(2589988110336L, 1000000))
        )
    )

//base unit joule
private val EnergyUnits
    get() = listOf(
        Unit(
            "_energy_electron_volt",
            Res.string.system_international,
            Res.string.electron_volt,
            Res.string.code_electron_volt,
            UnifiedReal(
                BoundedRational(
                    BigInteger("1602176565"), BigInteger("10000000000000000000000000000")
                )
            )
        ),
        Unit(
            "_energy_joule",
            Res.string.system_international,
            Res.string.joule,
            Res.string.code_joule,
            UnifiedReal(BoundedRational(1, 1))
        ), Unit(
            "_energy_kilo_joule",
            Res.string.system_international,
            Res.string.kilojoule,
            Res.string.code_kilojoule,
            UnifiedReal(BoundedRational(1000, 1))
        ), Unit(
            "_energy_thermal_calorie",
            Res.string.system_international,
            Res.string.thermal_calorie,
            Res.string.code_thermal_calorie,
            UnifiedReal(BoundedRational(4184, 1000))
        ), Unit(
            "_energy_food_calorie",
            Res.string.system_international,
            Res.string.food_calorie,
            Res.string.code_food_calorie,
            UnifiedReal(BoundedRational(4184, 1))
        ),
        Unit(
            "_energy_foot_pound",
            Res.string.imperial_system,
            Res.string.foot_pound,
            Res.string.code_foot_pound,
            UnifiedReal(
                BoundedRational(13558179483314003L, 10000000000000000L)
            )
        )
    )

// Base Unit Metre
private val LengthUnits
    get() = listOf(
        Unit(
            "_length_nanometre",
            Res.string.system_international,
            Res.string.nanometre,
            Res.string.code_nanometre,
            UnifiedReal(BoundedRational(1, 1000000000))
        ), Unit(
            "_length_micrometre",
            Res.string.system_international,
            Res.string.micrometre,
            Res.string.code_micrometre,
            UnifiedReal(BoundedRational(1, 1000000))
        ), Unit(
            "_length_millimetre",
            Res.string.system_international,
            Res.string.millimetre,
            Res.string.code_millimetre,
            UnifiedReal(BoundedRational(1, 1000))
        ), Unit(
            "_length_centimetre",
            Res.string.system_international,
            Res.string.centimetre,
            Res.string.code_centimetre,
            UnifiedReal(BoundedRational(1, 100))
        ), Unit(
            "_length_metre",
            Res.string.system_international,
            Res.string.metre,
            Res.string.code_metre,
            UnifiedReal(1)
        ), Unit(
            "_length_kilometre",
            Res.string.system_international,
            Res.string.kilometre,
            Res.string.code_kilometre,
            UnifiedReal(BoundedRational(1000, 1))
        ), Unit(
            "_length_mile",
            Res.string.imperial_system,
            Res.string.mile,
            Res.string.code_mile,
            UnifiedReal(BoundedRational(1609344, 1000))
        ), Unit(
            "_length_nautical_mile",
            Res.string.imperial_system,
            Res.string.nautical_mile,
            Res.string.code_nautical_mile,
            UnifiedReal(BoundedRational(1852, 1))
        ), Unit(
            "_length_yard",
            Res.string.imperial_system,
            Res.string.yard,
            Res.string.code_yard,
            UnifiedReal(BoundedRational(9144, 10000))
        ), Unit(
            "_length_foot",
            Res.string.imperial_system,
            Res.string.foot,
            Res.string.code_foot,
            UnifiedReal(BoundedRational(3048, 10000))
        ), Unit(
            "_length_inch",
            Res.string.imperial_system,
            Res.string.inch,
            Res.string.code_inch,
            UnifiedReal(BoundedRational(254, 10000))
        ), Unit(
            "_length_astronomical_unit",
            Res.string.system_international,
            Res.string.astronomical_unt,
            Res.string.code_astronomical_unit,
            UnifiedReal(BoundedRational(149597870700L, 1))
        ), Unit(
            "_length_light_year",
            Res.string.system_international,
            Res.string.light_year,
            Res.string.code_light_year,
            UnifiedReal(BoundedRational(9460730472580800L, 1))
        )
    )

//Base unit - Kilograms
private val MassUnits
    get() = listOf(
        Unit(
            "_mass_carat",
            Res.string.system_international,
            Res.string.carat,
            Res.string.code_carat,
            UnifiedReal(BoundedRational(2, 10000))
        ), Unit(
            "_mass_milli_gram",
            Res.string.system_international,
            Res.string.milligram,
            Res.string.code_milligram,
            UnifiedReal(BoundedRational(1, 1000000))
        ), Unit(
            "_mass_centi_gram",
            Res.string.system_international,
            Res.string.centigram,
            Res.string.code_centigram,
            UnifiedReal(BoundedRational(1, 100000))
        ), Unit(
            "_mass_decigram",
            Res.string.system_international,
            Res.string.decigram,
            Res.string.code_decigram,
            UnifiedReal(BoundedRational(1, 10000))
        ), Unit(
            "_mass_gram",
            Res.string.system_international,
            Res.string.gram,
            Res.string.code_gram,
            UnifiedReal(BoundedRational(1, 1000))
        ), Unit(
            "_mass_deca_gram",
            Res.string.system_international,
            Res.string.decagram,
            Res.string.code_decagram,
            UnifiedReal(BoundedRational(1, 100))
        ), Unit(
            "_mass_hectogram",
            Res.string.system_international,
            Res.string.hectogram,
            Res.string.code_hectogram,
            UnifiedReal(BoundedRational(1, 10))
        ), Unit(
            "_mass_kilo_gram",
            Res.string.system_international,
            Res.string.kilogram,
            Res.string.code_kilogram,
            UnifiedReal(BoundedRational(1, 1))
        ), Unit(
            "_mass_metric_tonne",
            Res.string.system_international,
            Res.string.metric_ton,
            Res.string.code_metric_ton,
            UnifiedReal(BoundedRational(1000, 1))
        ), Unit(
            "_mass_ounce",
            Res.string.imperial_system,
            Res.string.ounce,
            Res.string.code_ounce,
            UnifiedReal(BoundedRational(28349523125L, 1000000000000L))
        ), Unit(
            "_mass_pound",
            Res.string.imperial_system,
            Res.string.pound,
            Res.string.code_pound,
            UnifiedReal(BoundedRational(45359237, 100000000))
        ), Unit(
            "_mass_stone",
            Res.string.imperial_system,
            Res.string.stone,
            Res.string.code_stone,
            UnifiedReal(BoundedRational(635029318, 100000000))
        ), Unit(
            "_mass_short_tonne_us",
            Res.string.imperial_system_us,
            Res.string.short_ton,
            Res.string.code_short_ton,
            UnifiedReal(BoundedRational(90718474, 100000))
        ), Unit(
            "_mass_long_tonne_uk",
            Res.string.imperial_system,
            Res.string.long_ton,
            Res.string.code_long_ton,
            UnifiedReal(BoundedRational(10160469088L, 10000000))
        )
    )

//basic unit watt
private val PowerUnits
    get() = listOf(
        Unit(
            "_power_watt",
            Res.string.system_international,
            Res.string.watt,
            Res.string.code_watt,
            UnifiedReal(BoundedRational(1, 1))
        ), Unit(
            "_power_kilo_watt",
            Res.string.system_international,
            Res.string.kilowatt,
            Res.string.code_kilowatt,
            UnifiedReal(BoundedRational(1000, 1))
        ), Unit(
            "_power_horse_power",
            Res.string.imperial_system_us,
            Res.string.horse_power_us,
            Res.string.code_horse_power_us,
            UnifiedReal(
                BoundedRational(
                    7456998715822702L,
                    10000000000000L
                )
            )
        ), Unit(
            "_power_foot_pounds_per_minute",
            Res.string.imperial_system,
            Res.string.foot_pounds_per_minute,
            Res.string.code_foot_pounds_per_minute,
            UnifiedReal(
                BoundedRational(22596966, 1000000000L)
            )
        ), Unit(
            "_power_btu_per_minute",
            Res.string.imperial_system,
            Res.string.british_thermal_units_per_minute,
            Res.string.code_british_thermal_units_per_minute,
            UnifiedReal(
                BoundedRational(175842641667L, 10000000000L)
            )
        )
    )

// Basic Unit Pascal
private val PressureUnits
    get() = listOf(
        Unit(
            "_pressure_atmosphere",
            Res.string.system_international,
            Res.string.atmosphere,
            Res.string.code_atmosphere,
            UnifiedReal(
                BoundedRational(101325, 1)
            )
        ), Unit(
            "_pressure_bar",
            Res.string.system_international,
            Res.string.bar,
            Res.string.code_bar,
            UnifiedReal(
                BoundedRational(100000, 1)
            )
        ), Unit(
            "_pressure_kilo_pascal",
            Res.string.system_international,
            Res.string.kilopascal,
            Res.string.code_kilopascal,
            UnifiedReal(
                BoundedRational(1000, 1)
            )
        ), Unit(
            "_pressure_pounds_per_inch",
            Res.string.system_international,
            Res.string.pounds_per_inch,
            Res.string.code_pounds_per_inch,
            UnifiedReal(
                BoundedRational(
                    689475729316836L,
                    100000000000L
                )
            )
        ), Unit(
            "_pressure_mms_of_mercury",
            Res.string.system_international,
            Res.string.mm_of_mercury,
            Res.string.code_mm_of_mercury,
            UnifiedReal(
                BoundedRational(133322387415L, 1000000000)
            )
        ), Unit(
            "_pressure_pascal",
            Res.string.system_international,
            Res.string.pascal,
            Res.string.code_pascal,
            UnifiedReal(
                BoundedRational(1, 1)
            )
        )
    )

// Base Unit kmph
private val SpeedUnits
    get() = listOf(
        Unit(
            "_speed_cms_per_second",
            Res.string.system_international,
            Res.string.centimetres_per_second,
            Res.string.code_centimetres_per_second,
            UnifiedReal(
                BoundedRational(36, 1000)
            )
        ), Unit(
            "_speed_Ms_per_second",
            Res.string.system_international,
            Res.string.metres_per_second,
            Res.string.code_metres_per_second,
            UnifiedReal(
                BoundedRational(36, 10)
            )
        ), Unit(
            "_speed_KMs_per_hour",
            Res.string.system_international,
            Res.string.kilometres_per_hour,
            Res.string.code_kilometres_per_hour,
            UnifiedReal(
                BoundedRational(1, 1)
            )
        ), Unit(
            "_speed_feet_per_second",
            Res.string.imperial_system,
            Res.string.feet_per_second,
            Res.string.code_feet_per_second,
            UnifiedReal(
                BoundedRational(109728, 100000)
            )
        ), Unit(
            "_speed_miles_per_hour",
            Res.string.imperial_system,
            Res.string.miles_per_hour,
            Res.string.code_miles_per_hour,
            UnifiedReal(
                BoundedRational(16092, 10000)
            )
        ), Unit(
            "_speed_knot",
            Res.string.imperial_system,
            Res.string.knot,
            Res.string.code_knot,
            UnifiedReal(
                BoundedRational(185184, 100000)
            )
        ), Unit(
            "_speed_mach",
            Res.string.imperial_system,
            Res.string.mach,
            Res.string.code_mach,
            UnifiedReal(
                BoundedRational(122508, 100)
            )
        )
    )

// no base unit
private val TempretureUnits
    get() = listOf(
        object : Unit {
            override val title = Res.string.celsius
            override val symbol = Res.string.code_celsius
            override val id = "temp_celsius"
            override val group = Res.string.system_international

            override fun toBase(value: UnifiedReal): UnifiedReal = value
            override fun toUnit(value: UnifiedReal): UnifiedReal = value
        },
        object : Unit {
            override val title = Res.string.fahrenheit
            override val symbol = Res.string.code_fahrenheit
            override val id: String = TAG + "_temp_fahrenheit"
            override val group = Res.string.united_system_customary_system

            override fun toBase(value: UnifiedReal): UnifiedReal =
                value.subtract(UnifiedReal(32)).multiply(UnifiedReal(BoundedRational(5, 9)))

            override fun toUnit(value: UnifiedReal): UnifiedReal =
                value.multiply(UnifiedReal(BoundedRational(9, 5))).add(
                    UnifiedReal(32)
                )
        },
        object : Unit {
            override val title = Res.string.kelvin
            override val symbol = Res.string.code_kelvin
            override val id: String = "_temp_kelvin"
            override val group = Res.string.system_international

            override fun toBase(value: UnifiedReal) =
                value.subtract(UnifiedReal(BoundedRational(27315, 100)))

            override fun toUnit(value: UnifiedReal) =
                value.add(UnifiedReal(BoundedRational(27315, 100)))
        },
        object : Unit {
            override val title = Res.string.rankine
            override val symbol = Res.string.code_rankine
            override val id = TAG + "_temp_rankine"
            override val group = Res.string.imperial_system_us

            override fun toBase(value: UnifiedReal) =
                value.subtract(UnifiedReal(BoundedRational(49167, 100)))
                    .multiply(UnifiedReal(BoundedRational(5, 9)))

            override fun toUnit(value: UnifiedReal) =
                value.add(UnifiedReal(BoundedRational(27315, 100)))
                    .multiply(UnifiedReal(BoundedRational(9, 5)))
        },
        object : Unit {
            override val title = Res.string.delisle
            override val symbol = Res.string.code_delisle
            override val id = "_temp_delisle"
            override val group = Res.string.unknown

            override fun toBase(value: UnifiedReal) =
                UnifiedReal(100).subtract(value.multiply(UnifiedReal(BoundedRational(2, 3))))

            override fun toUnit(value: UnifiedReal) = UnifiedReal(100).subtract(value)
                .multiply(UnifiedReal(BoundedRational(15, 10)))
        },
        object : Unit {
            override val title = Res.string.newton
            override val symbol = Res.string.code_newton
            override val id = "_temp_newton"
            override val group = Res.string.unknown

            override fun toBase(value: UnifiedReal) =
                value.multiply(UnifiedReal(BoundedRational(100, 33)))

            override fun toUnit(value: UnifiedReal) =
                value.multiply(UnifiedReal(BoundedRational(33, 100)))
        },
        object : Unit {
            override val title = Res.string.reaumur
            override val symbol = Res.string.code_reaumur
            override val id = "_temp_reaumur"
            override val group = Res.string.unknown

            override fun toBase(value: UnifiedReal) =
                value.multiply(UnifiedReal(BoundedRational(5, 4)))

            override fun toUnit(value: UnifiedReal) =
                value.multiply(UnifiedReal(BoundedRational(4, 5)))
        },
        object : Unit {
            override val title = Res.string.romer
            override val symbol = Res.string.code_romer
            override val id = "_temp_romer"
            override val group = Res.string.unknown

            override fun toBase(value: UnifiedReal) =
                value.subtract(UnifiedReal(BoundedRational(75, 10)))
                    .multiply(UnifiedReal(BoundedRational(40, 21)))

            override fun toUnit(value: UnifiedReal) =
                value.multiply(UnifiedReal(BoundedRational(21, 40))).add(
                    UnifiedReal(
                        BoundedRational(
                            75,
                            10
                        )
                    )
                )
        }

    )

//Base unit second
private val TimeUnits
    get() = listOf(
        Unit(
            "_time_nano_second",
            Res.string.system_international,
            Res.string.nanosecond,
            Res.string.code_nanosecond,
            UnifiedReal(
                BoundedRational(1, 1000000000)
            )
        ), Unit(
            "_time_micro_second",
            Res.string.system_international,
            Res.string.microsecond,
            Res.string.code_microsecond,
            UnifiedReal(
                BoundedRational(1, 1000000)
            )
        ), Unit(
            "_time_milli_second",
            Res.string.system_international,
            Res.string.millisecond,
            Res.string.code_millisecond,
            UnifiedReal(
                BoundedRational(1, 1000)
            )
        ), Unit(
            "_time_second",
            Res.string.system_international,
            Res.string.second,
            Res.string.code_second,
            UnifiedReal(
                BoundedRational(1, 1)
            )
        ), Unit(
            "_time_minute",
            Res.string.system_international,
            Res.string.minute,
            Res.string.code_minute,
            UnifiedReal(
                BoundedRational(60, 1)
            )
        ), Unit(
            "_time_hour",
            Res.string.system_international,
            Res.string.hour,
            Res.string.code_hour,
            UnifiedReal(
                BoundedRational(3600, 1)
            )
        ), Unit(
            "_time_day",
            Res.string.system_international,
            Res.string.day,
            Res.string.code_day,
            UnifiedReal(
                BoundedRational(86400, 1)
            )
        ), Unit(
            "_time_week",
            Res.string.system_international,
            Res.string.week,
            Res.string.code_week,
            UnifiedReal(
                BoundedRational(604800, 1)
            )
        ), Unit(
            "_time_year",
            Res.string.system_international,
            Res.string.year,
            Res.string.code_year,
            UnifiedReal(
                BoundedRational(31557600, 1)
            )
        )
    )

// Base unit
private val VolumeUnits
    get(): List<Unit> = listOf()
private val DataUnits
    get(): List<Unit> = listOf()

/**
 * @return array of units for [converter]
 */
fun Units(converter: String): List<Unit> {
    return when (converter) {
        UnitConverter.KEY_ANGLE_CONVERTER -> AngelUnits
        UnitConverter.KEY_AREA_CONVERTER -> AreaUnits
        UnitConverter.KEY_TIME_CONVERTER -> TimeUnits
        UnitConverter.KEY_SPEED_CONVERTER -> SpeedUnits
        UnitConverter.KEY_POWER_CONVERTER -> PowerUnits
        UnitConverter.KEY_ENERGY_CONVERTER -> EnergyUnits
        UnitConverter.KEY_TEMPERATURE_CONVERTER -> TempretureUnits
        UnitConverter.KEY_LENGTH_CONVERTER -> LengthUnits
        UnitConverter.KEY_PRESSURE_CONVERTER -> PressureUnits
        UnitConverter.KEY_VOLUME_CONVERTER -> TODO("Volume Units")
        UnitConverter.KEY_MASS_CONVERTER -> MassUnits
        UnitConverter.KEY_DATA_CONVERTER -> TODO("Data Units")
        else -> error("converter $converter not found") // should not happen
    }
}