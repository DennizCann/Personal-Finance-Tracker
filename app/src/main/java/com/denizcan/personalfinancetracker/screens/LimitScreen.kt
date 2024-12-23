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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

@Composable
fun LimitScreen(navController: NavController) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    var totalExpenses by remember { mutableStateOf(0.0) }
    var limit by remember { mutableStateOf("") }
    var savedLimit by remember { mutableStateOf<Double?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var hasNotificationPermission by remember { mutableStateOf(true) }
    var hasSentNotification by remember { mutableStateOf(false) }

    val db = if (!isPreview) FirebaseFirestore.getInstance() else null
    val currentUser = if (!isPreview) FirebaseAuth.getInstance().currentUser else null

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasNotificationPermission = isGranted }

    if (!isPreview) {
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

        LaunchedEffect(currentUser) {
            if (currentUser != null) {
                isLoading = true
                db?.collection("expenses")
                    ?.whereEqualTo("userId", currentUser.uid)
                    ?.get()
                    ?.addOnSuccessListener { documents ->
                        totalExpenses = documents.sumOf { it.getDouble("amount") ?: 0.0 }
                        isLoading = false

                        if (savedLimit != null && totalExpenses >= savedLimit!! && !hasSentNotification) {
                            sendNotification(
                                context,
                                "Your expenses have exceeded the limit of ${"%.2f".format(savedLimit)}."
                            )
                            hasSentNotification = true
                            db?.collection("limits")?.document(currentUser.uid)
                                ?.update("notificationSent", true)
                        } else if (savedLimit != null && totalExpenses < savedLimit!!) {
                            hasSentNotification = false
                            db?.collection("limits")?.document(currentUser.uid)
                                ?.update("notificationSent", false)
                        }
                    }
                    ?.addOnFailureListener { e ->
                        errorMessage = e.localizedMessage ?: "Error fetching expenses"
                        isLoading = false
                    }

                db?.collection("limits")?.document(currentUser.uid)
                    ?.get()
                    ?.addOnSuccessListener { document ->
                        if (document.exists()) {
                            savedLimit = document.getDouble("limit")
                            hasSentNotification = document.getBoolean("notificationSent") ?: false
                        }
                        isLoading = false
                    }
                    ?.addOnFailureListener { e ->
                        errorMessage = e.localizedMessage ?: "Error fetching limit"
                        isLoading = false
                    }
            } else {
                errorMessage = "User not authenticated"
                isLoading = false
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

            if (savedLimit != null) {
                Text(
                    text = "Current Limit: ${"%.2f".format(savedLimit)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Total Expenses: ${"%.2f".format(totalExpenses)}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = limit,
                onValueChange = { limit = it },
                label = { Text("Set Limit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                            db?.collection("limits")
                                ?.document(currentUser.uid)
                                ?.set(data)
                                ?.addOnSuccessListener {
                                    isLoading = false
                                    savedLimit = enteredLimit
                                    navController.navigate("dashboard")
                                }
                                ?.addOnFailureListener { e ->
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
    val notificationId = System.currentTimeMillis().toInt()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
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

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("Expense Limit Exceeded")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
}

@Preview(showBackground = true)
@Composable
fun LimitScreenPreview() {
    MaterialTheme {
        val mockNavController = androidx.navigation.compose.rememberNavController()
        LimitScreen(navController = mockNavController)
    }
}
