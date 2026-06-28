package com.example.fantahelpfe.data.remote.dto.create

import com.squareup.moshi.Json

data class TeamCreateDto(
    @Json(name = "name") val name: String,
    @Json(name = "initialBudget") val initialBudget: Int,
    @Json(name = "ownerId") val ownerId: Int,
    @Json(name = "leagueId") val leagueId: Int
)