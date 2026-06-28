package com.example.fantahelpfe.data.remote.dto

import com.squareup.moshi.Json

data class AuctionedPlayerInfo(
    @Json(name = "playerId") val playerId: Int,
    @Json(name = "acquisitionPrice") val acquisitionPrice: Int
)