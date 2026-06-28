package com.example.fantahelpfe.ui.auction


import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.data.remote.dto.LineUp // Assuming you have this DTO
import com.example.fantahelpfe.data.remote.dto.SuggestionRequest
import com.example.fantahelpfe.data.DataRepository
import com.example.fantahelpfe.data.remote.dto.AuctionedPlayerInfo
import com.example.fantahelpfe.model.League // For league context if needed for LineUp
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.model.Team
import com.example.fantahelpfe.utils.YourAppError
import kotlinx.coroutines.async
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI State (mostly the same, added LineUp) ---
data class AuctionBuyPlayersUiState(
    val leagueId: Int, // Store leagueId for easy access
    val leagueDetails: League? = null, // For LineUp or other league rules
    val searchQuery: String = "",
    val searchResults: List<Player> = emptyList(),
    val isSearchingPlayers: Boolean = false,
    val selectedPlayer: Player? = null,

    val bidAmountInput: String = "1", // Default to 1 to be > 0
    val currentBidAmount: Int = 1,

    val teamsInLeague: List<Team> = emptyList(), // User's teams or all teams
    val selectedTeamForAssignment: Team? = null,
    val isLoadingTeams: Boolean = false,

    // Convenience Score (Optimal Team Comparison)
    val lineUpForSuggestion: LineUp? = null, // Crucial for SuggestionRequest
    val currentRosterIdsForTeam: List<Int> = emptyList(), // IDs of players already on selectedTeamForAssignment

    val baseOptimalScore: Double? = null,
    val potentialOptimalScore: Double? = null,
    val convenienceScoreDelta: Double? = null,
    val isLoadingConvenienceScore: Boolean = false,
    val convenienceScoreError: String? = null,

    val assignmentStatus: ActionStatus = ActionStatus.Idle,
    val assignmentError: String? = null,
    val generalError: String? = null
)

sealed interface ActionStatus {
    object Idle : ActionStatus
    object Loading : ActionStatus
    object Success : ActionStatus
}


