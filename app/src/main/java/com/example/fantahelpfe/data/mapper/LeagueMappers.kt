package com.example.fantahelpfe.data.mapper

import com.example.fantahelpfe.data.remote.dto.create.LeagueCreateDto
import com.example.fantahelpfe.data.remote.dto.read.LeagueReadDto
import com.example.fantahelpfe.model.League

fun LeagueReadDto.toDomain(): League {
    return League(
        id = this.id,
        name = this.name,
        initialBudget = this.initialBudget,
        teams = this.teams.map { it.toDomain() },
        goalBonusPerRole = this.goalBonusPerRole.mapKeys { (key, _) ->
            when (key.uppercase()) {
                "P" -> PlayerRole.P
                "D" -> PlayerRole.D
                "C" -> PlayerRole.C
                "A" -> PlayerRole.A
                else -> PlayerRole.UNKNOWN
            }
        }
    )
}

fun List<LeagueReadDto>.toDomainLeagues(): List<League> = this.map { it.toDomain() }

fun League.toCreateDto(): LeagueCreateDto {
    return LeagueCreateDto(
        name = this.name,
        initialBudget = this.initialBudget
    )
}