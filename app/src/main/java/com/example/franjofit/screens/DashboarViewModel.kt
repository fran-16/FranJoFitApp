package com.example.franjofit.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.franjofit.data.StepCounterManager
import com.example.franjofit.data.StepsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUi(
    val username: String = "Fran",
    val baseGoal: Int = 2200,
    val food: Int = 0,
    val exercise: Int = 0,
    val remaining: Int = baseGoal - food + exercise,
    val steps: Int = 0,
    val stepsGoal: Int = 10000,
    val exerciseMinutes: Int = 0
)

@OptIn(FlowPreview::class)
class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(DashboardUi())
    val ui: StateFlow<DashboardUi> = _ui

    // 👉 pon emulatorMode=true solo para probar
    private val stepCounter = StepCounterManager(app.applicationContext, emulatorMode = false)
    private val repo = StepsRepository()

    init {
        // fuerza una escritura la 1ra vez para confirmar BD (bórralo luego)
        viewModelScope.launch {
            try { repo.upsertToday(1234, _ui.value.stepsGoal); android.util.Log.d("StepsRepo","write test OK") }
            catch (e: Exception) { android.util.Log.e("StepsRepo","write test error", e) }
        }

        viewModelScope.launch {
            stepCounter.stepsTodayFlow()
                .debounce(1500)
                .collect { steps ->
                    _ui.update { it.copy(steps = steps) }
                    try {
                        repo.upsertToday(steps, _ui.value.stepsGoal)
                        android.util.Log.d("StepsRepo","Subido $steps")
                    } catch (e: Exception) {
                        android.util.Log.e("StepsRepo","Error subir", e)
                    }
                }
        }
    }
}
