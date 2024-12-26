package com.denizcan.personalfinancetracker.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRatesApi {
    @GET("latest/{base}")
    suspend fun getRates(
        @Path("base") baseCurrency: String
    ): Response<ExchangeRatesResponse>
}
