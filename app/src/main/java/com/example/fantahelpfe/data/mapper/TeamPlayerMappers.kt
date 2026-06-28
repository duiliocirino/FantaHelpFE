package com.example.fantahelpfe.data.mapper

import com.example.fantahelpfe.data.remote.dto.create.TeamPlayerCreateDto
import com.example.fantahelpfe.data.remote.dto.read.TeamPlayerReadDto
import com.example.fantahelpfe.model.TeamPlayer

fun TeamPlayerReadDto.toDomain(): TeamPlayer {
    return TeamPlayer(
        playerId = this.playerId,
        teamId = this.teamId,
        playerName = this.playerName,
        auctionPrice = this.auctionPrice
    )
}

fun List<TeamPlayerReadDto>.toDomainTeamPlayers(): List<TeamPlayer> = this.map { it.toDomain() }

fun TeamPlayer.toCreateDto(): TeamPlayerCreateDto {
    return TeamPlayerCreateDto(
        playerId = this.playerId,
        purchasePrice = this.auctionPrice
    )
}