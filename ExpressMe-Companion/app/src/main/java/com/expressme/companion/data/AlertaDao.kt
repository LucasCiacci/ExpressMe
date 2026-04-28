package com.expressme.companion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertaDao {

    @Insert
    suspend fun insert(alerta: AlertaEntity)

    @Query("SELECT * FROM alertas ORDER BY timestamp DESC LIMIT 500")
    fun getAllAlertas(): Flow<List<AlertaEntity>>

    @Query("SELECT * FROM alertas WHERE tipo = :tipo ORDER BY timestamp DESC LIMIT 500")
    fun getAlertasPorTipo(tipo: String): Flow<List<AlertaEntity>>

    @Query("SELECT COUNT(*) FROM alertas WHERE lido = 0")
    fun countNaoLidos(): Flow<Int>

    @Query("SELECT COUNT(*) FROM alertas WHERE tipo = :tipo AND timestamp >= :desde")
    fun countPorTipoHoje(tipo: String, desde: Long): Flow<Int>

    @Query("UPDATE alertas SET lido = 1 WHERE id = :id")
    suspend fun marcarComoLido(id: Int)

    @Query("UPDATE alertas SET lido = 1")
    suspend fun marcarTodosComoLidos()

    @Query("DELETE FROM alertas WHERE id = :id")
    suspend fun deletarPorId(id: Int)

    @Query("DELETE FROM alertas")
    suspend fun deletarTodos()

    @Query("DELETE FROM alertas WHERE timestamp < :antes")
    suspend fun deletarAntigos(antes: Long)
}
