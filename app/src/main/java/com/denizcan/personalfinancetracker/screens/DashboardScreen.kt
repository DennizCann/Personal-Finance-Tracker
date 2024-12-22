package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun DashboardScreen(navController: NavController) {
    var name by remember { mutableStateOf("User") }
    var age by remember { mutableStateOf(0) }
    var income by remember { mutableStateOf(0.0) }
    var expenses by remember { mutableStateOf(0.0) }
    var remainingBalance by remember { mutableStateOf(0.0) }
    var currency by remember { mutableStateOf("TRY") } // Varsayılan para birimi

    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Firestore'dan kullanıcı bilgilerini çek
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            // Profil Bilgilerini Çek
            db.collection("profiles").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        name = document.getString("name") ?: "User"
                        currency = document.getString("currency") ?: "TRY"
                        val dateOfBirth = document.getString("dateOfBirth")
                        if (!dateOfBirth.isNullOrEmpty()) {
                            try {
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                val birthDate = LocalDate.parse(dateOfBirth, formatter)
                                age = Period.between(birthDate, LocalDate.now()).years
                            } catch (e: DateTimeParseException) {
                                age = 0
                            }
                        } else {
                            age = 0
                        }
                    }
                }

            // Gelirleri Topla
            db.collection("incomes")
                .whereEqualTo("userId", user.uid)
                .get()
                .addOnSuccessListener { result ->
                    val totalIncome = result.documents.sumOf { doc ->
                        doc.getDouble("amount") ?: 0.0
                    }
                    income = totalIncome
                    remainingBalance = income - expenses
                }

            // Giderleri Topla
            db.collection("expenses")
                .whereEqualTo("userId", user.uid)
                .get()
                .addOnSuccessListener { result ->
                    val totalExpenses = result.documents.sumOf { doc ->
                        doc.getDouble("amount") ?: 0.0
                    }
                    expenses = totalExpenses
                    remainingBalance = income - expenses
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
            Text(
                text = "Welcome, $name",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Age: $age",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(), // Tüm genişliği doldur
                    horizontalAlignment = Alignment.Start // Sola hizalı yapıldı
                ) {
                    Text("Income: ${income} $currency", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Expenses: ${expenses} $currency", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Remaining Balance: ${remainingBalance} $currency",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit Profile Button
            Button(
                onClick = {
                    navController.navigate("editProfile")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Profile")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // View Records Button
            Button(
                onClick = {
                    navController.navigate("view") // ViewScreen ekranına geçiş
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Records") // Yeni buton
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout Button
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { navController.navigate("add") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
