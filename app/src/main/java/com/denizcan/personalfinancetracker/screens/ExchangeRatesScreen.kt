package com.denizcan.personalfinancetracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.denizcan.personalfinancetracker.network.ExchangeRatesViewModel
import com.denizcan.personalfinancetracker.network.CurrencyViewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavHostController

@Composable
fun ExchangeRatesScreen(
    navController: NavHostController,
    currencyViewModel: CurrencyViewModel = viewModel(),
    exchangeRatesViewModel: ExchangeRatesViewModel = viewModel()
) {
    val baseCurrency by currencyViewModel.baseCurrency.observeAsState("USD")
    val exchangeRates by exchangeRatesViewModel.exchangeRates.observeAsState(emptyMap())
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(baseCurrency) {
        isLoading = true
        exchangeRatesViewModel.fetchExchangeRates(baseCurrency)
        isLoading = false
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
            Text("Exchange Rates", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Base Currency: $baseCurrency", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (exchangeRates.isEmpty()) {
                Text("No exchange rates available.", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn {
                    items(exchangeRates.entries.toList()) { (currency, rate) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(currency, style = MaterialTheme.typography.bodyLarge)
                            Text("%.4f".format(rate), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.popBackStack() }) {
                Text("Go Back")
            }
        }
    }
}
