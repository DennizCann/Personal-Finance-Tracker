package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun AddScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "What would you like to add?",
                style = MaterialTheme.typography.headlineMedium
            )

            Button(
                onClick = { navController.navigate("addIncome") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Monthly Income")
            }

            Button(
                onClick = { navController.navigate("addExpense") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Monthly Expense")
            }

            Button(
                onClick = { navController.navigate("addDailyExpense") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Daily Expense")
            }

            Button(
                onClick = { navController.navigate("addDailyIncome") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Daily Income")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun AddScreenPreview() {
    MaterialTheme {
        // Mock NavController oluşturuluyor
        val mockNavController = androidx.navigation.compose.rememberNavController()
        AddScreen(navController = mockNavController)
    }
}
