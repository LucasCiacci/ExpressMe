package com.expressme.companion.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alertas",
    indices = [Index(value = ["tipo"]), Index(value = ["lido"])]
)
data class AlertaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mensagem: String,
    val timestamp: Long,
    val tipo: String,
    val lido: Boolean = false
)
