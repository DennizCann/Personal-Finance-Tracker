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
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Firebase'i başlat
        setContent {
            PersonalFinanceTrackerTheme {
                val navController: NavHostController = rememberNavController()

                // Firebase kullanıcı kimliği
                val currentUser = FirebaseAuth.getInstance().currentUser
                val userId = currentUser?.uid ?: ""

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
                                AddDailyExpenseScreen(
                                    navController = navController,
                                    userId = userId
                                )
                            }
                            composable("addDailyIncome") {
                                val currentUser = FirebaseAuth.getInstance().currentUser
                                val userId = currentUser?.uid ?: "" // Kullanıcı kimliği alınır
                                AddDailyIncomeScreen(navController = navController, userId = userId)
                            }

                            composable(
                                "editDailyExpense/{expenseId}/{expenseName}/{expenseAmount}",
                                arguments = listOf(
                                    navArgument("expenseId") { type = NavType.StringType },
                                    navArgument("expenseName") { type = NavType.StringType },
                                    navArgument("expenseAmount") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val expenseId =
                                    backStackEntry.arguments?.getString("expenseId") ?: ""
                                val expenseName =
                                    backStackEntry.arguments?.getString("expenseName") ?: ""
                                val expenseAmount =
                                    backStackEntry.arguments?.getString("expenseAmount")
                                        ?.toDoubleOrNull() ?: 0.0
                                EditDailyExpenseScreen(
                                    navController,
                                    expenseId,
                                    expenseName,
                                    expenseAmount
                                )
                            }

                            composable(
                                "editDailyIncome/{incomeId}/{incomeName}/{incomeAmount}",
                                arguments = listOf(
                                    navArgument("incomeId") { type = NavType.StringType },
                                    navArgument("incomeName") { type = NavType.StringType },
                                    navArgument("incomeAmount") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val incomeId = backStackEntry.arguments?.getString("incomeId") ?: ""
                                val incomeName =
                                    backStackEntry.arguments?.getString("incomeName") ?: ""
                                val incomeAmount =
                                    backStackEntry.arguments?.getString("incomeAmount")
                                        ?.toDoubleOrNull() ?: 0.0
                                EditDailyIncomeScreen(
                                    navController,
                                    incomeId,
                                    incomeName,
                                    incomeAmount
                                )
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
                            composable("editIncome/{incomeId}") { backStackEntry ->
                                val incomeId = backStackEntry.arguments?.getString("incomeId") ?: ""
                                EditIncomeScreen(navController, incomeId)
                            }
                            composable("editExpense/{expenseId}") { backStackEntry ->
                                val expenseId =
                                    backStackEntry.arguments?.getString("expenseId") ?: ""
                                EditExpenseScreen(navController, expenseId)
                            }
                            composable("limit") {
                                LimitScreen(navController)
                            }
                            composable("exchangeRates") {
                                ExchangeRatesScreen(
                                    navController = navController,
                                    currencyViewModel = currencyViewModel,
                                    exchangeRatesViewModel = exchangeRatesViewModel
                                )
                            }
                            composable("convert") {
                                ConvertScreen(navController = navController)
                            }
                            composable("goals") {
                                GoalsScreen(navController = navController)
                            }
                            composable("addGoal") {
                                AddGoalScreen(navController = navController)
                            }
                            composable(
                                "editGoal/{goalId}",
                                arguments = listOf(navArgument("goalId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val goalId = backStackEntry.arguments?.getString("goalId") ?: ""
                                EditGoalScreen(
                                    navController = navController,
                                    goalId = goalId,
                                    currencyViewModel = currencyViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}