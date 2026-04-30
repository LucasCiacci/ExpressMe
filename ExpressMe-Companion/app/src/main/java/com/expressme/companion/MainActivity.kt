package com.expressme.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.expressme.companion.data.AlertaEntity
import com.expressme.companion.ui.theme.ExpressMeCompanionTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpressMeCompanionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HistoricoScreen(viewModel)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Tela principal
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(viewModel: MainViewModel) {
    val alertas by viewModel.alertas.collectAsState()
    val filtroAtivo by viewModel.filtroAtivo.collectAsState()
    val naoLidos by viewModel.naoLidos.collectAsState()
    val resumoHoje by viewModel.resumoHoje.collectAsState()
    val watchConectado by viewModel.watchConectado.collectAsState()

    var mostrarDialogConfig by remember { mutableStateOf(false) }
    var nomePessoa by remember { mutableStateOf(viewModel.nomePessoa) }
    var mostrarDialogLimpar by remember { mutableStateOf(false) }

    if (mostrarDialogConfig) {
        DialogConfigurarNome(
            nomeAtual = nomePessoa,
            onConfirmar = { novoNome ->
                viewModel.salvarNomePessoa(novoNome)
                nomePessoa = novoNome
                mostrarDialogConfig = false
            },
            onDismiss = { mostrarDialogConfig = false }
        )
    }

    if (mostrarDialogLimpar) {
        DialogConfirmarLimpeza(
            onConfirmar = {
                viewModel.limparHistorico()
                mostrarDialogLimpar = false
            },
            onDismiss = { mostrarDialogLimpar = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Histórico de Alertas", fontWeight = FontWeight.Bold)
                        if (naoLidos > 0) {
                            Spacer(Modifier.width(8.dp))
                            BadgeNaoLidos(naoLidos)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Indicador do watch
                    WatchStatusIcon(watchConectado) { viewModel.verificarWatch() }

                    // Marcar todos como lidos
                    if (naoLidos > 0) {
                        IconButton(onClick = { viewModel.marcarTodosComoLidos() }) {
                            Text("✓✓", fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    // Limpar histórico
                    if (alertas.isNotEmpty()) {
                        IconButton(onClick = { mostrarDialogLimpar = true }) {
                            Text("🗑️", fontSize = 18.sp)
                        }
                    }

                    // Configurações
                    TextButton(onClick = { mostrarDialogConfig = true }) {
                        Text("⚙️", fontSize = 20.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Resumo de hoje
            ResumoHojeCard(resumoHoje)

            // Chips de filtro
            FiltroChips(
                filtroAtivo = filtroAtivo,
                onFiltroSelecionado = { viewModel.setFiltro(it) }
            )

            // Lista ou estado vazio
            if (alertas.isEmpty()) {
                EstadoVazio(filtroAtivo)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(alertas, key = { it.id }) { alerta ->
                        AlertaSwipeable(
                            alerta = alerta,
                            onDeletar = { viewModel.deletarAlerta(alerta.id) },
                            onMarcarLido = { viewModel.marcarComoLido(alerta.id) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Badge de não lidos
// ─────────────────────────────────────────────

@Composable
fun BadgeNaoLidos(count: Int) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = MaterialTheme.colorScheme.onError,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────
// Indicador de status do watch
// ─────────────────────────────────────────────

@Composable
fun WatchStatusIcon(conectado: Boolean?, onRefresh: () -> Unit) {
    IconButton(onClick = onRefresh) {
        val (emoji, desc) = when (conectado) {
            true  -> "⌚" to "Watch conectado"
            false -> "📵" to "Watch desconectado"
            null  -> "🔄" to "Verificando..."
        }
        Text(emoji, fontSize = 18.sp)
    }
}

// ─────────────────────────────────────────────
// Resumo de hoje
// ─────────────────────────────────────────────

@Composable
fun ResumoHojeCard(resumo: ResumoHoje) {
    val total = resumo.ajuda + resumo.desconfortavel + resumo.sair + resumo.feliz
    if (total == 0) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Hoje",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResumoItem("🆘", resumo.ajuda, Color(0xFFE91E63))
                ResumoItem("😣", resumo.desconfortavel, Color(0xFFFF9800))
                ResumoItem("🚪", resumo.sair, Color(0xFF008080))
                ResumoItem("😊", resumo.feliz, Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun ResumoItem(emoji: String, count: Int, cor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            count.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = cor
        )
    }
}

// ─────────────────────────────────────────────
// Chips de filtro
// ─────────────────────────────────────────────

@Composable
fun FiltroChips(filtroAtivo: FiltroTipo, onFiltroSelecionado: (FiltroTipo) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(FiltroTipo.entries) { filtro ->
            FilterChip(
                selected = filtro == filtroAtivo,
                onClick = { onFiltroSelecionado(filtro) },
                label = { Text(filtro.label, fontSize = 13.sp) }
            )
        }
    }
}

// ─────────────────────────────────────────────
// Card de alerta com swipe para deletar
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertaSwipeable(
    alerta: AlertaEntity,
    onDeletar: () -> Unit,
    onMarcarLido: () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == EndToStart) {
                onDeletar()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val cor by animateColorAsState(
                if (state.dismissDirection == EndToStart) Color(0xFFE53935)
                else Color.Transparent,
                animationSpec = tween(200),
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cor)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text("🗑️", fontSize = 20.sp, color = Color.White)
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        AlertaCard(alerta, onMarcarLido)
    }
}

@Composable
fun AlertaCard(alerta: AlertaEntity, onMarcarLido: () -> Unit = {}) {
    val backgroundColor = corParaTipo(alerta.tipo)
    val alpha = if (alerta.lido) 0.55f else 1f

    LaunchedEffect(alerta.id) {
        if (!alerta.lido) onMarcarLido()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor.copy(alpha = alpha)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = emojiParaTipo(alerta.tipo),
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Text(
                    text = alerta.mensagem,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (alerta.lido) FontWeight.Normal else FontWeight.SemiBold
                )
            }
            Text(
                text = formatarTimestamp(alerta.timestamp),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
// Estado vazio
// ─────────────────────────────────────────────

@Composable
fun EstadoVazio(filtroAtivo: FiltroTipo) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "📭", fontSize = 48.sp)
            Text(
                text = if (filtroAtivo == FiltroTipo.TODOS)
                    "Nenhum alerta recebido ainda"
                else
                    "Nenhum alerta do tipo \"${filtroAtivo.label}\"",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            if (filtroAtivo == FiltroTipo.TODOS) {
                Text(
                    text = "Os alertas do smartwatch aparecerão aqui",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────

@Composable
fun DialogConfigurarNome(
    nomeAtual: String,
    onConfirmar: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var texto by remember { mutableStateOf(nomeAtual) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Configurar nome", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Nome de quem usa o smartwatch:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    singleLine = true,
                    placeholder = { Text("Ex: Lucas") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (texto.isNotBlank()) onConfirmar(texto) },
                        enabled = texto.isNotBlank()
                    ) { Text("Salvar") }
                }
            }
        }
    }
}

@Composable
fun DialogConfirmarLimpeza(onConfirmar: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Limpar histórico") },
        text = { Text("Todos os alertas serão removidos permanentemente. Tem certeza?") },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Limpar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ─────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────

fun corParaTipo(tipo: String): Color = when (tipo) {
    "desconfortavel" -> Color(0xFFFF9800)
    "sair"           -> Color(0xFF008080)
    "ajuda"          -> Color(0xFFE91E63)
    "feliz"          -> Color(0xFF4CAF50)
    else             -> Color.Gray
}

fun emojiParaTipo(tipo: String): String = when (tipo) {
    "desconfortavel" -> "😣"
    "sair"           -> "🚪"
    "ajuda"          -> "🆘"
    "feliz"          -> "😊"
    else             -> "❓"
}

fun formatarTimestamp(timestamp: Long): String {
    val agora = Calendar.getInstance()
    val alerta = Calendar.getInstance().apply { timeInMillis = timestamp }
    val ehHoje = agora.get(Calendar.YEAR) == alerta.get(Calendar.YEAR) &&
            agora.get(Calendar.DAY_OF_YEAR) == alerta.get(Calendar.DAY_OF_YEAR)
    return if (ehHoje)
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    else
        SimpleDateFormat("dd/MM  HH:mm", Locale.getDefault()).format(Date(timestamp))
}
