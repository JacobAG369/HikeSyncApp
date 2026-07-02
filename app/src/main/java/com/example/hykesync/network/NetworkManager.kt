package com.example.hykesync.network

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class NetworkManager {

    /**
     * Envía datos de telemetría al servidor de forma asíncrona usando OkHttp.
     */
    fun enviarDatosSincronizacion() {
        // 1. Crear el val client = OkHttpClient()
        val client = OkHttpClient()

        // Definir la URL destino
        val url = "https://hikesync-api.onrender.com/api/sync"

        // Construir un String en formato JSON de prueba
        val jsonPrueba = """
            {
                "session_id": "hiking_2026_06_23",
                "telemetry": {
                    "heart_rate": 75,
                    "steps": 1200,
                    "altitude": 450.5
                },
                "location": {
                    "latitude": 40.4168,
                    "longitude": -3.7038,
                    "timestamp": 1719150000
                }
            }
        """.trimIndent()

        // 2. Definir el MediaType para JSON
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

        // 3. Crear el RequestBody con el string del JSON
        val body = jsonPrueba.toRequestBody(mediaType)

        // 4. Construir el Request con .url(url), .post(body) y .build()
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        // 5. Ejecutar asíncronamente con client.newCall(request).enqueue(...)
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Manejar error imprimiendo Log.d("FETCH", ...)
                Log.d("FETCH", "Error en la petición: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                // Manejar éxito imprimiendo Log.d("FETCH", ...)
                response.use {
                    if (response.isSuccessful) {
                        Log.d("FETCH", "Sincronización exitosa: ${response.code}")
                    } else {
                        Log.d("FETCH", "Error del servidor: ${response.code} ${response.message}")
                    }
                }
            }
        })
    }
}
