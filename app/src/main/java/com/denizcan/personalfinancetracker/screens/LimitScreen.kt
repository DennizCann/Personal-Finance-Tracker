package com.denizcan.personalfinancetracker.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

@Composable
fun LimitScreen(navController: NavController) {
    val context = LocalContext.current // Context'i LocalContext ile al
    var totalExpenses by remember { mutableStateOf(0.0) }
    var limit by remember { mutableStateOf("") }
    var savedLimit by remember { mutableStateOf<Double?>(null) } // Girilmiş limiti saklamak için
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var hasNotificationPermission by remember { mutableStateOf(false) } // Bildirim izni durumu

    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Bildirim izni kontrolü ve talebi
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 ve üzeri
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                hasNotificationPermission = true
            }
        } else {
            hasNotificationPermission = true
        }
    }

    // Giderleri ve limiti Firestore'dan çek
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            isLoading = true

            db.collection("expenses")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { documents ->
                    totalExpenses = documents.sumOf { it.getDouble("amount") ?: 0.0 }
                    isLoading = false

                    // Harcamalar limiti aşıyor mu kontrol et
                    if (savedLimit != null && totalExpenses >= savedLimit!! && hasNotificationPermission) {
                        sendNotification(
                            context,
                            "Your expenses have exceeded the limit of ${"%.2f".format(savedLimit)}."
                        )
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = e.localizedMessage ?: "Error fetching expenses"
                    isLoading = false
                }

            // Kaydedilmiş limiti çek
            db.collection("limits").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        savedLimit = document.getDouble("limit") // Girilmiş limit
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = e.localizedMessage ?: "Error fetching limit"
                }
        } else {
            errorMessage = "User not authenticated"
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

            // Kaydedilmiş limiti göster
            if (savedLimit != null) {
                Text(
                    text = "Current Limit: ${"%.2f".format(savedLimit)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Toplam Giderler Gösterimi
            Text(
                text = "Total Expenses: ${"%.2f".format(totalExpenses)}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Limit Girişi
            OutlinedTextField(
                value = limit,
                onValueChange = { limit = it },
                label = { Text("Set Limit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Kaydet Butonu
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        val enteredLimit = limit.toDoubleOrNull()
                        if (enteredLimit != null && currentUser != null) {
                            isLoading = true
                            val data = mapOf(
                                "limit" to enteredLimit,
                                "userId" to currentUser.uid
                            )
                            db.collection("limits")
                                .document(currentUser.uid)
                                .set(data)
                                .addOnSuccessListener {
                                    isLoading = false
                                    savedLimit = enteredLimit // Yeni limiti güncelle
                                    navController.navigate("dashboard")
                                }
                                .addOnFailureListener { e ->
                                    errorMessage = e.localizedMessage ?: "Error saving limit"
                                    isLoading = false
                                }
                        } else {
                            errorMessage = "Please enter a valid limit"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Limit")
                }
            }

            // Hata Mesajı
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun sendNotification(context: Context, message: String) {
    val channelId = "expense_limit_channel"
    val channelName = "Expense Limit Alerts"
    val notificationId = 1

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 ve üzeri
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // İzin verilmemişse işlem yapma
            println("Notification permission not granted")
            return
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for expense limit"
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    try {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Expense Limit Exceeded")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (e: SecurityException) {
        println("SecurityException: Notification permission is not granted.")
    }
}
