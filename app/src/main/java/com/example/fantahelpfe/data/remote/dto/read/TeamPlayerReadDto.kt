package com.example.fantahelpfe.data.remote.dto.read

import com.squareup.moshi.Json

data class TeamPlayerReadDto(
    @Json(name = "playerId") val playerId: Int,
    @Json(name = "teamId") val teamId: Int,
    @Json(name = "playerName") val playerName: String,
    @Json(name = "auctionPrice") val auctionPrice: Int
)