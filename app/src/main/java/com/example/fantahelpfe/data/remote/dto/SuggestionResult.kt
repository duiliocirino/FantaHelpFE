package com.example.fantahelpfe.data.remote.dto

import com.example.fantahelpfe.data.remote.dto.read.PlayerReadDto
import com.squareup.moshi.Json

data class SuggestionResult(
    @Json(name = "suggestedPlayers") val suggestedPlayers: List<PlayerReadDto> = emptyList(),
    @Json(name = "totalExpectedPrice") val totalExpectedPrice: Int = 0,
    @Json(name = "totalExpectedPriceStd") val totalExpectedPriceStd: Int = 0,
    @Json(name = "score") val score: Score,
    @Json(name = "potentialScore") val potentialScore: SuggestionResult? = null,
    @Json(name = "withoutPlayerScore") val withoutPlayerScore: SuggestionResult? = null
)