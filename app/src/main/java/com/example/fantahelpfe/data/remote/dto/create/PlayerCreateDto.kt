package com.example.fantahelpfe.data.remote.dto.create

import com.squareup.moshi.Json

data class PlayerCreateDto(
    @Json(name = "id") val id: Int,
    @Json(name = "role") val role: String,
    @Json(name = "name") val name: String,
    @Json(name = "squad") val squad: String,
    @Json(name = "price") val price: Int,
    @Json(name = "myRating") val myRating: Float,
    @Json(name = "mate") val mate: String,
    @Json(name = "regularness") val regularness: Int,
    @Json(name = "fvm") val fvm: Int,
    @Json(name = "expMf") val expMf: Float,
    @Json(name = "expPrice") val expPrice: Int,
    @Json(name = "expStd") val expStd: Int
)