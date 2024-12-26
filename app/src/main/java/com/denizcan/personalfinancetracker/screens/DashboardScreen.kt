package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun PieChart(income: Double, expenses: Double, remainingBalance: Double) {
    val total = income + expenses + remainingBalance
    if (total == 0.0) return // Grafik çizmek için veri yoksa çıkış

    val incomePercentage = (income / total).toFloat()
    val expensesPercentage = (expenses / total).toFloat()
    val remainingPercentage = (remainingBalance / total).toFloat()

    // Canvas bir @Composable bağlamda olduğundan burada kullanılabilir
    Canvas(
        modifier = Modifier.size(180.dp).padding(16.dp)
    ) {
        val pieSize = Size(size.minDimension, size.minDimension)
        val centerOffset = Offset(size.width / 2 - pieSize.width / 2, size.height / 2 - pieSize.height / 2)

        var startAngle = -90f

        // Gelir dilimi
        drawArc(
            color = Color(0xFF6200EE), // Sabit bir renk
            startAngle = startAngle,
            sweepAngle = 360 * incomePercentage,
            useCenter = true,
            topLeft = centerOffset,
            size = pieSize
        )
        startAngle += 360 * incomePercentage

        // Gider dilimi
        drawArc(
            color = Color(0xFF03DAC6), // Sabit bir renk
            startAngle = startAngle,
            sweepAngle = 360 * expensesPercentage,
            useCenter = true,
            topLeft = centerOffset,
            size = pieSize
        )
        startAngle += 360 * expensesPercentage

        // Kalan bakiye dilimi
        drawArc(
            color = Color(0xFFBB86FC), // Sabit bir renk
            startAngle = startAngle,
            sweepAngle = 360 * remainingPercentage,
            useCenter = true,
            topLeft = centerOffset,
            size = pieSize
        )
    }
}


@Composable
fun DashboardScreen(navController: NavController) {
    var name by remember { mutableStateOf("User") }
    var age by remember { mutableStateOf(0) }
    var income by remember { mutableStateOf(0.0) }
    var expenses by remember { mutableStateOf(0.0) }
    var remainingBalance by remember { mutableStateOf(0.0) }
    var currency by remember { mutableStateOf("TRY") }

    val isPreview = LocalInspectionMode.current // Preview modunda mı kontrolü
    val db = if (!isPreview) FirebaseFirestore.getInstance() else null
    val currentUser = if (!isPreview) FirebaseAuth.getInstance().currentUser else null

    // Firestore'dan kullanıcı bilgilerini çek
    if (!isPreview) {
        LaunchedEffect(currentUser) {
            currentUser?.let { user ->
                // Profil Bilgilerini Çek
                db?.collection("profiles")?.document(user.uid)
                    ?.get()
                    ?.addOnSuccessListener { document ->
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
                db?.collection("incomes")
                    ?.whereEqualTo("userId", user.uid)
                    ?.get()
                    ?.addOnSuccessListener { result ->
                        val totalIncome = result.documents.sumOf { doc ->
                            doc.getDouble("amount") ?: 0.0
                        }
                        income = totalIncome
                        remainingBalance = income - expenses
                    }

                // Giderleri Topla
                db?.collection("expenses")
                    ?.whereEqualTo("userId", user.uid)
                    ?.get()
                    ?.addOnSuccessListener { result ->
                        val totalExpenses = result.documents.sumOf { doc ->
                            doc.getDouble("amount") ?: 0.0
                        }
                        expenses = totalExpenses
                        remainingBalance = income - expenses
                    }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Logout Butonu Sol Altta
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate("login") {
                    popUpTo("login") { inclusive = true }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
        ) {
            Text("Logout", style = MaterialTheme.typography.bodySmall)
        }

        // Ana İçerik
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

            // Pasta Grafiği
            PieChart(income = income, expenses = expenses, remainingBalance = remainingBalance)

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
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

            // Diğer Butonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { navController.navigate("editProfile") },
                    modifier = Modifier.size(100.dp, 40.dp)
                ) {
                    Text("Profile")
                }

                Button(
                    onClick = { navController.navigate("view") },
                    modifier = Modifier.size(100.dp, 40.dp)
                ) {
                    Text("Records")
                }

                Button(
                    onClick = { navController.navigate("limit") },
                    modifier = Modifier.size(100.dp, 40.dp)
                ) {
                    Text("Limit")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Exchange Rates Butonu
            Button(
                onClick = { navController.navigate("exchangeRates") },
                modifier = Modifier.size(200.dp, 40.dp)
            ) {
                Text("Exchange Rates")
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


@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    MaterialTheme {
        val mockNavController = rememberNavController()
        DashboardScreen(navController = mockNavController)
    }
}

@Preview(showBackground = true)
@Composable
fun PieChartPreview() {
    MaterialTheme {
        PieChart(
            income = 5000.0,
            expenses = 3000.0,
            remainingBalance = 2000.0
        )
    }
}