package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EditDailyExpenseScreen(
    navController: NavController,
    expenseId: String,
    expenseName: String,
    expenseAmount: Double
) {
    var name by remember { mutableStateOf(expenseName) }
    var amount by remember { mutableStateOf(expenseAmount.toString()) }

    // Preview modunda mı kontrol et
    val isPreview = LocalInspectionMode.current
    val db = if (!isPreview) FirebaseFirestore.getInstance() else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Edit Expense",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Expense Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Expense Amount") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (!isPreview) {
                    val updatedExpense = mapOf(
                        "name" to name,
                        "amount" to (amount.toDoubleOrNull() ?: 0.0)
                    )

                    db?.collection("daily_expenses")
                        ?.document(expenseId)
                        ?.update(updatedExpense)
                        ?.addOnSuccessListener {
                            navController.popBackStack()
                        }
                        ?.addOnFailureListener { e ->
                            println("Error updating expense: $e")
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }

        Button(
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
        ) {
            Text("Cancel")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditDailyExpenseScreenPreview() {
    MaterialTheme {
        val mockNavController = rememberNavController()
        // Mock verilerle çalış
        EditDailyExpenseScreen(
            navController = mockNavController,
            expenseId = "1",
            expenseName = "Lunch",
            expenseAmount = 25.0
        )
    }
}
