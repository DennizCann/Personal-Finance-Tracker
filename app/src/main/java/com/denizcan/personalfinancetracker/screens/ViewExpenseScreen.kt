package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ViewExpenseScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var expenses by remember { mutableStateOf(listOf<Triple<String, String, Double>>()) }
    var currency by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Firestore'dan giderleri ve para birimini çek
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            // Kullanıcının seçtiği para birimini al
            db.collection("profiles").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    currency = document.getString("currency") ?: "USD" // Varsayılan USD
                }

            // Giderleri al
            db.collection("expenses")
                .whereEqualTo("userId", user.uid)
                .get()
                .addOnSuccessListener { result ->
                    expenses = result.documents.map { doc ->
                        Triple(
                            doc.id, // Belge ID'si
                            doc.getString("name") ?: "Unknown Expense", // İsim
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
            Text(
                text = "Your Expenses",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Başlıklar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Name",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(32.dp)) // Silme ikonu için boşluk
            }

            Spacer(modifier = Modifier.height(8.dp))

            expenses.forEach { (id, name, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .clickable { // Tıklanabilirlik ekleniyor
                            navController.navigate("editExpense/$id") // EditExpenseScreen'e yönlendirme
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(2f),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "$amount $currency",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    IconButton(
                        onClick = {
                            isLoading = true
                            db.collection("expenses").document(id)
                                .delete()
                                .addOnSuccessListener {
                                    expenses = expenses.filterNot { it.first == id } // Silinen öğeyi listeden kaldır
                                    isLoading = false
                                }
                                .addOnFailureListener { error ->
                                    errorMessage = error.localizedMessage ?: "Failed to delete expense"
                                    isLoading = false
                                }
                        },
                        modifier = Modifier.size(24.dp) // İkon buton boyutu küçültüldü
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                            contentDescription = "Delete Expense",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp) // İkon boyutu küçültüldü
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
