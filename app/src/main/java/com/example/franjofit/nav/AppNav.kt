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
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun AppNav() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.Welcome) {


        composable(Routes.Welcome) {
            WelcomeScreen(
                onSignUp = { navController.navigate(Routes.RegisterUsername) },
                onLogin  = { navController.navigate(Routes.Login) }
            )
        }


        composable(Routes.Login) {
            LoginScreen(
                onForgot   = { /* TODO */ },
                onGoogle   = { /* TODO */ },
                onLoggedIn = {
                    navController.navigate(Routes.Dashboard) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            )
        }


        composable(Routes.RegisterUsername) {
            RegisterUsernameScreen(
                onNext = { username ->
                    val safeUsername = Uri.encode(username.trim())
                    navController.navigate("register_personal/$safeUsername")
                }
            )
        }


        composable(
            route = Routes.RegisterPersonal,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { entry ->
            val username = entry.arguments?.getString("username").orEmpty()
            RegisterPersonalScreen(
                username = username,
                onNext   = { navController.navigate(Routes.RegisterGoals) }
            )
        }


        composable(Routes.RegisterGoals) {
            RegisterGoalsScreen(
                onContinue = { navController.navigate(Routes.RegisterEmail) }
            )
        }

        composable(
            route = Routes.RegisterEmail,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { entry ->
            val username = entry.arguments?.getString("username").orEmpty()
            RegisterEmailScreen(
                displayName  = username,
                navController = navController
            )
        }


        composable(Routes.Profile) {
            if (FirebaseAuth.getInstance().currentUser != null) {
                ProfileScreen()
            } else {
                navController.navigate(Routes.Login)
            }
        }


        composable(Routes.Dashboard) {
            DashboardScreen(
                onAddWeight    = { /* TODO */ },
                onOpenProfile  = { navController.navigate(Routes.Profile) },
                onOpenAddMeal  = { mealKey -> navController.navigate("add_meal/$mealKey") }
            )
        }


        composable(
            route = Routes.AddMeal,
            arguments = listOf(navArgument("meal") { type = NavType.StringType })
        ) { entry ->
            val mealArg = entry.arguments?.getString("meal") ?: "desayuno"


            val savedStateHandle = entry.savedStateHandle
            val scanNameFlow    = savedStateHandle.getStateFlow("scan_name",    null as String?)
            val scanKcalFlow    = savedStateHandle.getStateFlow("scan_kcal",    null as Int?)
            val scanPortionFlow = savedStateHandle.getStateFlow("scan_portion", null as String?)

            val scanName    by scanNameFlow.collectAsState()
            val scanKcal    by scanKcalFlow.collectAsState()
            val scanPortion by scanPortionFlow.collectAsState()


            var pending by remember { mutableStateOf<FoodSuggestion?>(null) }
            LaunchedEffect(scanName, scanKcal, scanPortion) {
                val n = scanName; val k = scanKcal; val p = scanPortion
                if (n != null && k != null && p != null) {
                    pending = FoodSuggestion(n, k, p)
                    // limpiar para no re-disparar
                    savedStateHandle.set("scan_name", null)
                    savedStateHandle.set("scan_kcal", null)
                    savedStateHandle.set("scan_portion", null)
                }
            }


            AddMealScreen(
                mealKey = mealArg,
                onBack  = { navController.popBackStack() },
                onScan  = { navController.navigate("scan_food/$mealArg") },
               
            )


            pending?.let { suggestion ->
                LaunchedEffect(suggestion) {
                    // TODO: guardar 'suggestion' si lo deseas
                    navController.popBackStack()
                }
            }
        }

        composable(
            route = Routes.ScanFood,
            arguments = listOf(navArgument("meal") { type = NavType.StringType })
        ) {
            ScanFoodScreen(
                onCancel = { navController.popBackStack() },
                onUseResult = { name, kcal, portion ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.apply {
                            set("scan_name", name)
                            set("scan_kcal", kcal)
                            set("scan_portion", portion)
                        }
                    navController.popBackStack()
                }
            )
        }
    }
}
