package com.example.fantahelpfe.data.remote.dto.read

import com.squareup.moshi.Json

data class PlayerReadDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "squad") val squad: String,
    @Json(name = "role") val role: String,
    @Json(name = "price") val price: Int,
    @Json(name = "rating") val rating: Double,
    @Json(name = "regularness") val regularness: Int,
    @Json(name = "fvm") val fvm: Int,
    @Json(name = "expectedPerformance") val expectedPerformance: Double,
    @Json(name = "expectedStd") val expectedStd: Double,
    @Json(name = "expectedPrice") val expectedPrice: Int
)