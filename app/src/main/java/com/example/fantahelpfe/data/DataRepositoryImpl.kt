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
    private val availablePlayersCache = mutableMapOf<Int, List<Player>>()

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
        val result = safeApiCall { apiService.removePlayerFromTeam(teamId, playerId).toDomain() }
        clearCache()
        return result
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

    override suspend fun addLeagueToTeam(leagueId: Int, teamId: Int): League {
        return safeApiCall { apiService.addTeamToLeague(leagueId, teamId).toDomain() }
    }

    override suspend fun createTeam(team: TeamCreateDto): Team {
        return safeApiCall { apiService.createTeam(team).toDomain() }
    }

    override suspend fun getAvailablePlayersForLeague(leagueId: Int): List<Player> {
        return safeApiCall { apiService.getAvailablePlayersForLeague(leagueId).toDomainPlayers() }
    }

    override suspend fun searchPlayer(currentLeagueId: Int, query: String): List<Player> {
        val availablePlayers = availablePlayersCache.getOrPut(currentLeagueId) {
            getAvailablePlayersForLeague(currentLeagueId)
        }
        return availablePlayers.filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun assignPlayerToTeam(teamId: Int, playerId: Int, bid: Int): Team {
        val result = safeApiCall { apiService.addPlayerToTeam(teamId, TeamPlayerCreateDto(playerId, bid)).toDomain() }
        clearCache()
        return result
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

    override fun clearCache() {
        playerCache = null
        leaguesCache = null
        availablePlayersCache.clear()
    }

    private suspend fun <T> safeApiCall(call: suspend () -> T): T {
        return try {
            call()
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("DataRepository", "API Error ${e.code()}: $errorBody")
            throw when (e.code()) {
                404 -> AppError.NotFoundError(cause = e)
                401, 403 -> AppError.AuthorizationError(cause = e)
                else -> AppError.ApiError(
                    statusCode = e.code(),
                    apiErrorMessage = errorBody ?: e.message(),
                    cause = e
                )
            }
        } catch (e: IOException) {
            Log.e("DataRepository", "Network error: ${e.message}", e)
            throw AppError.NetworkError(cause = e)
        } catch (e: AppError) {
            throw e
        } catch (e: Exception) {
            Log.e("DataRepository", "Unexpected error: ${e.message}", e)
            throw AppError.UnknownError(cause = e)
        }
    }

}