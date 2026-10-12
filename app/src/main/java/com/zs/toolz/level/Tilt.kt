package com.zs.toolz.level

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LongState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** Radians to degrees as a Float constant. */
private const val RAD_TO_DEG = (180.0 / PI).toFloat()

// ---------------------------------------------------------------------------
// Layout (file-private)
// ---------------------------------------------------------------------------
//
// Every field uses the same step, 1/8192 degree (about 0.0001 deg), but gets only
// as many bits as its range needs:
//
//   field | range needed | bits | representable range
//   ------+--------------+------+--------------------
//   pitch |  -90..90     |  21  | about -128 .. +128
//   roll  | -180..180    |  22  | about -256 .. +256
//   spin  |  -90..90     |  21  | about -128 .. +128
//
// The headroom matters: after subtracting a calibration offset, pitch and spin
// can pass 90 and roll can pass 180.
//
// Bit layout (low to high). All 64 bits are used, so there is no room for a
// fourth field:
//   pitch = bits  0..20
//   roll  = bits 21..42
//   spin  = bits 43..63
//
// Fields are two's complement, so all-zero bits mean 0 degrees. The spin field
// ends at bit 63, the sign bit of the Long. That is fine, because every field is
// read by shifting it out on its own and the Long is never compared as a number.
//
// Special float values: a fixed-point integer has no NaN or infinity, so three
// values at the ends of each field are reserved as markers:
//
//   NaN        = most negative value          (min)
//   -Infinity  = one above the most negative  (min + 1)
//   +Infinity  = most positive value          (max)
//
// Finite angles use the values in between, min + 2 .. max - 1, which is
// symmetric around 0. The cost is about 0.0002 degrees at each end of the range,
// far outside any real reading. A finite angle that is too large clamps to that
// limit; it never turns into an infinity. Zero is never a marker, so [Tilt.Zero]
// stays a valid all-zero tilt.
//
// Limits of this scheme: every NaN reads back as the same plain NaN (payload bits
// are lost), -0.0 becomes 0.0, and tiny values round to 0.
//
// Precision: a Float has 24 bits of mantissa, so every unit count here (at most
// 22 bits) converts to Float and back exactly.
//
// Trading precision for room: SCALE = 1024f with widths 18 / 19 / 18 gives a step
// of about 0.001 degrees and leaves 9 spare bits (55..63) for extra fields. The
// shifts are derived from the widths, so they update on their own. Keep
// SPIN_SHIFT + SPIN_BITS at or below 64.
//
// Changing the widths or SCALE changes the stored format. Raw Longs saved with
// another layout will read back wrong.

/** Units per degree. A power of two, so scaling by it is exact. */
private const val SCALE = 8192f

private const val PITCH_BITS = 21
private const val ROLL_BITS = 22
private const val SPIN_BITS = 21

private const val PITCH_SHIFT = 0
private const val ROLL_SHIFT = PITCH_SHIFT + PITCH_BITS   // 21
private const val SPIN_SHIFT = ROLL_SHIFT + ROLL_BITS     // 43

private fun mask(bits: Int): Long = (1L shl bits) - 1

private fun minUnits(bits: Int): Long = -(1L shl (bits - 1))

private fun maxUnits(bits: Int): Long = (1L shl (bits - 1)) - 1

/**
 * Degrees to fixed-point units for a field of [bits] bits.
 * NaN and the two infinities become their reserved markers (see the layout notes).
 * Finite values are rounded and clamped to the range between the markers.
 */
private fun pack(degrees: Float, bits: Int): Long {
    val min = minUnits(bits)
    val max = maxUnits(bits)
    return when {
        degrees.isNaN() -> min
        degrees == Float.NEGATIVE_INFINITY -> min + 1
        degrees == Float.POSITIVE_INFINITY -> max
        // degrees * SCALE can overflow to infinity for huge finite input;
        // roundToLong saturates, and coerceIn then clamps it.
        else -> (degrees * SCALE).roundToLong().coerceIn(min + 2, max - 1)
    }
}

/**
 * Reads a signed field of [bits] bits starting at bit [shift]: shift it to the top
 * of the Long, then arithmetic-shift back down so the sign extends.
 * The reserved markers read back as NaN, -Infinity and +Infinity.
 *
 * Note: Tilt equality compares the raw Long, so two tilts with a NaN field are
 * equal, unlike Float NaN, which never equals itself.
 */
private fun unpack(raw: Long, shift: Int, bits: Int): Float {
    val units = (raw shl (64 - shift - bits)) shr (64 - bits)
    return when (units) {
        minUnits(bits) -> Float.NaN
        minUnits(bits) + 1 -> Float.NEGATIVE_INFINITY
        maxUnits(bits) -> Float.POSITIVE_INFINITY
        else -> units.toFloat() / SCALE
    }
}

