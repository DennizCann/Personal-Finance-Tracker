package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.denizcan.personalfinancetracker.CustomTopBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
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

    Canvas(
        modifier = Modifier.size(180.dp).padding(16.dp)
    ) {
        val pieSize = Size(size.minDimension, size.minDimension)
        val center = Offset(size.width / 2, size.height / 2)
        val radius = pieSize.width / 2

        var startAngle = -90f

        // Gelir dilimi
        val incomeSweepAngle = 360 * incomePercentage
        drawArc(
            color = Color(0xFF6200EE),
            startAngle = startAngle,
            sweepAngle = incomeSweepAngle,
            useCenter = true,
            size = pieSize,
            topLeft = Offset(center.x - radius, center.y - radius)
        )
        drawTextOnArc(center, radius, startAngle, incomeSweepAngle / 2, "Income")

        startAngle += incomeSweepAngle

        // Gider dilimi
        val expensesSweepAngle = 360 * expensesPercentage
        drawArc(
            color = Color(0xFF03DAC6),
            startAngle = startAngle,
            sweepAngle = expensesSweepAngle,
            useCenter = true,
            size = pieSize,
            topLeft = Offset(center.x - radius, center.y - radius)
        )
        drawTextOnArc(center, radius, startAngle, expensesSweepAngle / 2, "Expenses")

        startAngle += expensesSweepAngle

        // Kalan bakiye dilimi
        val remainingSweepAngle = 360 * remainingPercentage
        drawArc(
            color = Color(0xFFBB86FC),
            startAngle = startAngle,
            sweepAngle = remainingSweepAngle,
            useCenter = true,
            size = pieSize,
            topLeft = Offset(center.x - radius, center.y - radius)
        )
        drawTextOnArc(center, radius, startAngle, remainingSweepAngle / 2, "Remaining")
    }
}

fun DrawScope.drawTextOnArc(center: Offset, radius: Float, startAngle: Float, middleAngle: Float, text: String) {
    val radians = Math.toRadians((startAngle + middleAngle).toDouble())
    val x = center.x + radius / 2 * Math.cos(radians).toFloat()
    val y = center.y + radius / 2 * Math.sin(radians).toFloat()

    drawContext.canvas.nativeCanvas.apply {
        drawText(
            text,
            x,
            y,
            android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 32f
            }
        )
    }
}


@Composable
fun DashboardScreen(navController: NavController) {
    var name by remember { mutableStateOf("User") }
    var age by remember { mutableStateOf(0) }
    var income by remember { mutableStateOf(0.0) }
    var expenses by remember { mutableStateOf(0.0) }
    var dailyExpensesForMonth by remember { mutableStateOf(0.0) }
    var remainingBalance by remember { mutableStateOf(0.0) }
    var currency by remember { mutableStateOf("TRY") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }

    val isPreview = LocalInspectionMode.current
    val db = if (!isPreview) FirebaseFirestore.getInstance() else null
    val currentUser = if (!isPreview) FirebaseAuth.getInstance().currentUser else null

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    if (!isPreview) {
        LaunchedEffect(currentUser) {
            currentUser?.let { user ->
                // Kullanıcı verilerini Firestore'dan yükleme kodu
                db?.collection("profiles")?.document(user.uid)
                    ?.get()
                    ?.addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            name = document.getString("name") ?: "User"
                            currency = document.getString("currency") ?: "TRY"
                            profileImageUrl = document.getString("profileImageUrl")
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

                // Gelir ve gider verilerini Firestore'dan yükleme kodu
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

                db?.collection("expenses")
                    ?.whereEqualTo("userId", user.uid)
                    ?.get()
                    ?.addOnSuccessListener { result ->
                        val totalExpenses = result.documents.sumOf { doc ->
                            doc.getDouble("amount") ?: 0.0
                        }
                        expenses = totalExpenses + dailyExpensesForMonth
                        remainingBalance = income - expenses
                    }

                // Günlük harcamalar için toplam hesaplama
                db?.collection("daily_expenses")
                    ?.whereEqualTo("userId", user.uid)
                    ?.get()
                    ?.addOnSuccessListener { result ->
                        val currentMonth = LocalDate.now().monthValue
                        val monthlyExpenses = result.documents.filter { doc ->
                            val dateString = doc.getString("date")
                            if (!dateString.isNullOrEmpty()) {
                                try {
                                    val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                    date.monthValue == currentMonth
                                } catch (e: DateTimeParseException) {
                                    false
                                }
                            } else false
                        }.sumOf { it.getDouble("amount") ?: 0.0 }

                        dailyExpensesForMonth = monthlyExpenses
                        expenses += dailyExpensesForMonth
                        remainingBalance = income - expenses
                    }

                // Günlük gelirler için toplam hesaplama
                db?.collection("daily_incomes")
                    ?.whereEqualTo("userId", user.uid)
                    ?.get()
                    ?.addOnSuccessListener { result ->
                        val currentMonth = LocalDate.now().monthValue
                        val monthlyIncomes = result.documents.filter { doc ->
                            val dateString = doc.getString("date")
                            if (!dateString.isNullOrEmpty()) {
                                try {
                                    val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                    date.monthValue == currentMonth
                                } catch (e: DateTimeParseException) {
                                    false
                                }
                            } else false
                        }.sumOf { it.getDouble("amount") ?: 0.0 }

                        income += monthlyIncomes // Gelire ekle
                        remainingBalance = income - expenses
                    }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(navController)
        }
    ) {
        Scaffold(
            topBar = {
                CustomTopBar(onMenuClick = {
                    coroutineScope.launch {
                        drawerState.open()
                    }
                })
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("add") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("+", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                }
            },
            content = { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (profileImageUrl != null) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No Image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Welcome, $name",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = "Age: $age",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(24.dp))

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
                                Text(
                                    "Income: ${income} $currency",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Expenses: ${expenses} $currency",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Remaining Balance: ${remainingBalance} $currency",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun DrawerContent(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Arka planı beyaz yap
            .padding(16.dp)
    ) {
        Text("Menu", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(8.dp))
        Divider()
        DrawerItem("Profile", onClick = { navController.navigate("editProfile") })
        DrawerItem("Records", onClick = { navController.navigate("view") })
        DrawerItem("Limit", onClick = { navController.navigate("limit") })
        DrawerItem("Exchange Rates", onClick = { navController.navigate("exchangeRates") })
        DrawerItem("Logout", onClick = {
            FirebaseAuth.getInstance().signOut()
            navController.navigate("login") {
                popUpTo("login") { inclusive = true }
            }
        })
    }
}

@Composable
fun DrawerItem(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
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