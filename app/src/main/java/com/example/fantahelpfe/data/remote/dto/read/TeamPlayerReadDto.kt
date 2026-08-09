package com.example.fantahelpfe.data.remote.dto.read

import com.squareup.moshi.Json

data class TeamPlayerReadDto(
    @Json(name = "playerId") val playerId: Int = 0,
    @Json(name = "id") val altPlayerId: Int = 0,
    @Json(name = "teamId") val teamId: Int = 0,
    @Json(name = "playerName") val playerName: String? = null,
    @Json(name = "name") val altPlayerName: String? = null,
    @Json(name = "auctionPrice") val auctionPrice: Int = 0,
    @Json(name = "purchasePrice") val purchasePrice: Int = 0,
    @Json(name = "price") val altPrice: Int = 0,
    @Json(name = "playerRole") val playerRole: String? = null,
    @Json(name = "role") val altRole: String? = null,
    @Json(name = "player") val player: PlayerReadDto? = null
) {
    val effectivePlayerId: Int
        get() = if (playerId != 0) playerId else if (altPlayerId != 0) altPlayerId else player?.id ?: 0

    val effectivePlayerName: String
        get() = playerName ?: altPlayerName ?: player?.name ?: "Unknown Player"

    val effectiveAuctionPrice: Int
        get() = if (auctionPrice != 0) auctionPrice else if (purchasePrice != 0) purchasePrice else altPrice

    val effectivePlayerRole: String
        get() = playerRole ?: altRole ?: player?.role ?: ""
}