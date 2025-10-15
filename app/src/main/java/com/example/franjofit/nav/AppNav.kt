package com.example.franjofit.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.franjofit.screens.*
import com.example.franjofit.data.UserProfile
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNav() {
    val navController = rememberNavController()


    NavHost(navController = navController, startDestination = Routes.Welcome) {


        composable(Routes.Welcome) {
            WelcomeScreen(
                onSignUp = { navController.navigate(Routes.RegisterUsername) },
                onLogin = { navController.navigate(Routes.Login) }
            )
        }


        composable(Routes.Login) {
            LoginScreen(
                onForgot = { /* TODO: handle forgot password */ },
                onGoogle = { /* TODO: handle Google login */ },
                onLoggedIn = {
                    // Navigate to the DashboardScreen after successful login
                    navController.navigate(Routes.Dashboard) {
                        popUpTo(Routes.Login) { inclusive = true } // Ensure the Login screen is removed from the stack
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
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username").orEmpty()
            RegisterPersonalScreen(
                username = username,
                onNext = { navController.navigate(Routes.RegisterGoals) }
            )
        }

        composable(Routes.RegisterGoals) {
            RegisterGoalsScreen(
                onContinue = {
                    navController.navigate(Routes.RegisterEmail)
                }
            )
        }


        composable(
            route = Routes.RegisterEmail,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username").orEmpty()
            RegisterEmailScreen(
                displayName = username,
                navController = navController
            )
        }


        composable(Routes.Profile) {

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {

                ProfileScreen()
            } else {

                navController.navigate(Routes.Login)
            }
        }
        composable(Routes.Dashboard) {
            DashboardScreen(
                onAddWeight = { /* acción de agregar peso */ },
                onOpenProfile = { navController.navigate(Routes.Profile) } // 👈 navegar al perfil
            )
        }


    }
}
