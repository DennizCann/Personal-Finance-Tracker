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
import com.denizcan.personalfinancetracker.screens.EditExpenseScreen
import com.denizcan.personalfinancetracker.screens.EditIncomeScreen
import com.denizcan.personalfinancetracker.screens.EditProfileScreen
import com.denizcan.personalfinancetracker.screens.LimitScreen
import com.denizcan.personalfinancetracker.screens.LoginScreen
import com.denizcan.personalfinancetracker.screens.RegisterScreen
import com.denizcan.personalfinancetracker.screens.ViewExpenseScreen
import com.denizcan.personalfinancetracker.screens.ViewIncomeScreen
import com.denizcan.personalfinancetracker.screens.ViewScreen
import com.denizcan.personalfinancetracker.screens.ExchangeRatesScreen // Import ExchangeRatesScreen
import com.denizcan.personalfinancetracker.ui.theme.PersonalFinanceTrackerTheme
import com.google.firebase.FirebaseApp
import androidx.lifecycle.viewmodel.compose.viewModel
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
