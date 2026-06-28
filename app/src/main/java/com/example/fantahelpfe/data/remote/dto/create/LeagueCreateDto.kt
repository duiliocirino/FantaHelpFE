package com.example.fantahelpfe.data.remote.dto.create

import com.squareup.moshi.Json

data class LeagueCreateDto(
    @Json(name = "name") val name: String,
    @Json(name = "initialBudget") val initialBudget: Int = 800
)