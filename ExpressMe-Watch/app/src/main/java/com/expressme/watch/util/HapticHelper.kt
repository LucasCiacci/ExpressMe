package com.expressme.watch.util

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Helper para feedback háptico (vibração) no Wear OS.
 * Usa VibrationEffect para criar uma vibração única de 200ms
 * com intensidade máxima, garantindo que a criança sinta o feedback.
 */
object HapticHelper {

    /**
     * Vibra o relógio uma vez por 200ms com intensidade máxima.
     * Deve ser chamado imediatamente ao toque do botão (<100ms de latência).
     */
    fun vibrate(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (it.hasVibrator()) {
                val effect = VibrationEffect.createOneShot(
                    200L, // duração em milissegundos
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
                it.vibrate(effect)
            }
        }
    }
}
