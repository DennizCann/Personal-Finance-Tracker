package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denizcan.personalfinancetracker.network.CurrencyViewModel
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AddDailyExpenseScreen(
    navController: NavController,
    currencyViewModel: CurrencyViewModel = viewModel()
) {
    val expenseName = remember { mutableStateOf("") }
    val expenseAmount = remember { mutableStateOf("") }
    val dailyExpenses = remember { mutableStateListOf<Map<String, Any>>() }
    val totalDailyExpense = remember { mutableStateOf(0.0) }
    val baseCurrency by currencyViewModel.baseCurrency.observeAsState("TRY")
    val isPreview = LocalInspectionMode.current

    if (!isPreview) {
        val db = FirebaseFirestore.getInstance()
        val currentDate = "2024-12-26" // Örnek bir tarih

        LaunchedEffect(Unit) {
            // Günlük harcamaları dinamik olarak yükle
            db.collection("daily_expenses")
                .whereEqualTo("date", currentDate)
                .addSnapshotListener { result, error ->
                    if (error == null && result != null) {
                        val expenses = result.documents.mapNotNull { it.data?.plus("id" to it.id) }
                        dailyExpenses.clear()
                        dailyExpenses.addAll(expenses)
                        totalDailyExpense.value = expenses.sumOf { it["amount"].toString().toDoubleOrNull() ?: 0.0 }
                    }
                }
        }
    } else {
        // Mock veriler
        dailyExpenses.addAll(
            listOf(
                mapOf("name" to "Lunch", "amount" to 25.0, "id" to "1"),
                mapOf("name" to "Coffee", "amount" to 15.0, "id" to "2")
            )
        )
        totalDailyExpense.value = 40.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Add Daily Expense",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = expenseName.value,
            onValueChange = { expenseName.value = it },
            label = { Text("Expense Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = expenseAmount.value,
            onValueChange = { expenseAmount.value = it },
            label = { Text("Expense Amount") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (!isPreview) {
                    val expense = hashMapOf(
                        "name" to expenseName.value,
                        "amount" to (expenseAmount.value.toDoubleOrNull() ?: 0.0),
                        "date" to "2024-12-26"
                    )

                    FirebaseFirestore.getInstance().collection("daily_expenses")
                        .add(expense)
                        .addOnSuccessListener { document ->
                            dailyExpenses.add(expense + ("id" to document.id))
                            totalDailyExpense.value += expense["amount"].toString().toDoubleOrNull() ?: 0.0
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Daily Expense")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Today's Expenses: ${totalDailyExpense.value} $baseCurrency",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(dailyExpenses) { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Name: ${expense["name"]}", style = MaterialTheme.typography.titleMedium)
                        Text("Amount: ${expense["amount"]} $baseCurrency", style = MaterialTheme.typography.titleMedium)
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Button(
                                onClick = {
                                    navController.navigate(
                                        "editDailyExpense/${expense["id"]}/${expense["name"]}/${expense["amount"]}"
                                    )
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Edit")
                            }

                            Button(
                                onClick = {
                                    val id = expense["id"].toString()

                                    if (!isPreview) {
                                        FirebaseFirestore.getInstance().collection("daily_expenses")
                                            .document(id)
                                            .delete()
                                            .addOnSuccessListener {
                                                dailyExpenses.remove(expense)
                                                totalDailyExpense.value = dailyExpenses.sumOf {
                                                    it["amount"].toString().toDoubleOrNull() ?: 0.0
                                                }
                                            }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
