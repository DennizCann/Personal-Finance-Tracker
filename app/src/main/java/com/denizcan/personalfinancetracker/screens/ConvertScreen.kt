package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denizcan.personalfinancetracker.network.ExchangeRatesViewModel

@Composable
fun ConvertScreen(
    navController: NavController,
    exchangeRatesViewModel: ExchangeRatesViewModel = viewModel()
) {
    val exchangeRates by exchangeRatesViewModel.exchangeRates.observeAsState(emptyMap())
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("EUR") }
    var amount by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(Unit) {
        if (exchangeRates.isEmpty()) {
            exchangeRatesViewModel.fetchExchangeRates(fromCurrency)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Currency Converter", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // From Currency Dropdown
        DropdownSelector(
            label = "From Currency",
            options = exchangeRates.keys.toList(),
            selectedOption = fromCurrency,
            onOptionSelected = {
                fromCurrency = it
                exchangeRatesViewModel.fetchExchangeRates(fromCurrency)
            }
        )

        // To Currency Dropdown
        DropdownSelector(
            label = "To Currency",
            options = exchangeRates.keys.toList(),
            selectedOption = toCurrency,
            onOptionSelected = { toCurrency = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Amount Input
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Convert Button
        Button(
            onClick = {
                val amountValue = amount.toDoubleOrNull()
                val rate = exchangeRates[toCurrency]
                if (amountValue != null && rate != null) {
                    result = amountValue * rate
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Convert")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result Display
        if (result != null) {
            Text(
                text = "Converted Amount: ${"%.2f".format(result)} $toCurrency",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedOption)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
