package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ViewScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "What would you like to view?",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Income Button
            Button(
                onClick = { navController.navigate("viewIncome") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Incomes")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expense Button
            Button(
                onClick = { navController.navigate("viewExpense") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Expenses")
            }
        }
    }
}
