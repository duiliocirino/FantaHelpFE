package com.example.fantahelpfe.data.remote.dto.read

import com.squareup.moshi.Json

data class LeagueReadDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "initialBudget") val initialBudget: Int,
    @Json(name = "teams") val teams: List<TeamReadDto>
)