package com.example.fantahelpfe.data

import com.example.fantahelpfe.data.remote.dto.LineUp

data class AuctionSettings(
    val globalFavoritePlayerIds: List<Int> = emptyList(),
    val creditsDistributionMultiplier: Int = 1,
    val lineUp: LineUp = LineUp(defenders = 4, midfielders = 3, attackers = 3)
)
