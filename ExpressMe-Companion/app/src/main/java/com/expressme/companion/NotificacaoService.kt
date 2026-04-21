package com.expressme.companion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.expressme.companion.data.AlertaEntity
import com.expressme.companion.data.AppDatabase
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificacaoService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/expressme_alerta") {
            val mensagem = String(messageEvent.data)

            val tipo = when {
                mensagem.contains("desconfortável", ignoreCase = true) -> "desconfortavel"
                mensagem.contains("sair", ignoreCase = true) -> "sair"
                mensagem.contains("ajuda", ignoreCase = true) -> "ajuda"
                mensagem.contains("feliz", ignoreCase = true) -> "feliz"
                else -> "outro"
            }

            serviceScope.launch {
                val alerta = AlertaEntity(
                    mensagem = mensagem,
                    timestamp = System.currentTimeMillis(),
                    tipo = tipo
                )
                database.alertaDao().insert(alerta)
                showNotification(mensagem, tipo)
            }
        }
    }

    private fun showNotification(mensagem: String, tipo: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "expressme_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ExpressMe Alertas"
            val importance = if (tipo == "ajuda") {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            }
            val channel = NotificationChannel(channelId, name, importance)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Alerta do Lucas")
            .setContentText(mensagem)
            .setPriority(if (tipo == "ajuda") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (tipo == "ajuda") {
            builder.setVibrate(longArrayOf(1000, 1000, 1000, 1000))
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
