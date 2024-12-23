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
fun ViewIncomeScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var incomes by remember { mutableStateOf(listOf<Triple<String, String, Double>>()) }
    var currency by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Firestore'dan gelir ve para birimini çek
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            // Kullanıcının seçtiği para birimini al
            db.collection("profiles").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    currency = document.getString("currency") ?: "USD" // Varsayılan USD
                }

            // Gelirleri al
            db.collection("incomes")
                .whereEqualTo("userId", user.uid)
                .get()
                .addOnSuccessListener { result ->
                    incomes = result.documents.map { doc ->
                        Triple(
                            doc.id, // Belge ID'si
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
            Text(
                text = "Your Incomes",
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
                    fontSize = 14.sp, // Başlık yazı boyutu küçültüldü
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp, // Başlık yazı boyutu küçültüldü
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(32.dp)) // İkon için boşluk
            }

            Spacer(modifier = Modifier.height(8.dp))

            incomes.forEach { (id, name, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .clickable { // Gelir tıklanabilir hale getirildi
                            navController.navigate("editIncome/$id") // EditIncomeScreen'e yönlendirme
                        },
                    verticalAlignment = Alignment.CenterVertically, // Satır hizalama
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp, // Yazı boyutu küçültüldü
                        modifier = Modifier.weight(2f),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "$amount $currency",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp, // Yazı boyutu küçültüldü
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    IconButton(
                        onClick = {
                            isLoading = true
                            db.collection("incomes").document(id)
                                .delete()
                                .addOnSuccessListener {
                                    incomes = incomes.filterNot { it.first == id } // Silinen öğeyi listeden kaldır
                                    isLoading = false
                                }
                                .addOnFailureListener { error ->
                                    errorMessage = error.localizedMessage ?: "Failed to delete income"
                                    isLoading = false
                                }
                        },
                        modifier = Modifier.size(24.dp) // İkon boyutu küçültüldü
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                            contentDescription = "Delete Income",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp) // İkon resmi küçültüldü
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
