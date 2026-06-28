package com.example.fantahelpfe.data.remote

import com.example.fantahelpfe.data.remote.dto.SuggestionRequest
import com.example.fantahelpfe.data.remote.dto.SuggestionResult
import com.example.fantahelpfe.data.remote.dto.create.LeagueCreateDto
import com.example.fantahelpfe.data.remote.dto.create.TeamCreateDto
import com.example.fantahelpfe.data.remote.dto.create.TeamPlayerCreateDto
import com.example.fantahelpfe.data.remote.dto.read.LeagueReadDto
import com.example.fantahelpfe.data.remote.dto.read.PlayerReadDto
import com.example.fantahelpfe.data.remote.dto.read.TeamReadDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @GET("api/players")
    suspend fun getAllPlayers(): List<PlayerReadDto>

    @GET("api/players/{id}")
    suspend fun getPlayerById(@Path("id") playerId: Int): PlayerReadDto

    @GET("api/leagues")
    suspend fun getAllLeagues(): List<LeagueReadDto>

    @GET("api/leagues/{id}")
    suspend fun getLeagueById(@Path("id") leagueId: Int): LeagueReadDto

    @POST("api/leagues")
    suspend fun createLeague(@Body league: LeagueCreateDto): LeagueReadDto

    @POST("api/leagues/{id}/teams/{teamId}")
    suspend fun addTeamToLeague(@Path("id") leagueId: Int, @Path("teamId") teamId: Int): TeamReadDto

    @POST("api/teams")
    suspend fun createTeam(@Body team: TeamCreateDto): TeamReadDto

    @GET("api/leagues/{id}/players")
    suspend fun getAvailablePlayersForLeague(@Path("id") leagueId: Int): List<PlayerReadDto>

    @POST("api/teams/{id}/players")
    suspend fun addPlayerToTeam(@Path("id") teamId: Int, @Body body: TeamPlayerCreateDto): TeamReadDto

    @POST("api/teams/getOptimal")
    suspend fun getSuggestion(@Body potentialRequest: SuggestionRequest): List<SuggestionResult>

    @GET("api/teams/{id}")
    suspend fun getTeam(@Path("id") teamId: Int): TeamReadDto

    // You will add all your other endpoints here later
    // For example:
    // @POST("api/teams/{id}/players")
    // suspend fun addPlayerToTeam(@Path("id") teamId: Int, @Body body: AddPlayerRequestDto): TeamDto
}