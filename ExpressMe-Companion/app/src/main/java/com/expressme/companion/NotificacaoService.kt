package com.expressme.companion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.expressme.companion.data.AlertaEntity
import com.expressme.companion.data.AppDatabase
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificacaoService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase

    companion object {
        const val CHANNEL_ID = "expressme_alerts"
        const val PATH_PREFIX = "/expressme_alerta"
        val TIPOS_VALIDOS = setOf("desconfortavel", "sair", "ajuda", "feliz")
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        createNotificationChannels()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (!messageEvent.path.startsWith(PATH_PREFIX)) return

        val mensagem = String(messageEvent.data)
        val tipo = extrairTipoDoPath(messageEvent.path) ?: inferirTipoPorTexto(mensagem)

        serviceScope.launch {
            val alerta = AlertaEntity(
                mensagem = mensagem,
                timestamp = System.currentTimeMillis(),
                tipo = tipo,
                lido = false
            )
            database.alertaDao().insert(alerta)
            showNotification(mensagem, tipo)
        }
    }

    /** Extrai tipo do path estruturado: /expressme_alerta/ajuda -> "ajuda" */
    private fun extrairTipoDoPath(path: String): String? {
        val sufixo = path.removePrefix(PATH_PREFIX).trimStart('/')
        return if (sufixo.isNotEmpty() && sufixo in TIPOS_VALIDOS) sufixo else null
    }

    /** Fallback por texto para compatibilidade com versão antiga do Watch */
    private fun inferirTipoPorTexto(mensagem: String): String {
        return when {
            mensagem.contains("desconfortável", ignoreCase = true) -> "desconfortavel"
            mensagem.contains("sair", ignoreCase = true)           -> "sair"
            mensagem.contains("ajuda", ignoreCase = true)          -> "ajuda"
            mensagem.contains("feliz", ignoreCase = true)          -> "feliz"
            else                                                    -> "outro"
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelPadrao = NotificationChannel(
            CHANNEL_ID,
            "ExpressMe Alertas",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Alertas recebidos do smartwatch" }

        val channelUrgente = NotificationChannel(
            "${CHANNEL_ID}_urgente",
            "ExpressMe — Ajuda Urgente",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas de ajuda urgente do smartwatch"
            enableVibration(true)
            vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
        }

        nm.createNotificationChannel(channelPadrao)
        nm.createNotificationChannel(channelUrgente)
    }

    private fun showNotification(mensagem: String, tipo: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val isUrgente = tipo == "ajuda"
        val channelAtivo = if (isUrgente) "${CHANNEL_ID}_urgente" else CHANNEL_ID

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prefs = getSharedPreferences("expressme_prefs", Context.MODE_PRIVATE)
        val nomePessoa = prefs.getString("nome_pessoa", "Alguém") ?: "Alguém"

        val emoji = when (tipo) {
            "desconfortavel" -> "😣"
            "sair"           -> "🚪"
            "ajuda"          -> "🆘"
            "feliz"          -> "😊"
            else             -> "📣"
        }

        val builder = NotificationCompat.Builder(this, channelAtivo)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("$emoji Alerta de $nomePessoa")
            .setContentText(mensagem)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensagem))
            .setPriority(
                if (isUrgente) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (isUrgente) {
            builder.setVibrate(longArrayOf(1000, 1000, 1000, 1000))
        }

        nm.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
