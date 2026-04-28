package com.expressme.companion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alertas")
data class AlertaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mensagem: String,
    val timestamp: Long,
    val tipo: String // 'desconfortavel', 'sair', 'ajuda', 'feliz'
)
