package com.example.guardiancare.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Make BASE_URL mutable so it can be changed at runtime
    private var _baseUrl = "http://172.20.10.14:8000/"
//    private var _baseUrl = "http://10.0.2.2:8000/"
    
    // Getter for BASE_URL
    val baseUrl: String
        get() = _baseUrl
    
    // Method to update the base URL
    fun updateBaseUrl(newUrl: String) {
        _baseUrl = newUrl
        // Reset the retrofit instance to use the new URL
        _retrofit = null
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Make retrofit nullable and lazy initialized
    private var _retrofit: Retrofit? = null
    
    // Getter for retrofit that initializes it if needed
    private val retrofit: Retrofit
        get() {
            return _retrofit ?: Retrofit.Builder()
                .baseUrl(_baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build().also { _retrofit = it }
        }

    // ✅ Add this function to create any Retrofit service
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}
