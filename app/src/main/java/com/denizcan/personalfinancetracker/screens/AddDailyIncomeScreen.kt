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
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AddDailyIncomeScreen(
    navController: NavController,
    userId: String,
    currencyViewModel: CurrencyViewModel = viewModel()
) {
    val db = FirebaseFirestore.getInstance()

    val incomeName = remember { mutableStateOf("") }
    val incomeAmount = remember { mutableStateOf("") }
    val dailyIncomes = remember { mutableStateListOf<Map<String, Any>>() }
    val totalDailyIncome = remember { mutableStateOf(0.0) }
    val baseCurrency by currencyViewModel.baseCurrency.observeAsState("TRY")
    val currentDate = java.time.LocalDate.now().toString() // Dinamik tarih

    // Günlük gelirleri dinamik olarak yükle
    LaunchedEffect(currentDate, userId) {
        if (userId.isNotEmpty()) {
            db.collection("daily_incomes")
                .whereEqualTo("date", currentDate)
                .whereEqualTo("userId", userId)
                .addSnapshotListener { result, error ->
                    if (error == null && result != null) {
                        val incomes = result.documents.mapNotNull { it.data?.plus("id" to it.id) }
                        dailyIncomes.clear()
                        dailyIncomes.addAll(incomes)
                        totalDailyIncome.value = incomes.sumOf { it["amount"].toString().toDoubleOrNull() ?: 0.0 }
                    } else {
                        println("Error fetching daily incomes: ${error?.localizedMessage}")
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
            text = "Add Daily Income",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = incomeName.value,
            onValueChange = { incomeName.value = it },
            label = { Text("Income Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = incomeAmount.value,
            onValueChange = { incomeAmount.value = it },
            label = { Text("Income Amount") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val amount = incomeAmount.value.toDoubleOrNull() ?: 0.0
                if (incomeName.value.isNotEmpty() && amount > 0) {
                    if (userId.isNotEmpty()) {
                        val income = hashMapOf(
                            "name" to incomeName.value,
                            "amount" to amount,
                            "date" to currentDate,
                            "userId" to userId
                        )

                        db.collection("daily_incomes")
                            .add(income)
                            .addOnSuccessListener {
                                println("Income added successfully.")
                                incomeName.value = ""
                                incomeAmount.value = ""
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
            Text("Save Daily Income")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Today's Incomes: ${totalDailyIncome.value} $baseCurrency",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(dailyIncomes) { income ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Name: ${income["name"]}", style = MaterialTheme.typography.titleMedium)
                        Text("Amount: ${income["amount"]} $baseCurrency", style = MaterialTheme.typography.titleMedium)

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Button(
                                onClick = {
                                    navController.navigate(
                                        "editDailyIncome/${income["id"]}/${income["name"]}/${income["amount"]}"
                                    )
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Edit")
                            }

                            Button(
                                onClick = {
                                    val id = income["id"].toString()

                                    db.collection("daily_incomes")
                                        .document(id)
                                        .delete()
                                        .addOnSuccessListener {
                                            println("Income deleted successfully.")
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

