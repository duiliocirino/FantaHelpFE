package com.example.fantahelpfe.data.remote.dto

import com.squareup.moshi.Json

data class Score(
    @Json(name = "starterScore") val starterScore: Double,
    @Json(name = "benchScore") val benchScore: Double,
    @Json(name = "penaltyScore") val penaltyScore: Double,
    @Json(name = "totalScore") val totalScore: Double
)