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

@Composable
fun LimitScreen(navController: NavController) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    val savedLimit = remember { mutableStateOf<Double?>(null) }
    val totalExpenses = remember { mutableStateOf(0.0) }
    val hasSentNotification = remember { mutableStateOf(false) }

    val limitInput = remember { mutableStateOf("") }
    val db = if (!isPreview) FirebaseFirestore.getInstance() else null
    val currentUser = if (!isPreview) FirebaseAuth.getInstance().currentUser else null

    LaunchedEffect(Unit) {
        if (!isPreview) {
            loadLimitAndExpenses(
                db = db,
                currentUser = currentUser,
                savedLimit = savedLimit,
                totalExpenses = totalExpenses,
                hasSentNotification = hasSentNotification,
                context = context
            )

            // Harcama dinleyicisini başlat
            if (currentUser != null && db != null) {
                addExpenseSnapshotListener(
                    db = db,
                    currentUser = currentUser,
                    savedLimit = savedLimit,
                    totalExpenses = totalExpenses, // Eksik parametre eklendi
                    context = context
                )
                addDailyExpenseSnapshotListener(
                    db = db,
                    currentUser = currentUser,
                    savedLimit = savedLimit,
                    totalExpenses = totalExpenses,
                    context = context
                )
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
            Text("Limit Settings", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(24.dp))

            if (savedLimit.value != null) {
                Text(
                    text = "Current Limit: ${"%.2f".format(savedLimit.value)}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Total Expenses: ${"%.2f".format(totalExpenses.value)}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = limitInput.value,
                onValueChange = { limitInput.value = it },
                label = { Text("Set Limit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val enteredLimit = limitInput.value.toDoubleOrNull()
                    if (enteredLimit != null && currentUser != null) {
                        val data = mapOf(
                            "limit" to enteredLimit,
                            "userId" to currentUser.uid,
                            "notificationSent" to false
                        )
                        db?.collection("limits")
                            ?.document(currentUser.uid)
                            ?.set(data)
                            ?.addOnSuccessListener {
                                savedLimit.value = enteredLimit
                                scheduleLimitControl(context) // WorkManager işini başlat
                                navController.navigate("dashboard")
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Limit")
            }

        }
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
