package com.example.fantahelpfe.data.remote.dto.read

import com.squareup.moshi.Json

data class PlayerReadDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "squad") val squad: String,
    @Json(name = "role") val role: String,
    @Json(name = "price") val price: Int,
    @Json(name = "rating") val rating: Double = 0.0,
    @Json(name = "regularness") val regularness: Double = 0.0,
    @Json(name = "regularity") val regularity: Double? = null,
    @Json(name = "fvm") val fvm: Int = 0,
    @Json(name = "expectedPerformance") val expectedPerformance: Double = 0.0,
    @Json(name = "expectedStd") val expectedStd: Double = 0.0,
    @Json(name = "expectedPrice") val expectedPrice: Int = 0
)