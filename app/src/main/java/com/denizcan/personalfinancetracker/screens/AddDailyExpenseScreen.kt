package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denizcan.personalfinancetracker.network.CurrencyViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AddDailyExpenseScreen(
    navController: NavController,
    userId: String, // Kullanıcı kimliği
    currencyViewModel: CurrencyViewModel = viewModel()
) {
    // Firestore ve Firebase Authentication
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: "" // Dinamik olarak kullanıcı kimliğini al

    val expenseName = remember { mutableStateOf("") }
    val expenseAmount = remember { mutableStateOf("") }
    val dailyExpenses = remember { mutableStateListOf<Map<String, Any>>() }
    val totalDailyExpense = remember { mutableStateOf(0.0) }
    val baseCurrency by currencyViewModel.baseCurrency.observeAsState("TRY")
    val currentDate = "2024-12-26" // Örnek tarih

    // Günlük harcamaları dinamik olarak yükle
    LaunchedEffect(currentDate, userId) {
        if (userId.isNotEmpty()) { // Kullanıcı kimliği kontrolü
            db.collection("daily_expenses")
                .whereEqualTo("date", currentDate)
                .whereEqualTo("userId", userId) // Kullanıcı kimliği ile filtrele
                .addSnapshotListener { result, error ->
                    if (error == null && result != null) {
                        val expenses = result.documents.mapNotNull { it.data?.plus("id" to it.id) }
                        dailyExpenses.clear()
                        dailyExpenses.addAll(expenses)
                        totalDailyExpense.value = expenses.sumOf { it["amount"].toString().toDoubleOrNull() ?: 0.0 }
                    } else {
                        println("Error fetching daily expenses: ${error?.localizedMessage}")
                    }
                }
        } else {
            println("Error: User ID is empty.")
        }
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
                val amount = expenseAmount.value.toDoubleOrNull() ?: 0.0
                if (expenseName.value.isNotEmpty() && amount > 0) {
                    if (userId.isNotEmpty()) {
                        val expense = hashMapOf(
                            "name" to expenseName.value,
                            "amount" to amount,
                            "date" to currentDate,
                            "userId" to userId // Kullanıcı kimliği ekleniyor
                        )

                        db.collection("daily_expenses")
                            .add(expense)
                            .addOnSuccessListener {
                                println("Expense added successfully.")
                                expenseName.value = ""
                                expenseAmount.value = ""
                            }
                            .addOnFailureListener { exception ->
                                println("Error adding document: ${exception.localizedMessage}")
                            }
                    } else {
                        println("Error: User ID is empty.")
                    }
                } else {
                    println("Error: Invalid input.")
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
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

                                    db.collection("daily_expenses")
                                        .document(id)
                                        .delete()
                                        .addOnSuccessListener {
                                            println("Expense deleted successfully.")
                                        }
                                        .addOnFailureListener { exception ->
                                            println("Error deleting document: ${exception.localizedMessage}")
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
