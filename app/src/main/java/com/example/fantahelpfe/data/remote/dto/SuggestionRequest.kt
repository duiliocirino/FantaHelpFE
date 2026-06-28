package com.example.fantahelpfe.data.remote.dto

import com.squareup.moshi.Json

data class SuggestionRequest(
    @Json(name = "teamId") val teamId: Int,
    @Json(name = "numTeams") val numTeams: Int = 1,
    @Json(name = "favoritePlayerIds") val favoritePlayerIds: List<Int> = emptyList(),
    @Json(name = "lineUp") val lineUp: LineUp,
    @Json(name = "creditsDistribution") val creditsDistribution: Int = 1,
    @Json(name = "auctionedPlayer") val auctionedPlayer: AuctionedPlayerInfo? = null // THE NEW FIELD
    // Add other future options as needed
)