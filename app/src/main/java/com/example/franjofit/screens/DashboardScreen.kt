package com.example.franjofit.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.Orange
import com.example.franjofit.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onAddWeight: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val uiState = viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hola ${uiState.value.username}") },
                navigationIcon = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Perfil de usuario"
                        )
                    }
                }
            )
        }
    )  { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlue)
                .padding(padding)
        ) {
            Column(Modifier.padding(16.dp)) {

                Spacer(Modifier.height(12.dp)) // espacio bajo la app bar

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Calorías", color = White)
                        Spacer(Modifier.height(8.dp))
                        Text("Objetivo base: ${uiState.value.baseGoal}", color = White)
                        Text("Alimentos: ${uiState.value.food}", color = White)
                        Text("Ejercicio: ${uiState.value.exercise}", color = White)
                        Text("Restantes: ${uiState.value.remaining}", color = White)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Pasos: ${uiState.value.steps}", color = White)
                            Text("Meta: ${uiState.value.stepsGoal}", color = White)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Ejercicio: ${uiState.value.exercise} cal", color = White)
                            Text("Tiempo: ${uiState.value.exerciseMinutes} min", color = White)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Peso (últimos 90 días)", color = White)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onAddWeight,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Orange)
                        ) {
                            Text("Agregar peso", color = White)
                        }
                    }
                }
            }
        }
    }
}
