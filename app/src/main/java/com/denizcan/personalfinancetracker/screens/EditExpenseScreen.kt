package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun EditExpenseScreen(navController: NavController, expenseId: String) {
    val isPreview = LocalInspectionMode.current // Preview modunda mı kontrolü
    val db = if (!isPreview) FirebaseFirestore.getInstance() else null
    val currentUser = if (!isPreview) FirebaseAuth.getInstance().currentUser else null

    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val categories = listOf("Education", "Food", "Transportation", "Health", "Entertainment", "Other")

    if (!isPreview) {
        // Firestore'dan gider bilgilerini çek
        LaunchedEffect(currentUser, expenseId) {
            currentUser?.let {
                db?.collection("expenses")?.document(expenseId)
                    ?.get()
                    ?.addOnSuccessListener { document ->
                        name = document.getString("name") ?: ""
                        amount = document.getDouble("amount")?.toString() ?: ""
                        selectedCategory = document.getString("category") ?: ""
                    }
                    ?.addOnFailureListener {
                        errorMessage = it.localizedMessage ?: "An error occurred"
                    }
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
            Text(text = "Edit Expense", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            // Name Input Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input Field
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    label = { Text("Category") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDropdownExpanded = !isDropdownExpanded },
                    trailingIcon = {
                        IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                            Icon(
                                imageVector = if (isDropdownExpanded)
                                    Icons.Default.ArrowDropUp
                                else
                                    Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    }
                )
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (name.isNotEmpty() && amount.isNotEmpty() && selectedCategory.isNotEmpty()) {
                            isLoading = true
                            db?.collection("expenses")?.document(expenseId)
                                ?.update("name", name, "amount", amount.toDouble(), "category", selectedCategory)
                                ?.addOnSuccessListener {
                                    isLoading = false
                                    navController.popBackStack() // Geri dön
                                }
                                ?.addOnFailureListener {
                                    isLoading = false
                                    errorMessage = it.localizedMessage ?: "An error occurred"
                                }
                        } else {
                            errorMessage = "Please fill in all fields"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditExpenseScreenPreview() {
    MaterialTheme {
        val mockNavController = androidx.navigation.compose.rememberNavController()
        EditExpenseScreen(navController = mockNavController, expenseId = "mockExpenseId")
    }
}
