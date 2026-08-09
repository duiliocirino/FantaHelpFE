package com.example.fantahelpfe.data.mapper

import com.example.fantahelpfe.data.remote.dto.create.TeamPlayerCreateDto
import com.example.fantahelpfe.data.remote.dto.read.TeamPlayerReadDto
import com.example.fantahelpfe.model.TeamPlayer
import com.example.fantahelpfe.model.PlayerRole

fun TeamPlayerReadDto.toDomain(): TeamPlayer {
    return TeamPlayer(
        playerId = this.effectivePlayerId,
        teamId = this.teamId,
        playerName = this.effectivePlayerName,
        auctionPrice = this.effectiveAuctionPrice,
        playerRole = when (this.effectivePlayerRole.uppercase()) {
            "P" -> PlayerRole.P
            "D" -> PlayerRole.D
            "C" -> PlayerRole.C
            "A" -> PlayerRole.A
            else -> PlayerRole.UNKNOWN
        }
    )
}

fun List<TeamPlayerReadDto>.toDomainTeamPlayers(): List<TeamPlayer> = this.map { it.toDomain() }

fun TeamPlayer.toCreateDto(): TeamPlayerCreateDto {
    return TeamPlayerCreateDto(
        playerId = this.playerId,
        purchasePrice = this.auctionPrice
    )
}