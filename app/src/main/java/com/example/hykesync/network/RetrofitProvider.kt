package com.example.hykesync.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {
    private const val DEFAULT_BASE_URL = "https://hikesync-api.onrender.com/"

    fun createApi(baseUrl: String = DEFAULT_BASE_URL): HikeSyncApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HikeSyncApi::class.java)
    }
}
