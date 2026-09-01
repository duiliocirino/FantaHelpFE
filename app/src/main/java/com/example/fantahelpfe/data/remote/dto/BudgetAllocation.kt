package com.example.fantahelpfe.data.remote.dto

import com.squareup.moshi.Json

data class BudgetAllocation(
    @Json(name = "goalkeepers") val goalkeepers: Double = 0.08,
    @Json(name = "defenders") val defenders: Double = 0.38,
    @Json(name = "midfielders") val midfielders: Double = 0.55,
    @Json(name = "attackers") val attackers: Double = 0.70
)
