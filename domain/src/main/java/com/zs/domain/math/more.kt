package com.zs.domain.math

import java.math.BigInteger

object NumUtil {

    /**
     * The symbol used to represent exponent part of a number.
     */
    const val EXPONENT = 'E'

    /**
     * The symbol for the decimal point of a number.
     */
    const val DECIMAL = '.'

    /**
     * The symbol for the zero digit
     */
    const val ZERO = '0'

    /**
     * Removes Front zeros from the whole part of a number.
     *
     * @param whole The Whole Part of a number.
     * @return modified number.
     */
    fun removeFrontZeroes(whole: String): String {

        //FIXME: Use whole number not just 'whole' part of number.

        val s = StringBuilder(whole)
        val lastWasZero = whole.indexOf("0") == 0 && s.length > 1
        var i = 0
        while (i < s.length) {
            if (lastWasZero && s[i] == '0' && i + 1 < s.length && s[i + 1] != '.') {
                s.deleteCharAt(i)
                continue
            }
            i++
        }
        return s.toString()
    }

    /**
     * Strips Trailing Zeros to from fraction part of a [String] representation of a number.
     *
     * @param fraction [String] representation of Number
     * @return [String] representation of modified number.
     */
    fun stripTrailingZeroes(fraction: String): String {

        // FIXME: Incorporate whole number not just fraction part.

        // if the provided number doesn't contain the decimal; then return the
        // provided number without any modifications
        // if (!fraction.contains(DECIMAL)) return fraction

        // else remove/strip th zeros from the fraction part
        val s = StringBuilder(fraction)
        var lastWasZero = fraction.lastIndexOf("0") == fraction.length - 1
        for (i in s.length - 1 downTo 0) {
            if ((s[i] == '0' || s[i] == '.') && lastWasZero) {
                if (s[i] == '.') lastWasZero = false
                s.deleteCharAt(i)
            } else lastWasZero = false
        }
        return s.toString()
    }


    /**
     * A helper function to split a number into 3 parts; whole, fraction and exponent.
     *
     * **Note**
     *  if the component (say fraction) is missing; while the symbol i.e., [DECIMAL] is present
     *  present empty is returned else *null* at its place in the resulting array.
     *
     * ***For E.g.***
     * If the given number is 12345.6789E12345 it will return a string array where
     * a[0] = 12345 (i.e,  Whole Part)
     * a[1] = .6789  (i.e., Fractional Part)
     * a[2] = 12345 (i.e., Scientific part)
     *
     * @param number: The number to split e.g., 123.45E789
     * @return [Array] of size 3 with whole, fraction and exponent at respective hole.
     */
    fun split(number: String): Array<String?> {

        //FIXME: Find out what happens to minus sign.

        val array = arrayOfNulls<String>(3)

        // The index of the components or null
        // Why null?
        // because it is easy to work with null than -1
        val iFraction = number.indexOf(DECIMAL).let { if (it == -1) null else it }
        val iExponent = number.indexOf(EXPONENT).let { if (it == -1) null else it }

        // why not use split method.
        // first it never returns array of size 3 every time.
        // it returns array list than again we have to check if properly it has worked; adds multiple overheads

        // whole part
        array[0] = number
            .substring(0, iFraction ?: iExponent ?: number.length)
        //.let { it.ifEmpty { null } }

        // fractional part
        array[1] =
            if (iFraction == null)
                null
            else
                number
                    .substring(iFraction + 1, iExponent ?: number.length)
        // if the fractional decimal present but no number
        // return zero in its place.
        //.let { it.ifEmpty { null } }

        // exponent
        array[2] =
            if (iExponent == null)
                null
            else
                number
                    .substring(iExponent + 1, number.length)
        // same logic as stated above.
        //.let { it.ifEmpty { null } }

        // return
        return array
    }

    /**
     * Return a copy of the supplied string with [separator] added every three digits.
     * Inserting a digit separator every 3 digits appears to be
     * at least somewhat acceptable, though not necessarily preferred, everywhere.

     * The grouping separator in the result is NOT localized.
     * @param whole: The number as whole in the format 123.456E789,
     * @param separator: The provided separator char like ,
     */
    fun addThousandSeparators(whole: String, separator: Char): String {

        //FIXME: Find out what happens to minus sign. and incorparate whole number not just 'whole' part

        // add separators to the resulting whole part.
        return buildString {
            val begin = 0
            val end = whole.length

            var current = begin
            append(whole, begin, current)
            while (current < end) {
                append(whole[current])
                ++current
                if ((end - current) % 3 == 0 && end != current) {
                    append(separator)
                }
            }
        }
    }


    /**
     * Renders a real number to a String (for user display).
     * @param maxLen the maximum total length of the resulting string
     * @param rounding the number of final digits to round
     */
    @Deprecated("not recommended.", level = DeprecationLevel.WARNING)
    fun doubleToString(x: Double, maxLen: Int, rounding: Int): String? {
        return Util.sizeTruncate(Util.doubleToString(x, rounding), maxLen)
    }
}

@Deprecated("not recommended.", level = DeprecationLevel.HIDDEN)
        /**
         * Stringifies the value of this [UnifiedReal]'s double value.
         * TODO: Move away from double
         */
fun UnifiedReal.stringfy(len: Int, rounding: Int): String =
    Util.doubleToString(doubleValue(), len, rounding)

/**
 * Constructs a unified real from any text. e.g., 1234.4565465
 */
@Suppress("FunctionName")
fun UnifiedReal(text: String): UnifiedReal {
    // split number in respective components.
    val (whole, fraction, exponent) = NumUtil.split(text)
    // Through error if an string with exponent is supplied.
    if (exponent != null)
        error("Current Implantation doesn't support exponent!!")

    // construct the numerator from the provided info
    val numerator = BigInteger(
        // replace with zero if null
        (whole ?: "0") + (fraction ?: "")
    )
    // construct the denominator from the provided info.
    val denominator = BigInteger.TEN.pow(
        // 10 power zero i.e., if fractional part empty
        fraction?.length ?: 0
    )
    // currently we don't support exponents
    return UnifiedReal(BoundedRational(numerator, denominator))
}