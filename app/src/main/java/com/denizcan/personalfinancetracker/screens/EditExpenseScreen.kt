package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EditExpenseScreen(navController: NavController, expenseId: String) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Firestore'dan gider bilgilerini çek
    LaunchedEffect(currentUser, expenseId) {
        currentUser?.let {
            db.collection("expenses").document(expenseId)
                .get()
                .addOnSuccessListener { document ->
                    name = document.getString("name") ?: ""
                    amount = document.getDouble("amount")?.toString() ?: ""
                }
                .addOnFailureListener {
                    errorMessage = it.localizedMessage ?: "An error occurred"
                }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "Edit Expense", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            // Name Input Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input Field
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (name.isNotEmpty() && amount.isNotEmpty()) {
                            isLoading = true
                            db.collection("expenses").document(expenseId)
                                .update("name", name, "amount", amount.toDouble())
                                .addOnSuccessListener {
                                    isLoading = false
                                    navController.popBackStack() // Geri dön
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    errorMessage = it.localizedMessage ?: "An error occurred"
                                }
                        } else {
                            errorMessage = "Please fill in all fields"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
