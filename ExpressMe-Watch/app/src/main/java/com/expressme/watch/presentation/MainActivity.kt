package com.expressme.watch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.expressme.watch.presentation.theme.ExpressMeWatchTheme

/**
 * Activity principal do ExpressMe Watch.
 *
 * Gerencia a navegação entre:
 * - MainScreen (tela dos 4 botões de emoção)
 * - ConfirmationScreen (tela de confirmação pós-toque)
 *
 * A navegação é feita via state hoisting simples,
 * sem splash screen e sem framework de navegação complexo.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpressMeWatchApp()
        }
    }
}

/**
 * Composable raiz que controla a navegação entre as telas.
 * Usa estado simples (selectedEmotion) para determinar qual tela exibir:
 * - null → MainScreen
 * - Emotion → ConfirmationScreen com a cor da emoção selecionada
 */
@Composable
fun ExpressMeWatchApp() {
    ExpressMeWatchTheme {
        var selectedEmotion by remember { mutableStateOf<Emotion?>(null) }

        if (selectedEmotion == null) {
            // Tela principal com grade 2x2 de botões
            MainScreen(
                onEmotionSelected = { emotion ->
                    selectedEmotion = emotion
                }
            )
        } else {
            // Tela de confirmação com cor do botão pressionado
            ConfirmationScreen(
                backgroundColor = selectedEmotion!!.color,
                onTimeout = {
                    selectedEmotion = null
                }
            )
        }
    }
}