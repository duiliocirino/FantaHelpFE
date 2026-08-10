package com.example.fantahelpfe.data.remote.dto

import com.squareup.moshi.Json

data class BudgetAllocation(
    @Json(name = "goalkeepers") val goalkeepers: Double = 0.1,
    @Json(name = "defenders") val defenders: Double = 0.3,
    @Json(name = "midfielders") val midfielders: Double = 0.6,
    @Json(name = "attackers") val attackers: Double = 0.6
)
