package com.example.fantahelpfe.ui.auction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.data.AuctionSessionManager
import com.example.fantahelpfe.data.DataRepository
import com.example.fantahelpfe.data.UserPreferencesRepository
import com.example.fantahelpfe.data.remote.dto.SuggestionRequest
import com.example.fantahelpfe.data.remote.dto.SuggestionResult
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.model.Team
import com.example.fantahelpfe.model.TeamPlayer
import com.example.fantahelpfe.utils.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuctionMyTeamUiState(
    val teams: List<Team> = emptyList(),
    val selectedTeam: Team? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val suggestionResults: List<SuggestionResult>? = null,
    val isLoadingSuggestion: Boolean = false
)

@HiltViewModel
class AuctionMyTeamViewModel @Inject constructor(
    private val repository: DataRepository,
    private val auctionSessionManager: AuctionSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuctionMyTeamUiState())
    val uiState: StateFlow<AuctionMyTeamUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        loadTeams()
        viewModelScope.launch {
            auctionSessionManager.refreshEvent.collect {
                refreshTeam()
            }
        }
    }

    fun loadTeams() {
        val leagueId = auctionSessionManager.currentLeagueId.value
        if (leagueId == null) {
            _uiState.update { it.copy(error = "No league selected for auction.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val teams = repository.getTeamsFromLeague(leagueId)
                _uiState.update { it.copy(teams = teams, isLoading = false) }

                val targetTeamId = _uiState.value.selectedTeam?.id ?: teams.firstOrNull()?.id
                if (targetTeamId != null) {
                    val detailedTeam = repository.getTeamById(targetTeamId)
                    _uiState.update { it.copy(selectedTeam = detailedTeam) }
                }
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoading = false, error = e.displayMessage ?: "Failed to load teams.") }
            } catch (e: Exception) {
                Log.e("AuctionMyTeamVM", "Error loading teams: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "An unexpected error occurred.") }
            }
        }
    }

    fun selectTeam(team: Team) {
        viewModelScope.launch {
            try {
                val detailedTeam = repository.getTeamById(team.id)
                _uiState.update { it.copy(selectedTeam = detailedTeam, suggestionResults = null) }
            } catch (e: Exception) {
                Log.e("AuctionMyTeamVM", "Error selecting team: ${e.message}", e)
                _uiState.update { it.copy(selectedTeam = team, suggestionResults = null) }
            }
        }
    }

    fun refreshTeam() {
        val currentLeagueId = auctionSessionManager.currentLeagueId.value ?: return
        viewModelScope.launch {
            try {
                repository.clearCache()
                val teams = repository.getTeamsFromLeague(currentLeagueId)
                val targetTeamId = _uiState.value.selectedTeam?.id ?: teams.firstOrNull()?.id
                if (targetTeamId != null) {
                    val updatedTeam = repository.getTeamById(targetTeamId)
                    _uiState.update { it.copy(teams = teams, selectedTeam = updatedTeam) }
                }
            } catch (e: Exception) {
                Log.e("AuctionMyTeamVM", "Error refreshing team: ${e.message}", e)
            }
        }
    }

    fun removePlayer(playerId: Int) {
        val team = _uiState.value.selectedTeam ?: return
        val playerName = _uiState.value.selectedTeam?.players?.find { it.playerId == playerId }?.playerName ?: "Player"
        viewModelScope.launch {
            try {
                repository.removePlayerFromTeam(team.id, playerId)
                repository.clearCache()
                refreshTeam()
                _snackbarMessage.emit("$playerName removed from ${team.name}")
            } catch (e: AppError) {
                _snackbarMessage.emit(e.displayMessage ?: "Failed to remove player")
            } catch (e: Exception) {
                _snackbarMessage.emit("An unexpected error occurred")
            }
        }
    }

    fun requestSuggestion() {
        val team = _uiState.value.selectedTeam ?: return
        val leagueId = auctionSessionManager.currentLeagueId.value ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSuggestion = true, error = null) }
            try {
                val settings = auctionSessionManager.settings.value
                val request = SuggestionRequest(
                    teamId = team.id,
                    numTeams = 1,
                    favoritePlayerIds = settings.globalFavoritePlayerIds,
                    lineUp = settings.lineUp,
                    creditsDistribution = settings.creditsDistributionMultiplier
                )
                val results = repository.getSuggestion(request)
                _uiState.update { it.copy(suggestionResults = results, isLoadingSuggestion = false) }
                _snackbarMessage.emit("Suggestion loaded successfully")
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoadingSuggestion = false) }
                _snackbarMessage.emit(e.displayMessage ?: "Failed to get suggestion")
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingSuggestion = false) }
                _snackbarMessage.emit("An unexpected error occurred")
            }
        }
    }
}
