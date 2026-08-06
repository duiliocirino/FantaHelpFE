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
import com.example.fantahelpfe.utils.AppError
import retrofit2.HttpException
import java.io.IOException
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
        if (playerCache != null) {
            return playerCache!!
        }

        return safeApiCall {
            val playersFromApi = apiService.getAllPlayers()
            val players = playersFromApi.toDomainPlayers()
            playerCache = players
            players
        }
    }

    override suspend fun getAllLeagues(): List<League> {
        if (leaguesCache != null) {
            return leaguesCache!!
        }

        return safeApiCall {
            val leaguesFromApi = apiService.getAllLeagues()
            val leagues = leaguesFromApi.toDomainLeagues()
            leaguesCache = leagues
            leagues
        }
    }

    override suspend fun getLeagueById(leagueId: Int): League {
        return safeApiCall { apiService.getLeagueById(leagueId).toDomain() }
    }

    override suspend fun getTeamsForLeague(leagueId: Int): List<Team> {
        return getTeamsFromLeague(leagueId)
    }

    override suspend fun getTeamsFromLeague(leagueId: Int): List<Team> {
        return safeApiCall { apiService.getTeamsFromLeague(leagueId).map { it.toDomain() } }
    }

    override suspend fun removePlayerFromTeam(teamId: Int, playerId: Int): Team {
        return safeApiCall { apiService.removePlayerFromTeam(teamId, playerId).toDomain() }
    }

    override suspend fun deleteTeam(teamId: Int) {
        safeApiCall { apiService.deleteTeam(teamId) }
    }

    override suspend fun deleteLeague(leagueId: Int) {
        safeApiCall { apiService.deleteLeague(leagueId) }
    }

    override suspend fun createLeague(league: LeagueCreateDto): League {
        return safeApiCall { apiService.createLeague(league).toDomain() }
    }

    override suspend fun addLeagueToTeam(leagueId: Int, teamId: Int): Team {
        return safeApiCall { apiService.addTeamToLeague(leagueId, teamId).toDomain() }
    }

    override suspend fun createTeam(team: TeamCreateDto): Team {
        return safeApiCall { apiService.createTeam(team).toDomain() }
    }

    override suspend fun getAvailablePlayersForLeague(leagueId: Int): List<Player> {
        return safeApiCall { apiService.getAvailablePlayersForLeague(leagueId).toDomainPlayers() }
    }

    override suspend fun searchPlayer(currentLeagueId: Int, query: String): List<Player> {
        if (playerCache == null) {
            playerCache = getAllPlayers()
        }
        return playerCache!!.filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun assignPlayerToTeam(teamId: Int, playerId: Int, bid: Int): Team {
        return safeApiCall { apiService.addPlayerToTeam(teamId, TeamPlayerCreateDto(playerId, bid)).toDomain() }
    }

    override suspend fun getSuggestion(potentialRequest: SuggestionRequest): List<SuggestionResult> {
        return safeApiCall { apiService.getSuggestion(potentialRequest) }
    }

    override suspend fun getPlayersForTeam(teamId: Int): List<TeamPlayer> {
        return safeApiCall {
            val team = apiService.getTeam(teamId).toDomain()
            team.players
        }
    }

    private suspend fun <T> safeApiCall(call: suspend () -> T): T {
        return try {
            call()
        } catch (e: HttpException) {
            throw when (e.code()) {
                404 -> AppError.NotFoundError(cause = e)
                401, 403 -> AppError.AuthorizationError(cause = e)
                else -> AppError.ApiError(statusCode = e.code(), apiErrorMessage = e.message(), cause = e)
            }
        } catch (e: IOException) {
            throw AppError.NetworkError(cause = e)
        } catch (e: AppError) {
            throw e
        } catch (e: Exception) {
            throw AppError.UnknownError(cause = e)
        }
    }
}