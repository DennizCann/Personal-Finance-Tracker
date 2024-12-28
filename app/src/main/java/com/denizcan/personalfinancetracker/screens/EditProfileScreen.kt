package com.denizcan.personalfinancetracker.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun EditProfileScreen(navController: NavController) {
    val isPreview = LocalInspectionMode.current
    val db = if (!isPreview) FirebaseFirestore.getInstance() else null
    val currentUser = if (!isPreview) FirebaseAuth.getInstance().currentUser else null
    val storageRef = if (!isPreview) FirebaseStorage.getInstance().reference else null

    var name by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("TRY") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val currencyOptions = listOf("TRY", "USD", "EUR", "GBP")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    if (!isPreview) {
        LaunchedEffect(currentUser) {
            currentUser?.let { user ->
                db?.collection("profiles")?.document(user.uid)
                    ?.get()
                    ?.addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            name = document.getString("name") ?: ""
                            dateOfBirth = document.getString("dateOfBirth") ?: ""
                            currency = document.getString("currency") ?: "TRY"
                            val profileImageUrl = document.getString("profileImageUrl")
                            selectedImageUri = profileImageUrl?.let { Uri.parse(it) }
                        }
                    }
                    ?.addOnFailureListener { e ->
                        errorMessage = e.localizedMessage ?: "An error occurred"
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
            Text("Edit Profile", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            // Profil Resmi
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        imagePickerLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Add Image", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

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

            Spacer(modifier = Modifier.height(16.dp))

            // Currency Dropdown
            Text("Select Currency", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }

            Box {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = currency)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    currencyOptions.forEach { option ->
                        DropdownMenuItem(
                            onClick = {
                                currency = option
                                expanded = false
                            },
                            text = { Text(option) }
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
                        if (name.isNotEmpty() && dateOfBirth.isNotEmpty() && currentUser != null) {
                            isLoading = true

                            val userId = currentUser.uid
                            val fileRef = storageRef?.child("profile_images/$userId.jpg")
                            val uploadTask = selectedImageUri?.let { fileRef?.putFile(it) }

                            if (fileRef != null) {
                                fileRef.let { ref ->
                                    uploadTask?.addOnSuccessListener {
                                        ref.downloadUrl.addOnSuccessListener { uri ->
                                            val userProfile = mapOf(
                                                "name" to name,
                                                "dateOfBirth" to dateOfBirth,
                                                "currency" to currency,
                                                "profileImageUrl" to uri.toString(),
                                                "userId" to userId
                                            )

                                            db?.collection("profiles")
                                                ?.document(userId)
                                                ?.set(userProfile)
                                                ?.addOnSuccessListener {
                                                    isLoading = false
                                                    navController.navigate("dashboard")
                                                }
                                                ?.addOnFailureListener { e ->
                                                    isLoading = false
                                                    errorMessage = e.localizedMessage ?: "An error occurred"
                                                }
                                        }
                                    }?.addOnFailureListener { e ->
                                        isLoading = false
                                        errorMessage = e.localizedMessage ?: "File upload failed"
                                    }
                                }
                            } else {
                                // Resim yüklenmemişse yalnızca diğer bilgileri kaydet
                                val userProfile = mapOf(
                                    "name" to name,
                                    "dateOfBirth" to dateOfBirth,
                                    "currency" to currency,
                                    "userId" to userId
                                )

                                db?.collection("profiles")
                                    ?.document(userId)
                                    ?.set(userProfile)
                                    ?.addOnSuccessListener {
                                        isLoading = false
                                        navController.navigate("dashboard")
                                    }
                                    ?.addOnFailureListener { e ->
                                        isLoading = false
                                        errorMessage = e.localizedMessage ?: "An error occurred"
                                    }
                            }

                                ?: run {
                                // Resim yüklenmemişse yalnızca diğer bilgileri kaydet
                                val userProfile = mapOf(
                                    "name" to name,
                                    "dateOfBirth" to dateOfBirth,
                                    "currency" to currency,
                                    "userId" to userId
                                )

                                db?.collection("profiles")
                                    ?.document(userId)
                                    ?.set(userProfile)
                                    ?.addOnSuccessListener {
                                        isLoading = false
                                        navController.navigate("dashboard")
                                    }
                                    ?.addOnFailureListener { e ->
                                        isLoading = false
                                        errorMessage = e.localizedMessage ?: "An error occurred"
                                    }
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

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    MaterialTheme {
        val mockNavController = androidx.navigation.compose.rememberNavController()
        EditProfileScreen(navController = mockNavController)
    }
}
