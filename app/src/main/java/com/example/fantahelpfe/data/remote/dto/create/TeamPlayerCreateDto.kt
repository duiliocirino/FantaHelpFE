package com.example.fantahelpfe.data.remote.dto.create

import com.squareup.moshi.Json

data class TeamPlayerCreateDto(
    @Json(name = "playerId") val playerId: Int,
    @Json(name = "purchasePrice") val purchasePrice: Int
)