/**
 * Pitch, roll and spin (degrees) packed into one [Long], modeled on Compose's
 * `Offset`: a zero-allocation value class with `Zero` and `Unspecified`,
 *  / `isFinite`, `takeOrElse`, `copy`, destructuring,
 * `unaryMinus` / `times` and [lerp].
 *
 * Axes follow Android's device frame (x across the screen, y up the screen,
 * z out of the screen):
 * - [pitch]: rotation about x, -90..90
 * - [roll]: rotation about y, -180..180
 * - [spin]: rotation about z (in-plane screen rotation), -90..90
 *
 * Unlike `Offset`, which packs two 32-bit floats, three floats do not fit in 64
 * bits, so each angle is stored as fixed-point (1/8192 degree). NaN, +Infinity
 * and -Infinity survive packing; out-of-range finite values clamp; -0.0 reads
 * back as 0.0. A raw value of 0 means 0 degrees for all three ([Zero]).
 *
 * Operators compute in Float and pack once, so rounding happens only once.
 * Build one with `Tilt(pitch, roll, spin)`, or wrap a stored raw Long with
 * `Tilt(rawValue)`. Layout and widths are documented below.
 */
@Immutable
@JvmInline
value class Tilt(val rawValue: Long) {

    @Stable
    val pitch: Float get() = unpack(rawValue, PITCH_SHIFT, PITCH_BITS)
    @Stable
    val roll: Float get() = unpack(rawValue, ROLL_SHIFT, ROLL_BITS)
    @Stable
    val spin: Float get() = unpack(rawValue, SPIN_SHIFT, SPIN_BITS)

    @Stable
    operator fun component1(): Float = pitch
    @Stable
    operator fun component2(): Float = roll
    @Stable
    operator fun component3(): Float = spin

    fun copy(
        pitch: Float = this.pitch,
        roll: Float = this.roll,
        spin: Float = this.spin
    ): Tilt = Tilt(pitch, roll, spin)

    /** False only for [Unspecified]. */
    @Stable
    val isSpecified: Boolean get() = rawValue != Unspecified.rawValue

    /** This if specified, otherwise the result of [block]. */
    @Stable
    inline fun takeOrElse(block: () -> Tilt): Tilt = if (isSpecified) this else block()

    override fun toString(): String =
        if (isSpecified) {
            String.format(Locale.US, "Tilt(pitch=%.4f, roll=%.4f, spin=%.4f)", pitch, roll, spin)
        } else {
            "Tilt.Unspecified"
        }

    companion object {
        /** All three angles 0. */
        @Stable
        val Zero = Tilt(0L)

        /** All three angles NaN: "no reading". Check with [isSpecified]. */
        @Stable
        val Unspecified = Tilt(Float.NaN, Float.NaN, Float.NaN)

        @Stable infix fun Long.of(tag: Companion): Tilt = Tilt(this)
    }
}

/**
 * Packs the three angles (degrees) into a [Tilt].
 *
 * Do all math in Float first and call this last, so rounding happens only once.
 */
@Stable
fun Tilt(pitch: Float, roll: Float, spin: Float): Tilt {
    val p = pack(pitch, PITCH_BITS)
    val r = pack(roll, ROLL_BITS)
    val s = pack(spin, SPIN_BITS)
    return Tilt(
        (p and mask(PITCH_BITS)) or
                ((r and mask(ROLL_BITS)) shl ROLL_SHIFT) or
                ((s and mask(SPIN_BITS)) shl SPIN_SHIFT)
    )
}

/**
 * Live, smoothed [Tilt] (pitch, roll, spin) of the phone, as Compose state.
 *
 * The state holds [Tilt.rawValue]; wrap it with `Tilt(raw)` to read the angles.
 * It starts at zero (level) until the first reading arrives. When the device has no
 * accelerometer it holds [Tilt.Unspecified], the "not a number" value of a packed
 * Tilt (a Long cannot hold a real NaN). No calibration is applied here.
 *
 * ```
 * val raw = rememberTilt()
 * BubbleLevel(tilt = { Tilt(raw.longValue) })
 * ```
 *
 * @param filterWindowMs smoothing time in milliseconds: the time to cover 63% of a
 * sudden change (95% in three times this). About 150 feels like a liquid settling;
 * lower is quicker but shakier. 0 turns smoothing off.
 */
