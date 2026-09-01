package com.example.fantahelpfe.data.remote.dto

import com.squareup.moshi.Json

data class StrategyWeights(
    @Json(name = "starterWeight") val starterWeight: Int = 6,
    @Json(name = "benchWeight") val benchWeight: Int = 3,
    @Json(name = "strategyWeight") val strategyWeight: Int = 1,
    @Json(name = "squadDiversity") val squadDiversity: Int = 2,
    @Json(name = "mateWeight") val mateWeight: Int = 1,
    @Json(name = "reliabilityWeight") val reliabilityWeight: Int = 10
)
