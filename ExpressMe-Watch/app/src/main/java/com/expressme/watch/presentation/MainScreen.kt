package com.expressme.watch.presentation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
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
    val icon: ImageVector,  // Ícone Material moderno
    val message: String,    // mensagem enviada ao cuidador
    val color: Color,       // cor do ícone e accent
    val colorHex: String    // hex da cor para enviar via FCM
)

// Cores vivas — estimulam atenção e reconhecimento em crianças com TEA
val colorFeliz = Color(0xFF2ECC71)          // Verde vibrante
val colorAjuda = Color(0xFFE74C3C)          // Vermelho vivo
val colorDesconfortavel = Color(0xFFF39C12)  // Laranja/âmbar quente
val colorSair = Color(0xFF3498DB)            // Azul vivo

// Lista das 4 emoções do app
val emotions = listOf(
    Emotion("feliz", "Feliz", Icons.Rounded.Mood, "está feliz", colorFeliz, "#2ECC71"),
    Emotion("ajuda", "Ajuda", Icons.Rounded.NotificationsActive, "precisa de ajuda", colorAjuda, "#E74C3C"),
    Emotion("desconfortavel", "Desconforto", Icons.Rounded.SentimentDissatisfied, "está desconfortável", colorDesconfortavel, "#F39C12"),
    Emotion("sair", "Sair", Icons.Rounded.Logout, "quer sair", colorSair, "#3498DB")
)

// Espessura da linha divisória entre quadrantes (em dp)
private val DIVIDER_THICKNESS = 1.dp

/**
 * Posição do quadrante na grade 2x2.
 * Usado para aplicar padding direcional que empurra o conteúdo
 * em direção ao centro da tela circular.
 */
enum class QuadrantPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

/**
 * Tela principal do ExpressMe Watch.
 * Grade 2x2 que preenche a tela inteira do relógio.
 * A tela circular do Wear OS faz o recorte natural nas bordas.
 * Separação entre quadrantes: linha fina de 1dp (preto do fundo).
 *
 * Cada quadrante tem padding extra nas bordas externas para
 * compensar o recorte circular e centralizar visualmente o conteúdo.
 */
@Composable
fun MainScreen(onEmotionSelected: (Emotion) -> Unit) {
    val context = LocalContext.current

    // Fundo preto — a fina linha entre os quadrantes é este preto aparecendo
    // O padding cria uma circunferência preta fina nas bordas da tela circular
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Linha superior: FELIZ e AJUDA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                EmotionQuadrant(
                    emotion = emotions[0],
                    context = context,
                    position = QuadrantPosition.TOP_LEFT,
                    onEmotionSelected = onEmotionSelected,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = DIVIDER_THICKNESS, bottom = DIVIDER_THICKNESS)
                )
                EmotionQuadrant(
                    emotion = emotions[1],
                    context = context,
                    position = QuadrantPosition.TOP_RIGHT,
                    onEmotionSelected = onEmotionSelected,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = DIVIDER_THICKNESS, bottom = DIVIDER_THICKNESS)
                )
            }
            // Linha inferior: DESCONFORTO e SAIR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                EmotionQuadrant(
                    emotion = emotions[2],
                    context = context,
                    position = QuadrantPosition.BOTTOM_LEFT,
                    onEmotionSelected = onEmotionSelected,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = DIVIDER_THICKNESS, top = DIVIDER_THICKNESS)
                )
                EmotionQuadrant(
                    emotion = emotions[3],
                    context = context,
                    position = QuadrantPosition.BOTTOM_RIGHT,
                    onEmotionSelected = onEmotionSelected,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = DIVIDER_THICKNESS, top = DIVIDER_THICKNESS)
                )
            }
        }
    }
}

// Padding extra nas bordas externas para compensar o recorte circular.
// Empurra o conteúdo em direção ao centro da tela.
private val OUTER_BIAS = 14.dp

/**
 * Quadrante individual de emoção — preenche todo o espaço disponível.
 * Fundo escuro com tint sutil da cor da emoção.
 * Ícone + label centralizados com compensação para tela circular.
 */
@Composable
fun EmotionQuadrant(
    emotion: Emotion,
    context: Context,
    position: QuadrantPosition,
    onEmotionSelected: (Emotion) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Fundo vivo com a cor cheia da emoção
    val backgroundColor = emotion.color

    // Padding direcional: mais espaço na borda externa, empurrando
    // o conteúdo para o centro da tela circular
    val contentPadding = when (position) {
        QuadrantPosition.TOP_LEFT -> PaddingValues(start = OUTER_BIAS, top = OUTER_BIAS)
        QuadrantPosition.TOP_RIGHT -> PaddingValues(end = OUTER_BIAS, top = OUTER_BIAS)
        QuadrantPosition.BOTTOM_LEFT -> PaddingValues(start = OUTER_BIAS, bottom = OUTER_BIAS)
        QuadrantPosition.BOTTOM_RIGHT -> PaddingValues(end = OUTER_BIAS, bottom = OUTER_BIAS)
    }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                // 1. Vibração imediata (feedback <100ms)
                HapticHelper.vibrate(context)

                // 2. Reproduzir áudio correspondente
                AudioHelper.play(context, emotion.name)

                // 3. Enviar notificação FCM (fire-and-forget) pela internet
                WatchMessagingService.sendEmotionMessage(
                    context = context,
                    emocao = emotion.name,
                    mensagem = emotion.message,
                    corHex = emotion.colorHex
                )

                // 4. Navegar para tela de confirmação
                onEmotionSelected(emotion)
            }
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícone Material Rounded
            Icon(
                imageVector = emotion.icon,
                contentDescription = emotion.label,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Label — tipografia clean, nunca quebra linha
            Text(
                text = emotion.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.3.sp
            )
        }
    }
}
