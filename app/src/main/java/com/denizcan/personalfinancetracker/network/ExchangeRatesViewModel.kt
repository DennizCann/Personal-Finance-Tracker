package com.denizcan.personalfinancetracker.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Response

class ExchangeRatesViewModel : ViewModel() {

    private val api = RetrofitClient.api

    private val _exchangeRates = MutableLiveData<Map<String, Double>>()
    val exchangeRates: LiveData<Map<String, Double>> get() = _exchangeRates

    fun fetchExchangeRates(baseCurrency: String) {
        viewModelScope.launch {
            try {
                val response: Response<ExchangeRatesResponse> = api.getRates(baseCurrency)
                if (response.isSuccessful && response.body()?.result == "success") {
                    _exchangeRates.value = response.body()?.conversion_rates ?: emptyMap()
                } else {
                    println("API Error: ${response.errorBody()?.string()}")
                    _exchangeRates.value = emptyMap()
                }
            } catch (e: Exception) {
                println("Error fetching exchange rates: ${e.message}")
                _exchangeRates.value = emptyMap()
            }
        }
    }
}
