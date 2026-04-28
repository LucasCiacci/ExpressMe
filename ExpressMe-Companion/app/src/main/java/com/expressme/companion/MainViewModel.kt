package com.expressme.companion

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expressme.companion.data.AlertaEntity
import com.expressme.companion.data.AppDatabase
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FiltroTipo(val label: String, val valor: String?) {
    TODOS("Todos", null),
    AJUDA("Ajuda", "ajuda"),
    DESCONFORTAVEL("Desconforto", "desconfortavel"),
    SAIR("Sair", "sair"),
    FELIZ("Feliz", "feliz")
}

data class ResumoHoje(
    val ajuda: Int = 0,
    val desconfortavel: Int = 0,
    val sair: Int = 0,
    val feliz: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val dao = database.alertaDao()
    private val prefs = application.getSharedPreferences("expressme_prefs", Context.MODE_PRIVATE)

    // --- Filtro ativo ---
    private val _filtroAtivo = MutableStateFlow(FiltroTipo.TODOS)
    val filtroAtivo: StateFlow<FiltroTipo> = _filtroAtivo.asStateFlow()

    // --- Lista de alertas (reage ao filtro) ---
    val alertas: StateFlow<List<AlertaEntity>> = _filtroAtivo
        .flatMapLatest { filtro ->
            if (filtro.valor == null) {
                dao.getAllAlertas()
            } else {
                dao.getAlertasPorTipo(filtro.valor)
            }
        }
        .catch { e ->
            Log.e("MainViewModel", "Erro ao carregar alertas", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // --- Contador de não lidos ---
    val naoLidos: StateFlow<Int> = dao.countNaoLidos()
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // --- Resumo de hoje ---
    val resumoHoje: StateFlow<ResumoHoje> = combine(
        dao.countPorTipoHoje("ajuda", inicioDoDia()),
        dao.countPorTipoHoje("desconfortavel", inicioDoDia()),
        dao.countPorTipoHoje("sair", inicioDoDia()),
        dao.countPorTipoHoje("feliz", inicioDoDia())
    ) { ajuda, desconfort, sair, feliz ->
        ResumoHoje(ajuda, desconfort, sair, feliz)
    }
        .catch { emit(ResumoHoje()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumoHoje())

    // --- Status do watch ---
    private val _watchConectado = MutableStateFlow<Boolean?>(null) // null = verificando
    val watchConectado: StateFlow<Boolean?> = _watchConectado.asStateFlow()

    // --- Nome da pessoa ---
    val nomePessoa: String
        get() = prefs.getString("nome_pessoa", "Alguém") ?: "Alguém"

    init {
        limparAlertosAntigos()
        verificarWatch()
    }

    fun setFiltro(filtro: FiltroTipo) {
        _filtroAtivo.value = filtro
    }

    fun salvarNomePessoa(nome: String) {
        prefs.edit().putString("nome_pessoa", nome.trim()).apply()
    }

    fun marcarComoLido(id: Int) {
        viewModelScope.launch {
            dao.marcarComoLido(id)
        }
    }

    fun marcarTodosComoLidos() {
        viewModelScope.launch {
            dao.marcarTodosComoLidos()
        }
    }

    fun deletarAlerta(id: Int) {
        viewModelScope.launch {
            dao.deletarPorId(id)
        }
    }

    fun limparHistorico() {
        viewModelScope.launch {
            dao.deletarTodos()
        }
    }

    fun verificarWatch() {
        _watchConectado.value = null
        Wearable.getCapabilityClient(getApplication())
            .getAllCapabilities(CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { capabilities ->
                val temWatch = capabilities.values.any { it.nodes.isNotEmpty() }
                _watchConectado.value = temWatch
            }
            .addOnFailureListener {
                _watchConectado.value = false
            }
    }

    private fun limparAlertosAntigos() {
        viewModelScope.launch {
            val trintaDiasAtras = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            dao.deletarAntigos(trintaDiasAtras)
        }
    }

    private fun inicioDoDia(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
