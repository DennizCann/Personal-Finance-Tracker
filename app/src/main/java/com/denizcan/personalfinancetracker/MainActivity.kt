package com.denizcan.personalfinancetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.denizcan.personalfinancetracker.screens.AddExpenseScreen
import com.denizcan.personalfinancetracker.screens.AddIncomeScreen
import com.denizcan.personalfinancetracker.screens.AddScreen
import com.denizcan.personalfinancetracker.screens.DashboardScreen
import com.denizcan.personalfinancetracker.screens.EditProfileScreen
import com.denizcan.personalfinancetracker.screens.LoginScreen
import com.denizcan.personalfinancetracker.screens.RegisterScreen
import com.denizcan.personalfinancetracker.screens.ViewExpenseScreen
import com.denizcan.personalfinancetracker.screens.ViewIncomeScreen
import com.denizcan.personalfinancetracker.screens.ViewScreen
import com.google.firebase.FirebaseApp


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Firebase'i başlat
        setContent {
            val navController: NavHostController = rememberNavController()
            MaterialTheme {
                Surface {
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(navController)
                        }
                        composable("register") {
                            RegisterScreen(navController)
                        }
                        composable("dashboard") {
                            DashboardScreen(navController)
                        }
                        composable("add") {
                            AddScreen(navController)
                        }
                        composable("addIncome") {
                            AddIncomeScreen(navController)
                        }
                        composable("addExpense") {
                            AddExpenseScreen(navController)
                        }
                        composable("editProfile") {
                            EditProfileScreen(navController)
                        }
                        composable("view") {
                            ViewScreen(navController)
                        }
                        composable("viewIncome") {
                            ViewIncomeScreen(navController)
                        }
                        composable("viewExpense") {
                            ViewExpenseScreen(navController)
                        }
                    }
                }
            }
        }
    }
}