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

    val nav = rememberNavController()

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
        composable(Routes.Dashboard) {
            DashboardScreen(
                onAddWeight = {},
                onOpenProfile = { nav.navigate(Routes.Profile) },
                onOpenAddMeal = { mealKey ->
                    nav.navigate("add_meal/$mealKey")
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

            // Recuperar datos del scan (si existen)
            val handle = entry.savedStateHandle

            val scannedName by handle.getStateFlow("scan_name", null as String?).collectAsState()
            val scannedKcal by handle.getStateFlow("scan_kcal", null as Int?).collectAsState()
            val scannedPortion by handle.getStateFlow("scan_portion", null as String?).collectAsState()

            // Limpiar para evitar re-trigger
            LaunchedEffect(scannedName, scannedKcal, scannedPortion) {
                if (scannedName != null || scannedKcal != null || scannedPortion != null) {
                    handle["scan_name"] = null
                    handle["scan_kcal"] = null
                    handle["scan_portion"] = null
                }
            }

            AddMealScreen(
                mealKey = mealKey,
                onBack = { nav.popBackStack() },
                onScan = { nav.navigate("scan_food/$mealKey") },
                scannedName = scannedName,
                scannedKcal = scannedKcal,
                scannedPortion = scannedPortion
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
    }
}
