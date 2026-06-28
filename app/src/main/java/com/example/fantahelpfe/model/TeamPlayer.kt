package com.example.fantahelpfe.model

import com.squareup.moshi.Json

data class TeamPlayer(
    val playerId: Int,
    var teamId: Int,
    val playerName: String,
    val auctionPrice: Int
)