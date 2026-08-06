package com.example.fantahelpfe.ui.auction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.data.DataRepository
import com.example.fantahelpfe.data.remote.dto.SuggestionRequest
import com.example.fantahelpfe.data.remote.dto.SuggestionResult
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.model.Team
import com.example.fantahelpfe.model.TeamPlayer
import com.example.fantahelpfe.utils.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val sharedViewModel: AuctionSharedViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuctionMyTeamUiState())
    val uiState: StateFlow<AuctionMyTeamUiState> = _uiState.asStateFlow()

    init {
        loadTeams()
        viewModelScope.launch {
            sharedViewModel.refreshEvent.collect {
                refreshTeam()
            }
        }
    }

    fun loadTeams() {
        val leagueId = sharedViewModel.currentLeagueId.value
        if (leagueId == null) {
            _uiState.update { it.copy(error = "No league selected for auction.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val teams = repository.getTeamsFromLeague(leagueId)
                // For now, we show all teams in the league. 
                // Later we will filter by ownerId.
                _uiState.update { it.copy(teams = teams, isLoading = false) }
                
                if (teams.isNotEmpty() && _uiState.value.selectedTeam == null) {
                    selectTeam(teams.first())
                }
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoading = false, error = e.displayMessage ?: "Failed to load teams.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "An unexpected error occurred.") }
            }
        }
    }

    fun selectTeam(team: Team) {
        _uiState.update { it.copy(selectedTeam = team, suggestionResults = null) }
        refreshTeam()
    }

    fun refreshTeam() {
        val team = _uiState.value.selectedTeam ?: return
        viewModelScope.launch {
            try {
                // Re-fetch the team to get the latest budget and players
                // We use getTeamsFromLeague and find the team, or we could add a getTeamById to repository
                val teams = repository.getTeamsFromLeague(sharedViewModel.currentLeagueId!!)
                val updatedTeam = teams.find { it.id == team.id }
                if (updatedTeam != null) {
                    _uiState.update { it.copy(selectedTeam = updatedTeam) }
                }
            } catch (e: Exception) {
                Log.e("AuctionMyTeamVM", "Error refreshing team: ${e.message}")
            }
        }
    }

    fun removePlayer(playerId: Int) {
        val team = _uiState.value.selectedTeam ?: return
        viewModelScope.launch {
            try {
                repository.removePlayerFromTeam(team.id, playerId)
                // Invalidate cache and refresh
                repository.clearCache()
                refreshTeam()
            } catch (e: AppError) {
                _uiState.update { it.copy(error = e.displayMessage ?: "Failed to remove player.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "An unexpected error occurred.") }
            }
        }
    }

    fun requestSuggestion() {
        val team = _uiState.value.selectedTeam ?: return
        val settings = sharedViewModel.settings.value
        val leagueId = sharedViewModel.currentLeagueId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSuggestion = true, error = null) }
            try {
                val request = SuggestionRequest(
                    teamId = team.id,
                    numTeams = 1,
                    favoritePlayerIds = settings.globalFavoritePlayerIds,
                    lineUp = settings.lineUp,
                    creditsDistribution = settings.creditsDistributionMultiplier
                )
                val results = repository.getSuggestion(request)
                _uiState.update { it.copy(suggestionResults = results, isLoadingSuggestion = false) }
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoadingSuggestion = false, error = e.displayMessage ?: "Failed to get suggestion.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingSuggestion = false, error = "An unexpected error occurred.") }
            }
        }
    }
}
