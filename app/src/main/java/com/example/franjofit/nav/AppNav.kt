package com.example.franjofit.nav

import android.net.Uri
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.franjofit.screens.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNav() {
    val controladorNavegacion = rememberNavController()

    NavHost(
        navController = controladorNavegacion,
        startDestination = Routes.Welcome
    ) {

        // Bienvenida
        composable(Routes.Welcome) {
            WelcomeScreen(
                onSignUp = { controladorNavegacion.navigate(Routes.RegisterUsername) },
                onLogin  = { controladorNavegacion.navigate(Routes.Login) }
            )
        }

        // Login
        composable(Routes.Login) {
            LoginScreen(
                onForgot   = { /* TODO: recuperar contraseña */ },
                onGoogle   = { /* TODO: login con Google */ },
                onLoggedIn = {
                    controladorNavegacion.navigate(Routes.Dashboard) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            )
        }

        // Registro - usuario
        composable(Routes.RegisterUsername) {
            RegisterUsernameScreen(
                onNext = { nombreUsuario ->
                    val seguro = Uri.encode(nombreUsuario.trim())
                    controladorNavegacion.navigate("register_personal/$seguro")
                }
            )
        }

        // Registro - datos personales
        composable(
            route = Routes.RegisterPersonal,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { entrada ->
            val nombreUsuario = entrada.arguments?.getString("username").orEmpty()
            RegisterPersonalScreen(
                username = nombreUsuario,
                onNext   = { controladorNavegacion.navigate(Routes.RegisterGoals) }
            )
        }

        // Registro - metas
        composable(Routes.RegisterGoals) {
            RegisterGoalsScreen(
                onContinue = { controladorNavegacion.navigate(Routes.RegisterEmail) }
            )
        }

        // Registro - email
        composable(
            route = Routes.RegisterEmail,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { entrada ->
            val nombreUsuario = entrada.arguments?.getString("username").orEmpty()
            RegisterEmailScreen(
                displayName  = nombreUsuario,
                navController = controladorNavegacion
            )
        }

        // Perfil
        composable(Routes.Profile) {
            if (FirebaseAuth.getInstance().currentUser != null) {
                ProfileScreen()
            } else {
                controladorNavegacion.navigate(Routes.Login)
            }
        }

        // Dashboard
        composable(Routes.Dashboard) {
            DashboardScreen(
                onAddWeight    = { /* TODO: agregar peso */ },
                onOpenProfile  = { controladorNavegacion.navigate(Routes.Profile) },
                onOpenAddMeal  = { claveComida ->
                    controladorNavegacion.navigate("add_meal/$claveComida")
                }
            )
        }

        // Agregar comida (buscador + catálogo CSV)
        composable(
            route = Routes.AddMeal,
            arguments = listOf(navArgument("meal") { type = NavType.StringType })
        ) { entrada ->
            val claveComida = entrada.arguments?.getString("meal") ?: "desayuno"

            // Recibir datos del resultado de escaneo (si existieran)
            val estadoGuardado = entrada.savedStateHandle
            val flujoNombreEscaneo  = estadoGuardado.getStateFlow("scan_name",    null as String?)
            val flujoKcalEscaneo    = estadoGuardado.getStateFlow("scan_kcal",    null as Int?)
            val flujoPorcionEscaneo = estadoGuardado.getStateFlow("scan_portion", null as String?)

            val nombreEscaneo  by flujoNombreEscaneo.collectAsState()
            val kcalEscaneo    by flujoKcalEscaneo.collectAsState()
            val porcionEscaneo by flujoPorcionEscaneo.collectAsState()

            // Si llega un resultado de escaneo, podrías auto-seleccionar o mostrar destacado.
            // Por ahora solo limpiamos para no re-disparar.
            LaunchedEffect(nombreEscaneo, kcalEscaneo, porcionEscaneo) {
                if (nombreEscaneo != null || kcalEscaneo != null || porcionEscaneo != null) {
                    estadoGuardado.set("scan_name", null)
                    estadoGuardado.set("scan_kcal", null)
                    estadoGuardado.set("scan_portion", null)
                }
            }

            AddMealScreen(
                mealKey = claveComida,
                onBack  = { controladorNavegacion.popBackStack() },
                onScan  = { controladorNavegacion.navigate("scan_food/$claveComida") }
            )
        }

        // Escanear alimento (visión)
        composable(
            route = Routes.ScanFood,
            arguments = listOf(navArgument("meal") { type = NavType.StringType })
        ) {
            ScanFoodScreen(
                onCancel = { controladorNavegacion.popBackStack() },
                onUseResult = { nombre, kcal, porcion ->
                    controladorNavegacion.previousBackStackEntry
                        ?.savedStateHandle
                        ?.apply {
                            set("scan_name", nombre)
                            set("scan_kcal", kcal)
                            set("scan_portion", porcion)
                        }
                    controladorNavegacion.popBackStack()
                }
            )
        }
    }
}
