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
fun EditDailyIncomeScreen(
    navController: NavController,
    incomeId: String,
    incomeName: String,
    incomeAmount: Double
) {
    var name by remember { mutableStateOf(incomeName) }
    var amount by remember { mutableStateOf(incomeAmount.toString()) }

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
            text = "Edit Income",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Income Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Income Amount") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (!isPreview) {
                    val updatedIncome = mapOf(
                        "name" to name,
                        "amount" to (amount.toDoubleOrNull() ?: 0.0)
                    )

                    db?.collection("daily_incomes")
                        ?.document(incomeId)
                        ?.update(updatedIncome)
                        ?.addOnSuccessListener {
                            navController.popBackStack()
                        }
                        ?.addOnFailureListener { e ->
                            println("Error updating income: $e")
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
fun EditDailyIncomeScreenPreview() {
    MaterialTheme {
        val mockNavController = rememberNavController()
        // Mock verilerle çalış
        EditDailyIncomeScreen(
            navController = mockNavController,
            incomeId = "1",
            incomeName = "Freelance Work",
            incomeAmount = 100.0
        )
    }
}
