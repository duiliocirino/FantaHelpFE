package com.example.fantahelpfe.ui.auction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.data.AuctionSessionManager
import com.example.fantahelpfe.data.DataRepository
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.utils.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption(val displayName: String) {
    ExpectedPerformance("⚡ Exp. Perf"),
    ExpectedPrice("🤖 Exp. Price"),
    Rating("⭐️ Rating"),
    Regularness("⏱️ Starter Share"),
    Price("💰 Base Price"),
    Name("🔤 Name")
}

data class AuctionMarketUiState(
    val availablePlayers: List<Player> = emptyList(),
    val filteredPlayers: List<Player> = emptyList(),
    val isLoading: Boolean = false,
    val filterRole: String? = null,
    val sortBy: SortOption = SortOption.ExpectedPerformance,
    val searchQuery: String = "",
    val onlyFavorites: Boolean = false,
    val globalFavoriteIds: List<Int> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AuctionMarketViewModel @Inject constructor(
    private val repository: DataRepository,
    private val auctionSessionManager: AuctionSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuctionMarketUiState())
    val uiState: StateFlow<AuctionMarketUiState> = _uiState.asStateFlow()

    init {
        loadAvailablePlayers()

        viewModelScope.launch {
            auctionSessionManager.refreshEvent.collect {
                loadAvailablePlayers()
            }
        }

        viewModelScope.launch {
            auctionSessionManager.settings.collect { settings ->
                _uiState.update { state ->
                    val favIds = settings.globalFavoritePlayerIds
                    val newState = state.copy(globalFavoriteIds = favIds)
                    newState.copy(
                        filteredPlayers = applyFiltersAndSort(
                            newState.availablePlayers,
                            newState.filterRole,
                            newState.sortBy,
                            newState.searchQuery,
                            newState.onlyFavorites,
                            favIds
                        )
                    )
                }
            }
        }
    }

    fun loadAvailablePlayers() {
        val leagueId = auctionSessionManager.currentLeagueId.value
        if (leagueId == null) {
            _uiState.update { it.copy(error = "No league selected.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val startersCount = auctionSessionManager.settings.value.lineUp.totalStarters
                val players = repository.getAvailablePlayersForLeague(leagueId, starters = startersCount)
                _uiState.update { state ->
                    state.copy(
                        availablePlayers = players,
                        filteredPlayers = applyFiltersAndSort(
                            players,
                            state.filterRole,
                            state.sortBy,
                            state.searchQuery,
                            state.onlyFavorites,
                            state.globalFavoriteIds
                        ),
                        isLoading = false
                    )
                }
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoading = false, error = e.displayMessage ?: "Failed to load players.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "An unexpected error occurred.") }
            }
        }
    }

    fun filterByRole(role: String?) {
        _uiState.update { state ->
            val newState = state.copy(filterRole = role)
            newState.copy(
                filteredPlayers = applyFiltersAndSort(
                    newState.availablePlayers,
                    role,
                    newState.sortBy,
                    newState.searchQuery,
                    newState.onlyFavorites,
                    newState.globalFavoriteIds
                )
            )
        }
    }

    fun sortBy(option: SortOption) {
        _uiState.update { state ->
            val newState = state.copy(sortBy = option)
            newState.copy(
                filteredPlayers = applyFiltersAndSort(
                    newState.availablePlayers,
                    newState.filterRole,
                    option,
                    newState.searchQuery,
                    newState.onlyFavorites,
                    newState.globalFavoriteIds
                )
            )
        }
    }

    fun toggleOnlyFavorites() {
        _uiState.update { state ->
            val newOnlyFavs = !state.onlyFavorites
            val newState = state.copy(onlyFavorites = newOnlyFavs)
            newState.copy(
                filteredPlayers = applyFiltersAndSort(
                    newState.availablePlayers,
                    newState.filterRole,
                    newState.sortBy,
                    newState.searchQuery,
                    newOnlyFavs,
                    newState.globalFavoriteIds
                )
            )
        }
    }

    fun search(query: String) {
        _uiState.update { state ->
            val newState = state.copy(searchQuery = query)
            newState.copy(
                filteredPlayers = applyFiltersAndSort(
                    newState.availablePlayers,
                    newState.filterRole,
                    newState.sortBy,
                    query,
                    newState.onlyFavorites,
                    newState.globalFavoriteIds
                )
            )
        }
    }

    fun refresh() {
        loadAvailablePlayers()
    }

    private fun applyFiltersAndSort(
        players: List<Player>,
        role: String?,
        sortBy: SortOption,
        query: String,
        onlyFavs: Boolean,
        favIds: List<Int>
    ): List<Player> {
        var filtered = players

        if (role != null) {
            filtered = filtered.filter { it.role.name.equals(role, ignoreCase = true) }
        }

        if (onlyFavs) {
            val favSet = favIds.toSet()
            filtered = filtered.filter { it.id in favSet }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) || it.squad.contains(query, ignoreCase = true) }
        }

        return when (sortBy) {
            SortOption.ExpectedPerformance -> filtered.sortedByDescending { it.expectedPerformance }
            SortOption.ExpectedPrice -> filtered.sortedByDescending { if (it.expectedPrice > 0) it.expectedPrice else it.price }
            SortOption.Rating -> filtered.sortedByDescending { it.rating }
            SortOption.Regularness -> filtered.sortedByDescending { it.regularness }
            SortOption.Price -> filtered.sortedBy { it.price }
            SortOption.Name -> filtered.sortedBy { it.name }
        }
    }
}
