package com.example.fantahelpfe.data.remote.dto

import com.example.fantahelpfe.data.remote.dto.read.PlayerReadDto
import com.squareup.moshi.Json

data class SuggestionResult(
    @Json(name = "suggestedPlayers") val suggestedPlayers: List<PlayerReadDto>,
    @Json(name = "totalExpectedPrice") val totalExpectedPrice: Int,
    @Json(name = "totalExpectedPriceStd") val totalExpectedPriceStd: Int,
    @Json(name = "score") val score: Score
)