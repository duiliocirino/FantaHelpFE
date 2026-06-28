package com.example.fantahelpfe.ui.auction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.data.DataRepository
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.model.League // If you need to load league details too
import com.example.fantahelpfe.utils.YourAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

sealed interface AuctionScreenUiState {
    object Loading : AuctionScreenUiState
    data class Success(
        val availablePlayers: List<Player>,
        val leagueDetails: League? = null // Optional: if you also want to show league name, etc.
    ) : AuctionScreenUiState
    data class Error(val message: String) : AuctionScreenUiState
    object NoPlayersAvailable : AuctionScreenUiState // Specific state for when the list is empty
}

@HiltViewModel
class AuctionViewModel @Inject constructor(
    private val repository: DataRepository,
    savedStateHandle: SavedStateHandle // To get leagueId from navigation arguments
) : ViewModel() {

    private val leagueId: Int = checkNotNull(savedStateHandle["leagueId"]) // Assumes "leagueId" is the nav argument name

    private val _uiState = MutableStateFlow<AuctionScreenUiState>(AuctionScreenUiState.Loading)
    val uiState: StateFlow<AuctionScreenUiState> = _uiState.asStateFlow()

    init {
        Log.d("AuctionViewModel", "Initializing for league ID: $leagueId")
        fetchAvailablePlayers()
        // Optionally fetch league details too if needed for the screen
        // fetchLeagueDetails()
    }

    fun fetchAvailablePlayers() {
        _uiState.update { AuctionScreenUiState.Loading } // Set loading state for players
        viewModelScope.launch {
            try {
                Log.d("AuctionViewModel", "Fetching available players for league ID: $leagueId")
                // Assuming repository.getAvailablePlayersForLeague(leagueId) exists
                val players = repository.getAvailablePlayersForLeague(leagueId)
                Log.d("AuctionViewModel", "Fetched ${players.size} available players.")

                if (players.isEmpty()) {
                    _uiState.update { AuctionScreenUiState.NoPlayersAvailable }
                } else {
                    // If you also fetched league details, combine them here
                    val currentLeagueDetails = (_uiState.value as? AuctionScreenUiState.Success)?.leagueDetails
                    _uiState.update { AuctionScreenUiState.Success(players, currentLeagueDetails) }
                }
            } catch (error: YourAppError) {
                Log.e("AuctionViewModel", "Error fetching available players: ${error.getDetailedMessage()}", error.cause ?: error)
                _uiState.update { AuctionScreenUiState.Error(error.displayMessage ?: "Failed to load players.") }
            } catch (e: Exception) {
                Log.e("AuctionViewModel", "Unexpected error fetching available players: ${e.message}", e)
                _uiState.update { AuctionScreenUiState.Error("An unexpected error occurred.") }
            }
        }
    }

    // Optional: If you want to load details about the league itself
    fun fetchLeagueDetails() {
        viewModelScope.launch {
            try {
                Log.d("AuctionViewModel", "Fetching details for league ID: $leagueId")
                val league = repository.getLeagueById(leagueId) // Assuming this method exists
                Log.d("AuctionViewModel", "Fetched league details: ${league.name}")

                // Update existing success state or create new one if state was Loading/Error
                _uiState.update { currentState ->
                    if (currentState is AuctionScreenUiState.Success) {
                        currentState.copy(leagueDetails = league)
                    } else if (currentState is AuctionScreenUiState.NoPlayersAvailable) {
                        // If players were empty but league details loaded, we might still want to show league name
                        AuctionScreenUiState.Success(emptyList(), league) // No, this isn't right if no players.
                        // Better to keep leagueDetails separate or update Success
                        // This logic needs refinement based on how you want to combine states
                        // For simplicity now, let's assume players are the primary data.
                        AuctionScreenUiState.Success(emptyList(), league)
                    }
                    else run { // If current state was Loading or Error, but league loaded
                        AuctionScreenUiState.Success(emptyList(), league) // This also implies no players yet
                    }
                }
            } catch (error: YourAppError) {
                Log.w("AuctionViewModel", "Could not fetch league details: ${error.displayMessage}")
                // Don't necessarily set main state to Error if players loaded fine
            } catch (e: Exception) {
                Log.w("AuctionViewModel", "Unexpected error fetching league details: ${e.message}")
            }
        }
    }
}

