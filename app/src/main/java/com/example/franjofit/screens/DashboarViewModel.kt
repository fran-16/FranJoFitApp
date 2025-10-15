package com.example.franjofit.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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

class DashboardViewModel : ViewModel() {

    private val _ui = MutableStateFlow(DashboardUi())
    val ui: StateFlow<DashboardUi> = _ui

    fun setMockActivity() {
        _ui.update {
            it.copy(
                steps = 253,
                exercise = 0,
                exerciseMinutes = 0
            )
        }
    }
}
