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
fun ViewExpenseScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var expenses by remember { mutableStateOf(listOf<Pair<String, Double>>()) }

    // Firestore'dan giderleri çek
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            db.collection("expenses")
                .whereEqualTo("userId", user.uid)
                .get()
                .addOnSuccessListener { result ->
                    expenses = result.documents.map { doc ->
                        Pair(
                            doc.getString("name") ?: "Unknown Expense",
                            doc.getDouble("amount") ?: 0.0
                        )
                    }
                }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Expenses",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Listeyi göster
            expenses.forEach { (name, amount) ->
                Text(
                    text = "$name: $${amount}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