@HiltViewModel
class AuctionBuyPlayersViewModel @Inject constructor(
    private val repository: DataRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val currentLeagueId: Int = checkNotNull(savedStateHandle["leagueId"])

    private val _uiState = MutableStateFlow(AuctionBuyPlayersUiState(leagueId = currentLeagueId))
    val uiState: StateFlow<AuctionBuyPlayersUiState> = _uiState.asStateFlow()

    private var playerSearchJob: Job? = null
    private var convenienceScoreJob: Job? = null

    init {
        Log.d("AuctionBuyVM", "Initializing for league ID: $currentLeagueId")
        fetchLeagueDetailsAndTeams() // Fetch league to get LineUp, then teams

        // The observer will now need to be triggered from the Composable,
        // or this ViewModel needs to combine its state with the shared state.
        // For now, let's keep the observer, but it will be slightly less direct
        // as shared settings are external.
        // A better approach might be to have the Composable call calculateConvenienceScore
        // with all necessary parameters (including shared ones).

        // Let's modify observeRelevantStateForConvenienceScore to NOT directly call calculate.
        // Instead, the Composable will observe shared state and this state, and call the calculate function.
    }

    private fun fetchLeagueDetailsAndTeams() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTeams = true) } // Use isLoadingTeams as a general initial load flag
            try {
                // 1. Fetch League Details (needed for LineUp constraints for Suggestion API)
                val league = repository.getLeagueById(currentLeagueId)
                if (league == null) {
                    _uiState.update { it.copy(isLoadingTeams = false, generalError = "Failed to load league details.") }
                    return@launch
                }
                // TODO: Extract LineUp from 'league' domain model or have a separate call
                // For now, let's assume a dummy/default LineUp or that it's part of League model
                val actualLineUp = league.getLineUp() // Hypothetical method on League domain model
                _uiState.update { it.copy(leagueDetails = league, lineUpForSuggestion = actualLineUp) }

                // 2. Fetch Teams (e.g., user's teams in this league)
                // This might depend on the logged-in user or be all teams if admin view
                val teams = repository.getTeamsForLeague(currentLeagueId) // Or getTeamsForUserInLeague
                _uiState.update { it.copy(teamsInLeague = teams, isLoadingTeams = false) }

                // If a default team should be selected, or if there's only one team
                if (teams.size == 1) {
                    onTeamSelectedForAssignment(teams.first()) // Auto-select if only one
                }

            } catch (e: YourAppError) {
                Log.e("AuctionBuyVM", "Error fetching league/teams: ${e.displayMessage}", e.cause)
                _uiState.update { it.copy(isLoadingTeams = false, generalError = e.displayMessage ?: "Failed to load initial data.") }
            } catch (e: Exception) {
                Log.e("AuctionBuyVM", "Unexpected error fetching league/teams: ${e.message}", e)
                _uiState.update { it.copy(isLoadingTeams = false, generalError = "An unexpected error occurred.") }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearchingPlayers = query.length > 2) }
        playerSearchJob?.cancel() // Cancel previous search
        if (query.length > 2) { // Perform search if query is long enough
            playerSearchJob = viewModelScope.launch {
                delay(300) // Debounce: wait 300ms after user stops typing
                try {
                    Log.d("AuctionBuyVM", "Searching players with query: $query for league $currentLeagueId")
                    val players: List<Player> = repository.searchPlayer(currentLeagueId, query)
                    _uiState.update { it.copy(searchResults = players, isSearchingPlayers = false) }
                } catch (e: YourAppError) {
                    Log.e("AuctionBuyVM", "Player search error: ${e.displayMessage}")
                    _uiState.update { it.copy(searchResults = emptyList(), isSearchingPlayers = false, generalError = "Player search failed.") }
                } catch (e: Exception) {
                    Log.e("AuctionBuyVM", "Unexpected player search error: ${e.message}")
                    _uiState.update { it.copy(searchResults = emptyList(), isSearchingPlayers = false, generalError = "Player search error.") }
                }
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), isSearchingPlayers = false) }
        }
    }

    fun onPlayerSelectedFromSearch(player: Player) {
        // Here, player is the full domain model from search
        _uiState.update {
            it.copy(
                selectedPlayer = player,
                searchResults = emptyList(), // Clear search results
                searchQuery = player.name, // Update search box with full name
                isSearchingPlayers = false
            )
        }
        // Convenience score will be triggered by the observer
    }

    fun clearSelectedPlayer() {
        _uiState.update { it.copy(selectedPlayer = null, searchQuery = "") }
    }


    fun onBidAmountChanged(newAmountText: String) {
        val amountInt = newAmountText.toIntOrNull() ?: _uiState.value.currentBidAmount // Keep old if invalid
        _uiState.update { it.copy(bidAmountInput = newAmountText, currentBidAmount = amountInt.coerceAtLeast(0)) }
        // Convenience score will be triggered by the observer if player is selected
    }

    fun adjustBid(delta: Int) {
        val currentAmount = _uiState.value.currentBidAmount
        val newAmount = (currentAmount + delta).coerceAtLeast(0) // Ensure non-negative
        onBidAmountChanged(newAmount.toString()) // Use existing handler
    }

    fun onTeamSelectedForAssignment(team: Team) {
        _uiState.update { it.copy(selectedTeamForAssignment = team) }
        // When team changes, we need to fetch its current roster for suggestions
        fetchCurrentRosterForSelectedTeam(team.id)
        // Convenience score will be triggered by the observer
    }

    private fun fetchCurrentRosterForSelectedTeam(teamId: Int) {
        viewModelScope.launch {
            try {
                // This method needs to exist: get players already assigned to this team
                val rosterPlayers = repository.getPlayersForTeam(teamId)
                _uiState.update { it.copy(currentRosterIdsForTeam = rosterPlayers.map { p -> p.playerId }) }
                // After updating roster, convenience score observer will pick it up
            } catch (e: Exception) {
                Log.e("AuctionBuyVM", "Failed to fetch roster for team $teamId: ${e.message}")
                _uiState.update { it.copy(currentRosterIdsForTeam = emptyList(), convenienceScoreError = "Could not load team roster.") }
            }
        }
    }


    fun calculateConvenienceScore(
        playerToConsider: Player,
        bidForPlayer: Int,
        teamForContext: Team,
        lineUp: LineUp,
        globalFavoritePlayerIds: List<Int>, // From SharedViewModel
        creditsDistributionMultiplier: Int   // From SharedViewModel
    ) {
        if (bidForPlayer <= 0) {
            _uiState.update { it.copy(baseOptimalScore = null, /* ... */ convenienceScoreError = "Bid must be positive.")}
            return
        }

        convenienceScoreJob?.cancel()
        convenienceScoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingConvenienceScore = true, /* ... */) }
            Log.d("AuctionBuyVM", "Calculating convenience score for ${playerToConsider.name} at $bidForPlayer on team ${teamForContext.name} with global favs: $globalFavoritePlayerIds & multiplier: $creditsDistributionMultiplier")

            try {
                // Launch both requests in parallel
                val baseScoreDeferred = async {
                    val baseRequest = SuggestionRequest(
                        teamId = teamForContext.id,
                        numTeams = 1,
                        favoritePlayerIds = globalFavoritePlayerIds, // Use global favorites
                        lineUp = lineUp,
                        creditsDistribution = creditsDistributionMultiplier // Use global multiplier
                    )
                    Log.d("AuctionBuyVM", "Base Request: $baseRequest")
                    val baseResult = repository.getSuggestion(baseRequest).first() // TODO: handle the case for more elements
                    baseResult.score.totalScore
                }

                val potentialScoreDeferred = async {
                    val potentialRequest = SuggestionRequest(
                        teamId = teamForContext.id,
                        numTeams = 1,
                        favoritePlayerIds = globalFavoritePlayerIds, // Keep global favorites the same
                        lineUp = lineUp,
                        creditsDistribution = creditsDistributionMultiplier,
                        auctionedPlayer = AuctionedPlayerInfo(
                            playerId = playerToConsider.id,
                            acquisitionPrice = bidForPlayer
                        )
                    )
                    Log.d("AuctionBuyVM", "Potential Request: $potentialRequest")
                    val potentialResult = repository.getSuggestion(potentialRequest).first() // TODO: handle the case for more elements
                    potentialResult.score.totalScore
                }

                // Await results
                val baseScore = baseScoreDeferred.await()
                val potentialScore = potentialScoreDeferred.await()

                _uiState.update {
                    it.copy(
                        baseOptimalScore = baseScore,
                        potentialOptimalScore = potentialScore,
                        convenienceScoreDelta = potentialScore - baseScore,
                        isLoadingConvenienceScore = false
                    )
                }

            } catch (e: YourAppError) {
                // ... error handling ...
                Log.e("AuctionBuyVM", "Error calculating score: ${e.displayMessage}")
                _uiState.update { it.copy(isLoadingConvenienceScore = false, convenienceScoreError = e.displayMessage ?: "Could not calculate score advantage.") }
            } catch (e: Exception) {
                // ... error handling ...
                Log.e("AuctionBuyVM", "Error calculating score: ${e.message}")
                _uiState.update { it.copy(isLoadingConvenienceScore = false, convenienceScoreError = "Error calculating score.") }
            }
        }
    }

    fun clearConvenienceScores() {
        _uiState.update { it.copy(
            baseOptimalScore = null,
            potentialOptimalScore = null,
            convenienceScoreDelta = null,
            isLoadingConvenienceScore = false,
            convenienceScoreError = null
        )}
    }

    fun assignPlayer() {
        val player = _uiState.value.selectedPlayer
        val team = _uiState.value.selectedTeamForAssignment
        val bid = _uiState.value.currentBidAmount // This bid amount is for the actual assignment API call
        val league = _uiState.value.leagueDetails

        if (player == null || team == null || bid <= 0 || league == null) {
            _uiState.update { it.copy(assignmentError = "Please select player, team, and enter a valid bid.") }
            return
        }

        _uiState.update { it.copy(assignmentStatus = ActionStatus.Loading, assignmentError = null) }
        viewModelScope.launch {
            try {
                // This API call assigns player to team with the actual BID
                repository.assignPlayerToTeam(team.id, player.id, bid)
                _uiState.update { it.copy(assignmentStatus = ActionStatus.Success) }

                // After successful assignment:
                // 1. Update the local roster for the team for next suggestion
                fetchCurrentRosterForSelectedTeam(team.id) // This will re-trigger convenience score if conditions met
                // 2. Clear selection
                _uiState.update { it.copy(
                    selectedPlayer = null,
                    searchQuery = "",
                    bidAmountInput = "1",
                    currentBidAmount = 1,
                    // Keep selectedTeamForAssignment for next pick or allow user to change
                    // Reset convenience scores
                    baseOptimalScore = null,
                    potentialOptimalScore = null,
                    convenienceScoreDelta = null,
                    convenienceScoreError = null
                )}
                // Potentially remove player from 'available players' list if that's managed client-side
                // or expect search to no longer return them.

            } catch (e: YourAppError) {
                Log.e("AuctionBuyVM", "Assign player error: ${e.displayMessage}", e.cause)
                _uiState.update { it.copy(assignmentStatus = ActionStatus.Idle, assignmentError = e.displayMessage ?: "Failed to assign player") }
            } catch (e: Exception) {
                Log.e("AuctionBuyVM", "Unexpected assign player error: ${e.message}", e)
                _uiState.update { it.copy(assignmentStatus = ActionStatus.Idle, assignmentError = "An unexpected error occurred during assignment.") }
            }
        }
    }

    fun resetAssignmentStatus() {
        _uiState.update { it.copy(assignmentStatus = ActionStatus.Idle) }
    }
}


// --- Dummy/Placeholder for League Domain Model method ---
// You need to define how LineUp is stored/accessed for your League
fun League.getLineUp(): LineUp {
    // TODO: Implement logic to get LineUp based on league rules.
    // This could be a fixed value, or part of the League object from the API.
    // For now, returning a default. Replace this!
    return LineUp(
        keepers = 1, defenders = 4, midfielders = 3, attackers = 3
    )
}