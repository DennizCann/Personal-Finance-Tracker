package com.denizcan.personalfinancetracker.network

data class ExchangeRatesResponse(
    val result: String,
    val base_code: String,
    val conversion_rates: Map<String, Double>
)
