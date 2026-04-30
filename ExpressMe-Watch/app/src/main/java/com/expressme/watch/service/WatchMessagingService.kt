package com.expressme.watch.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Serviço Firebase Cloud Messaging para o ExpressMe Watch.
 *
 * Responsável por:
 * 1. Receber mensagens push (caso necessário no futuro)
 * 2. Enviar notificações ao cuidador via POST HTTP para o FCM
 *
 * IMPORTANTE: Para produção, substitua FCM_SERVER_KEY pela chave
 * de servidor real do seu projeto Firebase, ou migre para um backend seguro.
 */
class WatchMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "WatchMessagingService"

        // URL da API legada do FCM para envio de mensagens
        private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"

        // TODO: Substituir pela chave de servidor real do Firebase Console
        // Firebase Console > Projeto > Configurações > Cloud Messaging > Server key
        private const val FCM_SERVER_KEY = "SUA_CHAVE_DE_SERVIDOR_AQUI"

        // Nome da criança (fixo por enquanto, pode ser configurável futuramente)
        private const val NOME_CRIANCA = "A criança"

        /**
         * Envia uma mensagem FCM para o tópico "cuidador" informando
         * a emoção da criança.
         *
         * @param emocao Nome da emoção (feliz, ajuda, desconfortavel, sair)
         * @param mensagem Descrição da mensagem (ex: "está feliz")
         * @param corHex Cor hexadecimal do botão (ex: "#4CAF50")
         */
        fun sendEmotionMessage(emocao: String, mensagem: String, corHex: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val horario = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                    val jsonBody = JSONObject().apply {
                        put("to", "/topics/cuidador")
                        put("data", JSONObject().apply {
                            put("emocao", emocao)
                            put("mensagem", "$NOME_CRIANCA $mensagem")
                            put("horario", horario)
                            put("cor", corHex)
                        })
                    }

                    val url = URL(FCM_URL)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Authorization", "key=$FCM_SERVER_KEY")
                        doOutput = true
                        connectTimeout = 10000
                        readTimeout = 10000
                    }

                    OutputStreamWriter(connection.outputStream).use { writer ->
                        writer.write(jsonBody.toString())
                        writer.flush()
                    }

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Log.d(TAG, "Mensagem FCM enviada com sucesso: $emocao")
                    } else {
                        Log.e(TAG, "Erro ao enviar FCM. Código: $responseCode")
                    }

                    connection.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Falha ao enviar mensagem FCM", e)
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Mensagem recebida: ${message.data}")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo token FCM: $token")
    }
}
