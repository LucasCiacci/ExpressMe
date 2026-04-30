package com.expressme.watch.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.expressme.watch.R

/**
 * Helper para reprodução de áudio correspondente a cada emoção.
 * Usa MediaPlayer para tocar os arquivos .mp3 armazenados em res/raw/.
 * O MediaPlayer é liberado automaticamente após a conclusão do áudio.
 */
object AudioHelper {

    private const val TAG = "AudioHelper"

    // Mapeamento de nome de emoção para resource ID do áudio
    private val audioMap = mapOf(
        "feliz" to R.raw.audio_feliz,
        "ajuda" to R.raw.audio_ajuda,
        "desconfortavel" to R.raw.audio_desconfortavel,
        "sair" to R.raw.audio_sair
    )

    /**
     * Reproduz o áudio correspondente à emoção informada.
     * @param context Contexto da aplicação
     * @param emocao Nome da emoção (feliz, ajuda, desconfortavel, sair)
     */
    fun play(context: Context, emocao: String) {
        val resId = audioMap[emocao]
        if (resId == null) {
            Log.w(TAG, "Áudio não encontrado para emoção: $emocao")
            return
        }

        try {
            val mediaPlayer = MediaPlayer.create(context, resId)
            if (mediaPlayer == null) {
                Log.w(TAG, "MediaPlayer retornou null para: $emocao (arquivo placeholder vazio?)")
                return
            }
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
            }
            mediaPlayer.setOnErrorListener { mp, _, _ ->
                Log.e(TAG, "Erro ao reproduzir áudio: $emocao")
                mp.release()
                true
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao reproduzir áudio: $emocao", e)
        }
    }
}