@Composable
fun rememberTilt(
    samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME,
    filterWindowMs: Long = 150L
): LongState {
    //   How it works, in simple words:
    //   1. The accelerometer feels gravity, which always pulls straight down.
    //      Which way gravity points inside the phone tells us how the phone is tilted.
    //   2. The raw reading shakes a little, so we smooth it (like a liquid settling).
    //   3. From the smoothed gravity we compute three angles and pack them into one Long.
    val context = LocalContext.current

    // State: the packed angles as one primitive Long (no boxing). It starts at zero
    // (level), so the screen shows "level" until the first reading arrives.
    // Needs Compose runtime 1.5+.
    val tilt = remember { mutableLongStateOf(Tilt(0f, 0f, 0f).rawValue) }

    // Effect: turns the sensor on when this enters the screen and off when it
    // leaves (or restarts it if one of the keys changes).
    // To also pause it while the app is in the background, register inside
    // LifecycleResumeEffect (androidx.lifecycle:lifecycle-runtime-compose).
    DisposableEffect(context, samplingPeriodUs, filterWindowMs) {
        // applicationContext avoids holding on to an Activity.
        val sensorManager =
            context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Null when the device has no accelerometer: report Unspecified (the "NaN"
        // of a packed Tilt) and stop.
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            tilt.longValue = Tilt.Unspecified.rawValue
            return@DisposableEffect onDispose { }
        }

        val listener = object : SensorEventListener {
            // The smoothed gravity (fx, fy, fz) and the time of the last reading.
            // They live here so every new registration starts fresh.
            private var fx = 0f
            private var fy = 0f
            private var fz = 0f
            private var lastTimestampNs = 0L

            override fun onSensorChanged(event: SensorEvent) {
                // Gravity as felt by the phone, as an arrow with three parts:
                // x across the screen, y up the screen, z out of the screen.
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Step 1 - Throw away broken readings.
                // rawG is the length of the gravity arrow (3D Pythagoras). A phone at
                // rest always feels about 9.8 (Earth's pull), so a healthy reading is
                // a positive, finite number. A length of 0, NaN or infinity means the
                // sensor glitched. If any one of x, y, z is NaN or infinite, rawG is
                // too, so a single check catches all three. The test is written
                // "not (good)" because every comparison with NaN is false, so this
                // also rejects NaN.
                // Must come BEFORE smoothing: one bad number would poison it forever.
                val rawG = sqrt(x * x + y * y + z * z)
                if (!(rawG > 0f && rawG < Float.POSITIVE_INFINITY)) return

                // Step 2 - Smooth the gravity (fx, fy, fz is the smoothed copy).
                // Think of the smoothed value as a heavy ball on a rubber band: the
                // raw reading tugs it, but it only moves part of the way each time.
                //   new = old + alpha * (reading - old)
                // alpha is between 0 and 1: 0.1 means "move 10% of the way toward the
                // reading". Quick random shakes (up, down, up, down) tug in opposite
                // directions and cancel out. A real tilt keeps tugging the same way,
                // so the value follows it, just a little late.
                //
                // alpha = dt / (smoothingTime + dt), where dt is the time since the
                // last reading. A longer wait means a bigger step, so the result feels
                // the same at any sampling rate. After smoothingTime the value has
                // covered 63% of a sudden change, and 95% after three times that.
                //
                // We smooth the gravity, not the angles: roll jumps from +180 to -180
                // and averaging those would give 0, while gravity averages cleanly.
                if (lastTimestampNs == 0L) {
                    // First reading: start right there instead of rising from zero.
                    fx = x
                    fy = y
                    fz = z
                } else {
                    val dt = (event.timestamp - lastTimestampNs).coerceAtLeast(0L).toFloat()
                    val smoothingNs = filterWindowMs * 1_000_000f
                    val alpha = if (filterWindowMs <= 0L) 1f else dt / (smoothingNs + dt)
                    fx += alpha * (x - fx)
                    fy += alpha * (y - fy)
                    fz += alpha * (z - fz)
                }
                lastTimestampNs = event.timestamp

                // Step 3 - Turn the smoothed gravity into angles. Only the direction
                // of gravity matters, so its strength g is divided out.
                //
                //   pitch = asin(-y / g)            tipping forward/back, -90..90
                //   roll  = atan2(x, z)             tipping left/right, -180..180
                //   spin  = asin(x / hypot(x, y))   turning on the table, -90..90
                //
                // The angles follow the phone's body, not the screen rotation, so a
                // pose gives the same angles in portrait and landscape.
                val g = sqrt(fx * fx + fy * fy + fz * fz)
                if (!(g > 0f)) return

                val pitch = asin((-fy / g).coerceIn(-1f, 1f)) * RAD_TO_DEG
                val roll = atan2(fx, fz) * RAD_TO_DEG

                // Spin is just noise when the phone lies flat (x and y near 0), and
                // roll is just noise when it stands on its top or bottom edge. Ignore
                // them in those poses where the position is chosen, not here.
                val inPlane = hypot(fx, fy)
                val spin =
                    if (inPlane == 0f) 0f
                    else asin((fx / inPlane).coerceIn(-1f, 1f)) * RAD_TO_DEG

                // Step 4 - Pack the three angles into one Long (rounded once, at the
                // end). Calibration offsets are NOT applied here; subtract them
                // per position before showing the values.
                tilt.longValue = Tilt(pitch, roll, spin).rawValue
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        // Start listening. SENSOR_DELAY_GAME is about 50 readings per second; rates
        // above 200 Hz need HIGH_SAMPLING_RATE_SENSORS permission on Android 12+.
        sensorManager.registerListener(listener, accelerometer, samplingPeriodUs)

        onDispose { sensorManager.unregisterListener(listener) }
    }

    return tilt
}