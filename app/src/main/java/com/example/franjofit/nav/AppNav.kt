package com.example.franjofit.nav

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.franjofit.data.GoalsRepository
import com.example.franjofit.screens.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AppNav(
    openSmpFromNotification: Boolean = false
) {

    val nav = rememberNavController()

    // Si viene desde la notificación de SMP, lo mandamos al Dashboard y luego al formulario
    LaunchedEffect(openSmpFromNotification) {
        if (openSmpFromNotification) {
            nav.navigate(Routes.Dashboard) {
                popUpTo(0) { inclusive = false }
            }
            nav.navigate(Routes.FormsSmp)
        }
    }

    NavHost(
        navController = nav,
        startDestination = Routes.Welcome
    ) {

        // =====================================
        // WELCOME
        // =====================================
        composable(Routes.Welcome) {
            WelcomeScreen(
                onSignUp = { nav.navigate(Routes.RegisterUsername) },
                onLogin = { nav.navigate(Routes.Login) }
            )
        }

        // =====================================
        // LOGIN
        // =====================================
        composable(Routes.Login) {
            LoginScreen(
                onForgot = {},
                onGoogle = {},
                onLoggedIn = {
                    nav.navigate(Routes.Dashboard) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            )
        }

        // =====================================
        // REGISTRO 1
        // =====================================
        composable(Routes.RegisterUsername) {
            RegisterUsernameScreen(
                onNext = { username ->
                    val safe = Uri.encode(username.trim())
                    nav.navigate("register_personal/$safe")
                }
            )
        }

        // =====================================
        // REGISTRO 2
        // =====================================
        composable(
            route = Routes.RegisterPersonal,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { entry ->
            val username = entry.arguments?.getString("username") ?: ""
            RegisterPersonalScreen(
                username = username,
                onNext = { nav.navigate(Routes.RegisterGoals) }
            )
        }

        // =====================================
        // REGISTRO 3 (METAS)
        // =====================================
        composable(Routes.RegisterGoals) {
            RegisterGoalsScreen(
                onContinue = { nav.navigate(Routes.RegisterEmail) }
            )
        }

        // =====================================
        // REGISTRO 4 (EMAIL)
        // =====================================
        composable(
            route = Routes.RegisterEmail,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { entry ->
            val username = entry.arguments?.getString("username") ?: ""
            RegisterEmailScreen(
                displayName = username,
                navController = nav
            )
        }

        // =====================================
        // PERFIL
        // =====================================
        composable(Routes.Profile) {
            if (FirebaseAuth.getInstance().currentUser != null) {
                ProfileScreen()
            } else {
                nav.navigate(Routes.Login)
            }
        }

        // =====================================
        // DASHBOARD
        // =====================================
// =====================================
// DASHBOARD
// =====================================
        composable(Routes.Dashboard) { entry ->

            // 👇 LEEMOS EL FLAG DE QUE LAS COMIDAS CAMBIARON
            val handle = entry.savedStateHandle
            val mealsChanged by handle
                .getStateFlow("meals_changed", false)
                .collectAsState()

            DashboardScreen(
                onAddWeight = {},
                onOpenProfile = { nav.navigate(Routes.Profile) },
                onOpenAddMeal = { mealKey ->
                    nav.navigate("add_meal/$mealKey")
                },
                onUpdateBaseGoal = { /* si luego quieres algo extra */ },
                onOpenReminders = {
                    // por ahora vacío
                },
                mealsChanged = mealsChanged,                               // 👈 NUEVO
                onMealsChangeHandled = {
                    handle["meals_changed"] = false   // reseteamos el flag
                },
                onOpenSmpBot = {
                    nav.navigate(Routes.SmpBot)   // 👈 AQUÍ NAVEGAS AL BOT
                }
            )
        }

// =====================================
// ADD MEAL (con HIGHLIGHT desde escaneo)
// =====================================
        composable(
            route = Routes.AddMeal,
            arguments = listOf(navArgument("meal") { type = NavType.StringType })
        ) { entry ->

            val mealKey = entry.arguments?.getString("meal") ?: "desayuno"

            val handle = entry.savedStateHandle

            val scannedName by handle.getStateFlow("scan_name", null as String?).collectAsState()
            val scannedKcal by handle.getStateFlow("scan_kcal", null as Int?).collectAsState()
            val scannedPortion by handle.getStateFlow("scan_portion", null as String?).collectAsState()

            LaunchedEffect(scannedName, scannedKcal, scannedPortion) {
                if (scannedName != null || scannedKcal != null || scannedPortion != null) {
                    handle["scan_name"] = null
                    handle["scan_kcal"] = null
                    handle["scan_portion"] = null
                }
            }

            AddMealScreen(
                mealKey = mealKey,
                onBack = { nav.popBackStack() },               // para el botón de atrás de la toolbar
                onScan = { nav.navigate("scan_food/$mealKey") },
                scannedName = scannedName,
                scannedKcal = scannedKcal,
                scannedPortion = scannedPortion,
                onMealsSaved = {
                    // 👇 AVISAMOS AL DASHBOARD QUE CAMBIARON LAS COMIDAS
                    nav.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("meals_changed", true)

                    // 👈 y regresamos al Dashboard
                    nav.popBackStack()
                }
            )
        }


        // =====================================
        // SCAN FOOD
        // =====================================
        composable(
            route = Routes.ScanFood,
            arguments = listOf(navArgument("meal") { type = NavType.StringType })
        ) { entry ->

            val mealKey = entry.arguments?.getString("meal") ?: "desayuno"

            ScanFoodScreen(
                onCancel = { nav.popBackStack() },
                onUseResult = { name, kcal, portion ->

                    nav.previousBackStackEntry?.savedStateHandle?.apply {
                        this["scan_name"] = name
                        this["scan_kcal"] = kcal
                        this["scan_portion"] = portion
                    }

                    nav.popBackStack()
                }
            )
        }

        // =====================================
        // FORMULARIO SMP (síntomas postprandiales)
        // =====================================
        composable(Routes.FormsSmp) {
            val scope = rememberCoroutineScope()
            var baseScore by remember { mutableStateOf<Int?>(null) }

            LaunchedEffect(Unit) {
                baseScore = GoalsRepository.getTodaySmpCurrent()
            }

            if (baseScore == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                FormsSMPScreen(
                    baseScore = baseScore!!,
                    onSubmit = { newScore ->
                        scope.launch {
                            GoalsRepository.updateTodaySmpCurrent(newScore)
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
        }

// =====================================
// SMP BOT
// =====================================
        composable(Routes.SmpBot) {
            SmpBotScreen(
                onBack = { nav.popBackStack() }
            )
        }




    }
}
