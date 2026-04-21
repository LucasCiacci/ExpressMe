package com.expressme.companion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertaDao {
    @Insert
    suspend fun insert(alerta: AlertaEntity)

    @Query("SELECT * FROM alertas ORDER BY timestamp DESC")
    fun getAllAlertas(): Flow<List<AlertaEntity>>
}
