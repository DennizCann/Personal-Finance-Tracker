package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.denizcan.personalfinancetracker.network.CurrencyViewModel
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EditGoalScreen(
    navController: NavController,
    goalId: String,
    currencyViewModel: CurrencyViewModel
) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var currentAmount by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()

    // Mevcut hedef verilerini yükle
    LaunchedEffect(goalId) {
        db.collection("goals")
            .document(goalId)
            .get()
            .addOnSuccessListener { document ->
                name = document.getString("name") ?: ""
                targetAmount = document.getDouble("targetAmount")?.toString() ?: ""
                currentAmount = document.getDouble("currentAmount")?.toString() ?: ""
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Hedefi Düzenle",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Hedef Adı") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = targetAmount,
            onValueChange = { targetAmount = it },
            label = { Text("Hedef Tutar") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentAmount,
            onValueChange = { currentAmount = it },
            label = { Text("Mevcut Tutar") },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isEmpty() || targetAmount.isEmpty() || currentAmount.isEmpty()) {
                    errorMessage = "Lütfen tüm alanları doldurun"
                    return@Button
                }

                isLoading = true
                val updates = mapOf(
                    "name" to name,
                    "targetAmount" to (targetAmount.toDoubleOrNull() ?: 0.0),
                    "currentAmount" to (currentAmount.toDoubleOrNull() ?: 0.0)
                )

                db.collection("goals")
                    .document(goalId)
                    .update(updates)
                    .addOnSuccessListener {
                        isLoading = false
                        val current = currentAmount.toDoubleOrNull() ?: 0.0
                        val target = targetAmount.toDoubleOrNull() ?: 0.0
                        if (current >= target) {
                            currencyViewModel.updateGoalProgress(name, 100)
                        }
                        navController.navigateUp()
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        errorMessage = "Hata: ${e.localizedMessage}"
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Güncelle")
            }
        }
    }
} 