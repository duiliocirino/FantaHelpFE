package com.example.fantahelpfe.data.remote.dto

import com.squareup.moshi.Json

data class SuggestionRequest(
    @Json(name = "teamId") val teamId: Int,
    @Json(name = "numTeams") val numTeams: Int = 1,
    @Json(name = "favoritePlayerIds") val favoritePlayerIds: List<Int> = emptyList(),
    @Json(name = "lineUp") val lineUp: LineUp,
    @Json(name = "creditsDistribution") val creditsDistribution: Int = 1,
    @Json(name = "auctionedPlayer") val auctionedPlayer: AuctionedPlayerInfo? = null,
    @Json(name = "budgetAllocation") val budgetAllocation: BudgetAllocation? = null,
    @Json(name = "weights") val weights: StrategyWeights? = null
)