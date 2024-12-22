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
fun ViewIncomeScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var incomes by remember { mutableStateOf(listOf<Pair<String, Double>>()) }

    // Firestore'dan gelirleri çek
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            db.collection("incomes")
                .whereEqualTo("userId", user.uid)
                .get()
                .addOnSuccessListener { result ->
                    incomes = result.documents.map { doc ->
                        Pair(
                            doc.getString("name") ?: "Unknown Income",
                            doc.getDouble("amount") ?: 0.0
                        )
                    }
                }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart // Üstte ve sola hizala
    ) {
        Column(
            horizontalAlignment = Alignment.Start, // Tüm metinleri sola hizala
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Incomes",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Listeyi göster
            incomes.forEach { (name, amount) ->
                Text(
                    text = "$name: $${amount}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth() // Tüm genişliği doldur
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
