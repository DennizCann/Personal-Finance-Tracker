package com.denizcan.personalfinancetracker.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun EditProfileScreen(navController: NavController) {
    // Firebase references
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val storageRef = FirebaseStorage.getInstance().reference

    // State variables
    var name by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("TRY") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val currencyOptions = listOf("TRY", "USD", "EUR", "GBP")

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && uri.toString().startsWith("content://")) {
            selectedImageUri = uri
        } else {
            errorMessage = "Invalid image selected. Please choose a valid file."
        }
    }

    // Load profile data on component initialization
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            errorMessage = "User not authenticated. Please log in again."
            return@LaunchedEffect
        }

        db.collection("profiles").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    name = document.getString("name") ?: ""
                    dateOfBirth = document.getString("dateOfBirth") ?: ""
                    currency = document.getString("currency") ?: "TRY"
                    profileImageUrl = document.getString("profileImageUrl")
                    if (profileImageUrl != null) {
                        selectedImageUri = Uri.parse(profileImageUrl)
                    }
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Failed to load profile: ${e.localizedMessage}"
                Log.e("EditProfileScreen", "Error loading profile: ${e.localizedMessage}")
            }
    }

    // UI Layout
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

            // Profile picture section
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

            // Currency selection
            Text("Select Currency", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(currency)
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
                            val fileRef = storageRef.child("profile_images/$userId.jpg")

                            if (selectedImageUri != null && selectedImageUri.toString().startsWith("content://")) {
                                // Upload the new image
                                fileRef.putFile(selectedImageUri!!)
                                    .addOnSuccessListener {
                                        fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                            saveProfile(
                                                db,
                                                userId,
                                                name,
                                                dateOfBirth,
                                                currency,
                                                downloadUri.toString()
                                            ) {
                                                isLoading = false
                                                navController.navigate("dashboard")
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        errorMessage = "Image upload failed: ${e.localizedMessage}"
                                    }
                            } else {
                                // Use the existing image
                                saveProfile(db, userId, name, dateOfBirth, currency, profileImageUrl) {
                                    isLoading = false
                                    navController.navigate("dashboard")
                                }
                            }
                        } else {
                            errorMessage = "Please fill in all fields."
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

// Save profile data function
fun saveProfile(
    db: FirebaseFirestore,
    userId: String,
    name: String,
    dateOfBirth: String,
    currency: String,
    profileImageUrl: String?,
    onSuccess: () -> Unit
) {
    val userProfile = mapOf(
        "name" to name,
        "dateOfBirth" to dateOfBirth,
        "currency" to currency,
        "profileImageUrl" to (profileImageUrl ?: ""),
        "userId" to userId
    )

    db.collection("profiles").document(userId)
        .set(userProfile)
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener { e ->
            Log.e("EditProfileScreen", "Failed to save profile: ${e.localizedMessage}")
        }
}


@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    val mockNavController = androidx.navigation.compose.rememberNavController()
    EditProfileScreen(navController = mockNavController)
}
