package com.example.fantahelpfe.data

import android.util.Log
import com.example.fantahelpfe.data.mapper.toDomain
import com.example.fantahelpfe.data.mapper.toDomainLeagues
import com.example.fantahelpfe.data.mapper.toDomainPlayers
import com.example.fantahelpfe.data.remote.ApiService
import com.example.fantahelpfe.data.remote.dto.SuggestionRequest
import com.example.fantahelpfe.data.remote.dto.SuggestionResult
import com.example.fantahelpfe.data.remote.dto.create.LeagueCreateDto
import com.example.fantahelpfe.data.remote.dto.create.TeamCreateDto
import com.example.fantahelpfe.data.remote.dto.create.TeamPlayerCreateDto
import com.example.fantahelpfe.model.League
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.model.Team
import com.example.fantahelpfe.model.TeamPlayer
import javax.inject.Inject

// This is the concrete implementation. It defines HOW the repository does its job.
// @Inject tells Hilt how to create an instance of this class.
class DataRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : DataRepository {

    // A simple in-memory cache for the player list.
    // 'private set' means only this class can change the list.
    private var playerCache: List<Player>? = null
    private var leaguesCache: List<League>? = null

    override suspend fun getAllPlayers(): List<Player> {
        // If the cache is not empty, return the cached data immediately.
        if (playerCache != null) {
            return playerCache!!
        }

        // If the cache is empty, fetch from the API, store it, and then return it.
        val playersFromApi = apiService.getAllPlayers()
        val players = playersFromApi.toDomainPlayers()
        playerCache = players
        return players
    }

    override suspend fun getAllLeagues(): List<League> {
        if (leaguesCache != null) {
            return leaguesCache!!
        }

        val leaguesFromApi = apiService.getAllLeagues()
        val leagues = leaguesFromApi.toDomainLeagues()
        leaguesCache = leagues
        return leagues
    }

    override suspend fun getLeagueById(leagueId: Int): League {
        return apiService.getLeagueById(leagueId).toDomain()
    }

    override suspend fun getTeamsForLeague(leagueId: Int): List<Team> {
        Log.d("RepoDebug", "getTeamsForLeague called with leagueId: $leagueId")
        val league = apiService.getLeagueById(leagueId).toDomain()
        Log.d("RepoDebug", "League domain object: $league")
        Log.d("RepoDebug", "Teams in domain object for league ${league.id}: ${league.teams.size} teams. Teams: ${league.teams}")
        return league.teams
    }

    override suspend fun createLeague(league: LeagueCreateDto): League {
        val league = apiService.createLeague(league).toDomain()
        return league
    }

    override suspend fun addLeagueToTeam(leagueId: Int, teamId: Int): Team {
        val team = apiService.addTeamToLeague(leagueId, teamId).toDomain()
        return team
    }

    override suspend fun createTeam(team: TeamCreateDto): Team {
        val team = apiService.createTeam(team).toDomain()
        return team
    }

    override suspend fun getAvailablePlayersForLeague(leagueId: Int): List<Player> {
        val players = apiService.getAvailablePlayersForLeague(leagueId).toDomainPlayers()
        return players
    }

    override suspend fun searchPlayer(currentLeagueId: Int, query: String): List<Player> {
        if (playerCache == null) {
            playerCache = getAllPlayers()
        }
        return playerCache!!.filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun assignPlayerToTeam(teamId: Int, playerId: Int, bid: Int): Team {
        val team = apiService.addPlayerToTeam(teamId, TeamPlayerCreateDto(playerId, bid)).toDomain()
        return team
    }

    override suspend fun getSuggestion(potentialRequest: SuggestionRequest): List<SuggestionResult> {
        val suggestionResult = apiService.getSuggestion(potentialRequest)
        return suggestionResult
    }

    override suspend fun getPlayersForTeam(teamId: Int): List<TeamPlayer> {
        val team = apiService.getTeam(teamId).toDomain()
        val players = team.players
        return players
    }
}