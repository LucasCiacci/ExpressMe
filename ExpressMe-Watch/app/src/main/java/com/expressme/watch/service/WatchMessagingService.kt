package com.expressme.watch.service

import android.content.Context
import android.util.Log
import com.expressme.watch.R
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 * Agora utiliza a API V1 do Firebase Cloud Messaging e gera
 * automaticamente o token OAuth2 usando a chave de serviço local.
 */
class WatchMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "WatchMessagingService"

        // O ID do seu projeto Firebase (extraído do seu JSON)
        private const val PROJECT_ID = "expressme-6b363"
        
        // URL da API V1 do FCM
        private const val FCM_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"

        // Nome da criança
        private const val NOME_CRIANCA = "A criança"

        /**
         * Gera o token OAuth2 e envia a mensagem para a API V1 do Firebase.
         */
        fun sendEmotionMessage(context: Context, emocao: String, mensagem: String, corHex: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. LER A CHAVE E GERAR O TOKEN (OAuth2)
                    // Pega o arquivo res/raw/chave_firebase.json
                    val inputStream = context.resources.openRawResource(R.raw.chave_firebase)
                    val credentials = GoogleCredentials.fromStream(inputStream)
                        .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
                    
                    credentials.refreshIfExpired()
                    val token = credentials.accessToken.tokenValue

                    // 2. MONTAR O PAYLOAD
                    val horario = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    
                    // O formato da API V1 exige o encapsulamento dentro de "message"
                    val messageJson = JSONObject().apply {
                        put("topic", "cuidador")
                        put("data", JSONObject().apply {
                            put("emocao", emocao)
                            put("mensagem", "$NOME_CRIANCA $mensagem")
                            put("horario", horario)
                            put("cor", corHex)
                        })
                    }

                    val rootJson = JSONObject().apply {
                        put("message", messageJson)
                    }

                    // 3. FAZER A REQUISIÇÃO (POST)
                    val url = URL(FCM_URL)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Authorization", "Bearer $token")
                        doOutput = true
                        connectTimeout = 15000
                        readTimeout = 15000
                    }

                    OutputStreamWriter(connection.outputStream).use { writer ->
                        writer.write(rootJson.toString())
                        writer.flush()
                    }

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Log.d(TAG, "Mensagem FCM V1 enviada com sucesso: \$emocao")
                    } else {
                        val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e(TAG, "Erro ao enviar FCM V1. Código: \$responseCode - Detalhe: \$errorStream")
                    }

                    connection.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Falha ao enviar mensagem FCM V1", e)
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Mensagem recebida do FCM: \${message.data}")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo token FCM do relógio: \$token")
    }
}
