package com.example.hykesync.network

import com.example.hykesync.network.dto.SyncRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface HikeSyncApi {
    @POST("/api/sync")
    suspend fun syncData(@Body request: SyncRequest): Response<Unit>
}
