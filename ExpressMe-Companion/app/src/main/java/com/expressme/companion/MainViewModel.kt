package com.expressme.companion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.expressme.companion.data.AppDatabase
import kotlinx.coroutines.flow.Flow
import com.expressme.companion.data.AlertaEntity

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val alertas: Flow<List<AlertaEntity>> = database.alertaDao().getAllAlertas()
}
