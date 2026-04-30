package com.expressme.watch.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay

/**
 * Tela de confirmação exibida após o toque em um botão de emoção.
 *
 * Mostra:
 * - Fundo com a cor do botão pressionado
 * - Ícone de check (✓) centralizado grande
 * - Texto "Mensagem enviada!" abaixo do ícone
 *
 * Volta automaticamente para a tela principal após 2 segundos.
 * Não possui botão de voltar para simplificar a experiência da criança.
 *
 * @param backgroundColor Cor de fundo correspondente à emoção selecionada
 * @param onTimeout Callback chamado após 2 segundos para retornar à tela principal
 */
@Composable
fun ConfirmationScreen(
    backgroundColor: Color,
    onTimeout: () -> Unit
) {
    // Timer de 2 segundos para retornar à tela principal
    LaunchedEffect(Unit) {
        delay(2000L)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Ícone de check grande
            Text(
                text = "✓",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            // Texto de confirmação
            Text(
                text = "Mensagem\nenviada!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
