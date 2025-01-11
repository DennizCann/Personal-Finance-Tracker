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
fun AddGoalScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Yeni Hedef Ekle",
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
                if (name.isEmpty() || targetAmount.isEmpty()) {
                    errorMessage = "Lütfen tüm alanları doldurun"
                    return@Button
                }

                isLoading = true
                currentUser?.let { user ->
                    val goalData = mapOf(
                        "userId" to user.uid,
                        "name" to name,
                        "targetAmount" to (targetAmount.toDoubleOrNull() ?: 0.0),
                        "currentAmount" to 0.0
                    )

                    db.collection("goals")
                        .add(goalData)
                        .addOnSuccessListener {
                            isLoading = false
                            navController.navigateUp()
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            errorMessage = "Hata: ${e.localizedMessage}"
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Hedef Ekle")
            }
        }
    }
} 