package com.example.fantahelpfe.data

import com.example.fantahelpfe.data.remote.dto.BudgetAllocation
import com.example.fantahelpfe.data.remote.dto.LineUp
import com.example.fantahelpfe.data.remote.dto.StrategyWeights

data class AuctionSettings(
    val globalFavoritePlayerIds: List<Int> = emptyList(),
    val creditsDistributionMultiplier: Int = 3,
    val budgetAllocation: BudgetAllocation = BudgetAllocation(),
    val lineUp: LineUp = LineUp(defenders = 4, midfielders = 3, attackers = 3),
    val strategyWeights: StrategyWeights = StrategyWeights(),
    val selectedPresetName: String = "Balanced"
)
