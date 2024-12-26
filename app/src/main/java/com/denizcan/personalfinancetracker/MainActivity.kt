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
import com.denizcan.personalfinancetracker.screens.*
import com.denizcan.personalfinancetracker.ui.theme.PersonalFinanceTrackerTheme
import com.google.firebase.FirebaseApp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.denizcan.personalfinancetracker.network.CurrencyViewModel
import com.denizcan.personalfinancetracker.network.ExchangeRatesViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Firebase'i başlat
        setContent {
            PersonalFinanceTrackerTheme {
                val navController: NavHostController = rememberNavController()

                // viewModels for ExchangeRatesScreen
                val currencyViewModel: CurrencyViewModel = viewModel()
                val exchangeRatesViewModel: ExchangeRatesViewModel = viewModel()

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
                            composable("addDailyExpense") {
                                AddDailyExpenseScreen(navController)
                            }
                            composable(
                                "editDailyExpense/{expenseId}/{expenseName}/{expenseAmount}",
                                arguments = listOf(
                                    navArgument("expenseId") { type = NavType.StringType },
                                    navArgument("expenseName") { type = NavType.StringType },
                                    navArgument("expenseAmount") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
                                val expenseName = backStackEntry.arguments?.getString("expenseName") ?: ""
                                val expenseAmount = backStackEntry.arguments?.getString("expenseAmount")?.toDoubleOrNull() ?: 0.0
                                EditDailyExpenseScreen(navController, expenseId, expenseName, expenseAmount)
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
                            // Edit Income Screen için rota
                            composable("editIncome/{incomeId}") { backStackEntry ->
                                val incomeId = backStackEntry.arguments?.getString("incomeId") ?: ""
                                EditIncomeScreen(navController, incomeId)
                            }
                            // Edit Expense Screen için rota
                            composable("editExpense/{expenseId}") { backStackEntry ->
                                val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
                                EditExpenseScreen(navController, expenseId)
                            }
                            // Limit Screen için rota
                            composable("limit") {
                                LimitScreen(navController)
                            }
                            // Exchange Rates Screen için rota
                            composable("exchangeRates") {
                                ExchangeRatesScreen(
                                    navController = navController,
                                    currencyViewModel = currencyViewModel,
                                    exchangeRatesViewModel = exchangeRatesViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
