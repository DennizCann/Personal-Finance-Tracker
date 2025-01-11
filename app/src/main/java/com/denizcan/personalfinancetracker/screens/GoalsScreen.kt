package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.denizcan.personalfinancetracker.network.CurrencyViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.Alignment

@Composable
fun GoalsScreen(
    navController: NavController,
    currencyViewModel: CurrencyViewModel = viewModel()
) {
    var goals by remember { mutableStateOf(listOf<Goal>()) }
    var showDeleteDialog by remember { mutableStateOf<Goal?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    // Hedefleri yükle
    LaunchedEffect(currentUser) {
        isLoading = true
        currentUser?.let { user ->
            db.collection("goals")
                .whereEqualTo("userId", user.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        error = e.localizedMessage
                        return@addSnapshotListener
                    }

                    snapshot?.let { documents ->
                        goals = documents.mapNotNull { doc ->
                            Goal(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                                currentAmount = doc.getDouble("currentAmount") ?: 0.0
                            )
                        }

                        // Her hedef için ilerlemeyi kontrol et
                        goals.forEach { goal ->
                            val progress = ((goal.currentAmount / goal.targetAmount) * 100).toInt()
                            currencyViewModel.updateGoalProgress(goal.name, progress)
                        }
                    }
                }
        }
        isLoading = false
    }

    fun deleteGoal(goal: Goal) {
        db.collection("goals")
            .document(goal.id)
            .delete()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addGoal") }
            ) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Finansal Hedeflerim",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (goals.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Henüz hedef eklenmemiş",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { navController.navigate("addGoal") }) {
                                Text("Hedef Ekle")
                            }
                        }
                    }
                } else {
                    LazyColumn {
                        items(goals) { goal ->
                            GoalCard(
                                goal = goal,
                                onEdit = {
                                    navController.navigate("editGoal/${goal.id}")
                                },
                                onDelete = {
                                    showDeleteDialog = goal
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Silme onay dialogu
    showDeleteDialog?.let { goal ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Hedefi Sil") },
            text = { Text("'${goal.name}' hedefini silmek istediğinizden emin misiniz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteGoal(goal)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // Hata mesajı gösterimi
    error?.let { message ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { error = null }) {
                    Text("Tamam")
                }
            }
        ) {
            Text(message)
        }
    }
}

@Composable
fun GoalCard(
    goal: Goal,
    onEdit: (Goal) -> Unit,
    onDelete: (Goal) -> Unit
) {
    val progress = ((goal.currentAmount / goal.targetAmount) * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(goal.name, style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = { onEdit(goal) }) {
                        Icon(Icons.Default.Edit, "Düzenle")
                    }
                    IconButton(onClick = { onDelete(goal) }) {
                        Icon(Icons.Default.Delete, "Sil")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "İlerleme: %$progress (${goal.currentAmount} TRY / ${goal.targetAmount} TRY)",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

data class Goal(
    val id: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double
)