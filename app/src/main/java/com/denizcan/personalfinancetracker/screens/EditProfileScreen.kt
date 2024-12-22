package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EditProfileScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Kullanıcı bilgileri için state'ler
    var name by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Firestore'dan kullanıcı bilgilerini çek
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            db.collection("profiles").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        name = document.getString("name") ?: ""
                        dateOfBirth = document.getString("dateOfBirth") ?: ""
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = e.localizedMessage ?: "An error occurred"
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
            Text("Edit Profile", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            // Name TextField
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date of Birth TextField
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { dateOfBirth = it },
                label = { Text("Date of Birth (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (name.isNotEmpty() && dateOfBirth.isNotEmpty() && currentUser != null) {
                            isLoading = true
                            val userProfile = mapOf(
                                "name" to name,
                                "dateOfBirth" to dateOfBirth,
                                "userId" to currentUser.uid
                            )

                            db.collection("profiles")
                                .document(currentUser.uid)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    isLoading = false
                                    navController.navigate("dashboard")
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = e.localizedMessage ?: "An error occurred"
                                }
                        } else {
                            errorMessage = "Please fill in all fields"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Profile")
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
