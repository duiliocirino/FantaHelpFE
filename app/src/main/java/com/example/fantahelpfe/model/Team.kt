package com.example.fantahelpfe.model

import com.example.fantahelpfe.data.remote.dto.read.TeamPlayerReadDto
import com.squareup.moshi.Json

data class Team(
    val id: Int,
    val leagueId: Int,
    val name: String,
    val remainingBudget: Int,
    val players: List<TeamPlayer>
)