package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.clickable
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
    var incomes by remember { mutableStateOf(listOf<Triple<String, String, Double>>()) }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            db.collection("incomes")
                .whereEqualTo("userId", user.uid)
                .get()
                .addOnSuccessListener { result ->
                    incomes = result.documents.map { doc ->
                        Triple(
                            doc.id, // Firestore belge ID'si
                            doc.getString("name") ?: "Unknown Income", // İsim
                            doc.getDouble("amount") ?: 0.0 // Miktar
                        )
                    }
                }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "Your Incomes", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            incomes.forEach { (id, name, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("editIncome/$id") // Düzenleme ekranına yönlendirme
                        }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "$name",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$${amount}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
