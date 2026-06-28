package com.example.fantahelpfe.data.remote.dto.read

import com.squareup.moshi.Json

data class TeamReadDto(
    @Json(name = "id") val id: Int,
    @Json(name = "leagueId") val leagueId: Int,
    @Json(name = "name") val name: String,
    @Json(name = "remainingBudget") val remainingBudget: Int,
    @Json(name = "players") val players: List<TeamPlayerReadDto>
)