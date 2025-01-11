package com.denizcan.personalfinancetracker.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import com.denizcan.personalfinancetracker.workers.LimitControlWorker
import com.google.firebase.auth.FirebaseUser
import java.util.concurrent.TimeUnit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.denizcan.personalfinancetracker.network.CurrencyViewModel

@Composable
fun LimitScreen(
    navController: NavController,
    currencyViewModel: CurrencyViewModel = viewModel()
) {
    var limit by remember { mutableStateOf("") }
    var currentExpense by remember { mutableStateOf(0.0) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    // Mevcut harcamaları ve limiti yükle
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            // Limiti yükle
            db.collection("limits")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    limit = document.getDouble("limit")?.toString() ?: ""
                }

            // Toplam harcamayı hesapla
            db.collection("expenses")
                .whereEqualTo("userId", user.uid)
                .get()
                .addOnSuccessListener { result ->
                    currentExpense = result.documents.sumOf { 
                        it.getDouble("amount") ?: 0.0 
                    }
                    
                    // Harcama limitini kontrol et
                    if (limit.isNotEmpty()) {
                        currencyViewModel.checkExpenseLimit(
                            currentExpense,
                            limit.toDouble()
                        )
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Harcama Limiti Ayarla",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = limit,
            onValueChange = { newValue ->
                limit = newValue
                if (newValue.isNotEmpty()) {
                    currencyViewModel.checkExpenseLimit(
                        currentExpense,
                        newValue.toDoubleOrNull() ?: 0.0
                    )
                }
            },
            label = { Text("Aylık Limit") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // ... diğer UI elemanları
    }
}


// Gerçek zamanlı harcama dinleyici
private fun addExpenseSnapshotListener(
    db: FirebaseFirestore,
    currentUser: FirebaseUser,
    savedLimit: MutableState<Double?>,
    totalExpenses: MutableState<Double>,
    context: Context
) {
    val userId = currentUser.uid // `FirebaseUser` ile doğrudan UID alınır

    db.collection("expenses")
        .whereEqualTo("userId", userId)
        .addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("LimitScreen", "Expenses listen failed.", e)
                return@addSnapshotListener
            }

            var expenseTotal = 0.0

            if (snapshots != null && !snapshots.isEmpty) {
                expenseTotal = snapshots.documents.sumOf { it.getDouble("amount") ?: 0.0 }
            }

            totalExpenses.value += expenseTotal

            if (savedLimit.value != null && totalExpenses.value > savedLimit.value!!) {
                sendNotification(
                    context,
                    "Your expenses have exceeded the limit of ${"%.2f".format(savedLimit.value)}."
                )
            }
        }
}


private fun addDailyExpenseSnapshotListener(
    db: FirebaseFirestore,
    currentUser: FirebaseUser,
    savedLimit: MutableState<Double?>,
    totalExpenses: MutableState<Double>,
    context: Context
) {
    val userId = currentUser.uid // `FirebaseUser` ile doğrudan UID alınır

    db.collection("daily_expenses")
        .whereEqualTo("userId", userId)
        .addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("LimitScreen", "Daily expenses listen failed.", e)
                return@addSnapshotListener
            }

            var dailyExpenseTotal = 0.0

            if (snapshots != null && !snapshots.isEmpty) {
                dailyExpenseTotal = snapshots.documents.sumOf { it.getDouble("amount") ?: 0.0 }
            }

            totalExpenses.value += dailyExpenseTotal

            if (savedLimit.value != null && totalExpenses.value > savedLimit.value!!) {
                sendNotification(
                    context,
                    "Your daily expenses have exceeded the limit of ${"%.2f".format(savedLimit.value)}."
                )
            }
        }
}



// Harcamaları ve limitleri yükle
private fun loadLimitAndExpenses(
    db: FirebaseFirestore?,
    currentUser: FirebaseUser?,
    savedLimit: MutableState<Double?>,
    totalExpenses: MutableState<Double>,
    hasSentNotification: MutableState<Boolean>,
    context: Context
) {
    currentUser?.uid?.let { userId ->
        // Limiti yükle
        db?.collection("limits")
            ?.document(userId)
            ?.get()
            ?.addOnSuccessListener { document ->
                if (document.exists()) {
                    savedLimit.value = document.getDouble("limit")
                    hasSentNotification.value = document.getBoolean("notificationSent") ?: false
                }
            }

        // Harcamaları yükle
        db?.collection("expenses")
            ?.whereEqualTo("userId", userId)
            ?.get()
            ?.addOnSuccessListener { documents ->
                totalExpenses.value = documents.sumOf { it.getDouble("amount") ?: 0.0 }
            }
            ?.addOnFailureListener { e ->
                Log.e("LimitScreen", "Failed to fetch expenses: ${e.localizedMessage}")
                totalExpenses.value = 0.0
            }
    }
}


// Bildirim gönderme
internal fun sendNotification(context: Context, message: String) {
    val channelId = "expense_limit_channel"
    val channelName = "Expense Limit Alerts"
    val notificationId = System.currentTimeMillis().toInt()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val channel = NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Notifications for expense limit"
    }
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (notificationManager.getNotificationChannel(channelId) == null) {
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("Expense Limit Exceeded")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
}

// WorkManager'i başlat
fun scheduleLimitControl(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<LimitControlWorker>(
        15, TimeUnit.MINUTES
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "LimitControlWork",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}
