package com.example.fantahelpfe.data.mapper

import com.example.fantahelpfe.data.remote.dto.read.TeamReadDto
import com.example.fantahelpfe.model.Team

fun TeamReadDto.toDomain(): Team {
    return Team(
        id = this.id,
        name = this.name,
        leagueId = this.leagueId,
        remainingBudget = this.remainingBudget,
        players = this.players.map { it.toDomain() }
    )
}

fun List<TeamReadDto>.toDomainTeams(): List<Team> = this.map { it.toDomain() }
