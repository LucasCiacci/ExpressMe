package com.expressme.watch.service

import android.content.Context
import android.util.Base64
import android.util.Log
import com.expressme.watch.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WatchMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "WatchMessagingService"
        private const val PROJECT_ID = "expressme-6b363"
        private const val FCM_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"
        private const val NOME_CRIANCA = "A criança"

        fun sendEmotionMessage(context: Context, emocao: String, mensagem: String, corHex: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. Gerar token manualmente para evitar problemas de relógio do emulador (Clock Skew)
                    val token = generateAccessToken(context)
                    if (token == null) {
                        Log.e(TAG, "Não foi possível gerar o token de acesso.")
                        return@launch
                    }

                    // 2. MONTAR O PAYLOAD
                    val horario = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    val messageJson = JSONObject().apply {
                        put("topic", "cuidador")
                        put("data", JSONObject().apply {
                            put("emocao", emocao)
                            put("mensagem", "$NOME_CRIANCA $mensagem")
                            put("horario", horario)
                            put("cor", corHex)
                        })
                    }

                    val rootJson = JSONObject().apply { put("message", messageJson) }

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
                        Log.d(TAG, "Mensagem FCM V1 enviada com sucesso: $emocao")
                    } else {
                        val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e(TAG, "Erro ao enviar FCM V1. Código: $responseCode - Detalhe: $errorStream")
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Falha ao enviar mensagem FCM V1", e)
                }
            }
        }

        private fun generateAccessToken(context: Context): String? {
            try {
                // Ler JSON
                val jsonStr = context.resources.openRawResource(R.raw.chave_firebase).bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val clientEmail = json.getString("client_email")
                var privateKeyStr = json.getString("private_key")

                // Extrair apenas a chave base64
                privateKeyStr = privateKeyStr.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\\n", "")
                    .replace("\n", "")
                    .replace("\\r", "")
                    .replace("\r", "")
                    .trim()

                val keyBytes = Base64.decode(privateKeyStr, Base64.DEFAULT)
                val keySpec = PKCS8EncodedKeySpec(keyBytes)
                val kf = KeyFactory.getInstance("RSA")
                val privateKey: PrivateKey = kf.generatePrivate(keySpec)

                val privateKeyId = json.getString("private_key_id")

                // Headers e Claims
                val header = JSONObject().apply {
                    put("alg", "RS256")
                    put("typ", "JWT")
                    put("kid", privateKeyId)
                }
                
                // Subtrair 5 minutos (300 segundos) para evitar erro de "Token do futuro" no Emulador
                val now = System.currentTimeMillis() / 1000
                val iat = now - 300
                val exp = iat + 3600 // O tempo de expiração NUNCA pode ser mais de 3600s depois do iat

                val claim = JSONObject().apply {
                    put("iss", clientEmail)
                    put("scope", "https://www.googleapis.com/auth/firebase.messaging")
                    put("aud", "https://oauth2.googleapis.com/token")
                    put("exp", exp)
                    put("iat", iat)
                }

                val encodedHeader = Base64.encodeToString(header.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                val encodedClaim = Base64.encodeToString(claim.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                val signatureInput = "$encodedHeader.$encodedClaim"

                val signature = Signature.getInstance("SHA256withRSA")
                signature.initSign(privateKey)
                signature.update(signatureInput.toByteArray())
                val signatureBytes = signature.sign()
                val encodedSignature = Base64.encodeToString(signatureBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

                val jwt = "$signatureInput.$encodedSignature"

                // Solicitar Token
                val url = URL("https://oauth2.googleapis.com/token")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    doOutput = true
                }

                val body = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$jwt"
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body)
                    writer.flush()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = InputStreamReader(connection.inputStream).readText()
                    val responseJson = JSONObject(responseStr)
                    return responseJson.getString("access_token")
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "Falha ao obter token OAuth2: ${connection.responseCode} - $err")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro na geração do token JWT", e)
            }
            return null
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}
