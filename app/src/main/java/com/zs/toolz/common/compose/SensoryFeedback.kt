package com.zs.toolz.common.compose

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import androidx.annotation.RawRes
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.SoundEffect
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat

// my custom interface that combines haptics and sound.
// it perfomrs haptics for sound as well.
interface SensoryFeedback : HapticFeedback, SoundEffect {

    // another method to perform haptic feedback that takes striegnth along with type.
    // the strenght goes from 0 to 1. that is compared to orif=ginal type.
    fun performHapticFeedback(type: HapticFeedbackType, strength: Float)

}


class AndroidSensoryFeedback(private val view: View): SensoryFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        val constant =
            when (hapticFeedbackType) {
                HapticFeedbackType.Confirm -> HapticFeedbackConstantsCompat.CONFIRM
                HapticFeedbackType.ContextClick -> HapticFeedbackConstantsCompat.CONTEXT_CLICK
                HapticFeedbackType.GestureEnd -> HapticFeedbackConstantsCompat.GESTURE_END
                HapticFeedbackType.GestureThresholdActivate ->
                    HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_ACTIVATE
                HapticFeedbackType.KeyboardTap -> HapticFeedbackConstantsCompat.KEYBOARD_TAP
                HapticFeedbackType.LongPress -> HapticFeedbackConstantsCompat.LONG_PRESS
                HapticFeedbackType.Reject -> HapticFeedbackConstantsCompat.REJECT
                HapticFeedbackType.SegmentFrequentTick ->
                    HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK
                HapticFeedbackType.SegmentTick -> HapticFeedbackConstantsCompat.SEGMENT_TICK
                HapticFeedbackType.TextHandleMove -> HapticFeedbackConstantsCompat.TEXT_HANDLE_MOVE
                HapticFeedbackType.ToggleOff -> HapticFeedbackConstantsCompat.TOGGLE_OFF
                HapticFeedbackType.ToggleOn -> HapticFeedbackConstantsCompat.TOGGLE_ON
                HapticFeedbackType.VirtualKey -> HapticFeedbackConstantsCompat.VIRTUAL_KEY
                else -> HapticFeedbackConstantsCompat.NO_HAPTICS
            }
        ViewCompat.performHapticFeedback(view, constant)
    }

    override fun playClickSound() {
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    override fun performHapticFeedback(type: HapticFeedbackType, strength: Float) {
      performHapticFeedback(type)
    }
}

class NoSensoryFeedback(): SensoryFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        // No-op
    }

    override fun playClickSound() {
        // No-op
    }

    override fun performHapticFeedback(
        type: HapticFeedbackType,
        strength: Float
    ) {
        // no-op
    }
}

class AppSensoryFeedback(
    context: Context,
    @RawRes clickSoundRes: Int,
    private val soundEnabled: Boolean = true,
    private val hapticsEnabled: Boolean = true,
) : SensoryFeedback {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var soundId = 0
    @Volatile private var loaded = false

    init {
        soundPool.setOnLoadCompleteListener { _, id, status ->
            if (id == soundId && status == 0) loaded = true
        }
        soundId = soundPool.load(context, clickSoundRes, 1)
    }

    /** How a haptic type feels: relative intensity (0..1), pulse length, sound pitch. */
    private class Profile(
        val intensity: Float,
        val durationMs: Long,
        val rate: Float,
        val pattern: LongArray? = null, // timings for multi-pulse effects (starts with a 0 delay)
    )

    private fun profileOf(type: HapticFeedbackType): Profile = when (type) {
        HapticFeedbackType.Confirm -> Profile(0.8f, 25, 1.1f, longArrayOf(0, 20, 40, 30))
        HapticFeedbackType.Reject -> Profile(1f, 25, 0.8f, longArrayOf(0, 30, 50, 30, 50, 30))
        HapticFeedbackType.LongPress -> Profile(1f, 45, 0.85f)
        HapticFeedbackType.ContextClick -> Profile(0.6f, 20, 1f)
        HapticFeedbackType.GestureEnd -> Profile(0.5f, 15, 0.95f)
        HapticFeedbackType.GestureThresholdActivate -> Profile(0.8f, 25, 1.05f)
        HapticFeedbackType.KeyboardTap -> Profile(0.4f, 10, 1.15f)
        HapticFeedbackType.VirtualKey -> Profile(0.6f, 15, 1.1f)
        HapticFeedbackType.SegmentFrequentTick -> Profile(0.25f, 6, 1.4f)
        HapticFeedbackType.SegmentTick -> Profile(0.4f, 8, 1.25f)
        HapticFeedbackType.TextHandleMove -> Profile(0.3f, 8, 1.2f)
        HapticFeedbackType.ToggleOn -> Profile(0.7f, 18, 1.15f)
        HapticFeedbackType.ToggleOff -> Profile(0.5f, 14, 0.9f)
        else -> Profile(0.5f, 15, 1f)
    }

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) =
        performHapticFeedback(hapticFeedbackType, 1f)

    override fun performHapticFeedback(type: HapticFeedbackType, strength: Float) {
        val s = strength.coerceIn(0f, 1f)
        if (s == 0f) return
        val profile = profileOf(type)
        val level = profile.intensity * s
        playSound(volume = 0.3f + 0.7f * level, rate = profile.rate)
        vibrate(profile, level)
    }

    // Goes through the haptic path, so click = sound + vibration (no double sound).
    override fun playClickSound() = performHapticFeedback(HapticFeedbackType.ContextClick)

    private fun playSound(volume: Float, rate: Float) {
        if (!soundEnabled || !loaded) return
        soundPool.play(soundId, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    private fun vibrate(profile: Profile, level: Float) {
        if (!hapticsEnabled) return
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            v.vibrate(profile.durationMs)
            return
        }
        val amplitude = if (v.hasAmplitudeControl()) {
            (255 * level).toInt().coerceIn(1, 255)
        } else {
            VibrationEffect.DEFAULT_AMPLITUDE
        }
        val effect = profile.pattern?.let { timings ->
            // 0 amplitude for the gaps, the computed amplitude for the pulses
            val amps = IntArray(timings.size) { i -> if (i % 2 == 0) 0 else amplitude }
            VibrationEffect.createWaveform(timings, amps, -1)
        } ?: VibrationEffect.createOneShot(profile.durationMs, amplitude)
        v.vibrate(effect)
    }

    /** Call when the owner is destroyed (e.g. from DisposableEffect). */
    fun release() {
        loaded = false
        soundPool.release()
    }
}