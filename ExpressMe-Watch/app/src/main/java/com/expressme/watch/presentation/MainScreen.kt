package com.expressme.watch.presentation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.expressme.watch.service.WatchMessagingService
import com.expressme.watch.util.AudioHelper
import com.expressme.watch.util.HapticHelper

/**
 * Dados de cada emoção exibida como botão na tela principal.
 */
data class Emotion(
    val name: String,       // identificador interno (feliz, ajuda, etc.)
    val label: String,      // texto exibido no botão
    val emoji: String,      // emoji representativo
    val message: String,    // mensagem enviada ao cuidador
    val color: Color,       // cor de fundo do botão
    val colorHex: String    // hex da cor para enviar via FCM
)

// Lista das 4 emoções do app
val emotions = listOf(
    Emotion("feliz", "FELIZ", "😄", "está feliz", Color(0xFF4CAF50), "#4CAF50"),
    Emotion("ajuda", "AJUDA", "🆘", "precisa de ajuda", Color(0xFFF44336), "#F44336"),
    Emotion("desconfortavel", "DESCON-\nFORTÁVEL", "😣", "está desconfortável", Color(0xFFFF9800), "#FF9800"),
    Emotion("sair", "SAIR", "🚪", "quer sair", Color(0xFF009688), "#009688")
)

/**
 * Tela principal do ExpressMe Watch.
 * Exibe uma grade 2x2 com botões grandes para cada emoção.
 * Ocupa a tela inteira sem scroll.
 *
 * @param onEmotionSelected Callback chamado quando uma emoção é selecionada,
 *                          passando a Emotion escolhida para navegação.
 */
@Composable
fun MainScreen(onEmotionSelected: (Emotion) -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Linha superior: FELIZ e AJUDA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                EmotionButton(
                    emotion = emotions[0],
                    context = context,
                    onEmotionSelected = onEmotionSelected,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                EmotionButton(
                    emotion = emotions[1],
                    context = context,
                    onEmotionSelected = onEmotionSelected,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
            // Linha inferior: DESCONFORTÁVEL e SAIR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                EmotionButton(
                    emotion = emotions[2],
                    context = context,
                    onEmotionSelected = onEmotionSelected,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                EmotionButton(
                    emotion = emotions[3],
                    context = context,
                    onEmotionSelected = onEmotionSelected,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

/**
 * Botão individual de emoção.
 * Exibe emoji + label com cor de fundo sólida.
 * Ao ser tocado: vibra, toca áudio, envia FCM e navega para confirmação.
 */
@Composable
fun EmotionButton(
    emotion: Emotion,
    context: Context,
    onEmotionSelected: (Emotion) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(emotion.color)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                // 1. Vibração imediata (feedback <100ms)
                HapticHelper.vibrate(context)

                // 2. Reproduzir áudio correspondente
                AudioHelper.play(context, emotion.name)

                // 3. Enviar notificação FCM (fire-and-forget, roda em Dispatchers.IO)
                WatchMessagingService.sendEmotionMessage(
                    emocao = emotion.name,
                    mensagem = emotion.message,
                    corHex = emotion.colorHex
                )

                // 4. Navegar para tela de confirmação
                onEmotionSelected(emotion)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Emoji grande
            Text(
                text = emotion.emoji,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
            // Label curto
            Text(
                text = emotion.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}
