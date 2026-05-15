package com.expressme.companion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.expressme.companion.data.AlertaEntity
import com.expressme.companion.data.AppDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificacaoService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase

    companion object {
        private const val TAG = "NotificacaoService"
        const val CHANNEL_ID = "expressme_alerts"
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        createNotificationChannels()
        
        // Inscrever-se no tópico para receber as mensagens do relógio
        FirebaseMessaging.getInstance().subscribeToTopic("cuidador")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Inscrito no tópico 'cuidador' com sucesso.")
                } else {
                    Log.e(TAG, "Falha ao inscrever no tópico 'cuidador'.", task.exception)
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo token FCM: $token")
        FirebaseMessaging.getInstance().subscribeToTopic("cuidador")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // As mensagens enviadas pelo relógio estão no bloco "data" do payload
        val data = message.data
        if (data.isEmpty()) return

        val emocao = data["emocao"] ?: "outro"
        val mensagem = data["mensagem"] ?: "A criança enviou um alerta"
        val corHex = data["cor"] ?: "#808080"

        serviceScope.launch {
            val alerta = AlertaEntity(
                mensagem = mensagem,
                timestamp = System.currentTimeMillis(),
                tipo = emocao,
                lido = false
            )
            database.alertaDao().insert(alerta)
            showNotification(mensagem, emocao)
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelPadrao = NotificationChannel(
            CHANNEL_ID,
            "ExpressMe Alertas",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Alertas recebidos do smartwatch via Internet" }

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
