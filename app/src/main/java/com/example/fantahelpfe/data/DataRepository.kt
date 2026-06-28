package com.example.fantahelpfe.data

import com.example.fantahelpfe.data.remote.dto.SuggestionRequest
import com.example.fantahelpfe.data.remote.dto.SuggestionResult
import com.example.fantahelpfe.data.remote.dto.create.LeagueCreateDto
import com.example.fantahelpfe.data.remote.dto.create.TeamCreateDto
import com.example.fantahelpfe.model.League
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.model.Team
import com.example.fantahelpfe.model.TeamPlayer

interface DataRepository {
    suspend fun getAllPlayers(): List<Player>
    suspend fun getAllLeagues(): List<League>
    suspend fun getLeagueById(leagueId: Int): League
    suspend fun getTeamsForLeague(leagueId: Int): List<Team>
    suspend fun createLeague(league: LeagueCreateDto): League
    suspend fun createTeam(team: TeamCreateDto): Team
    suspend fun addLeagueToTeam(leagueId: Int, teamId: Int): Team
    suspend fun getAvailablePlayersForLeague(leagueId: Int): List<Player>
    suspend fun searchPlayer(currentLeagueId: Int, query: String): List<Player>
    suspend fun assignPlayerToTeam(teamId: Int, playerId: Int, bid: Int): Team
    suspend fun getSuggestion(potentialRequest: SuggestionRequest): List<SuggestionResult>
    suspend fun getPlayersForTeam(teamId: Int): List<TeamPlayer>

}