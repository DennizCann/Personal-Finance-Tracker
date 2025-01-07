package com.denizcan.personalfinancetracker.network

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CurrencyViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private val _baseCurrency = MutableLiveData<String>("USD")
    val baseCurrency: LiveData<String> get() = _baseCurrency

    init {
        fetchBaseCurrency()
    }

    private fun fetchBaseCurrency() {
        currentUser?.let { user ->
            db.collection("profiles").document(user.uid)
                .addSnapshotListener { document, exception ->
                    if (exception != null) {
                        println("Error fetching base currency: ${exception.localizedMessage}")
                        return@addSnapshotListener
                    }

                    if (document != null && document.exists()) {
                        _baseCurrency.value = document.getString("currency") ?: "USD"
                    } else {
                        println("Base currency not found. Using default: USD")
                    }
                }
        }
    }

    // Test veya Preview amaçlı set metodu
    @VisibleForTesting
    fun setBaseCurrencyForTest(value: String) {
        _baseCurrency.value = value
    }
}
