package com.denizcan.personalfinancetracker.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.denizcan.personalfinancetracker.screens.sendNotification
import kotlinx.coroutines.tasks.await

class LimitControlWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val userId = currentUser.uid

            try {
                // Harcamaları getir
                val expensesSnapshot = db.collection("expenses")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val totalExpenses = expensesSnapshot.sumOf { it.getDouble("amount") ?: 0.0 }

                // Limiti getir
                val limitSnapshot = db.collection("limits")
                    .document(userId)
                    .get()
                    .await()

                val limit = limitSnapshot.getDouble("limit") ?: Double.MAX_VALUE

                val hasSentNotification = limitSnapshot.getBoolean("notificationSent") ?: false

                // Limit kontrolü
                if (totalExpenses >= limit && !hasSentNotification) {
                    sendNotification(
                        context,
                        "Your expenses have exceeded the limit of ${"%.2f".format(limit)}."
                    )
                    db.collection("limits").document(userId)
                        .update("notificationSent", true)
                } else if (totalExpenses < limit) {
                    db.collection("limits").document(userId)
                        .update("notificationSent", false)
                }

                return Result.success()
            } catch (e: Exception) {
                Log.e("LimitKontrolWorker", "Error checking limit: ${e.localizedMessage}")
                return Result.retry()
            }
        }

        return Result.failure()
    }
}
