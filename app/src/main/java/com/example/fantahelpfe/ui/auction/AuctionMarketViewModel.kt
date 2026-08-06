package com.example.fantahelpfe.ui.auction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

enum class SortOption { Price, Rating, ExpectedPerformance, Name }

data class AuctionMarketUiState(
    val availablePlayers: List<Player> = emptyList(),
    val filteredPlayers: List<Player> = emptyList(),
    val isLoading: Boolean = false,
    val filterRole: String? = null, // Using String for simplicity, can be PlayerRole
    val sortBy: SortOption = SortOption.Price,
    val searchQuery: String = "",
    val error: String? = null
)

@HiltViewModel
class AuctionMarketViewModel @Inject constructor(
    private val repository: DataRepository,
    private val sharedViewModel: AuctionSharedViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuctionMarketUiState())
    val uiState: StateFlow<AuctionMarketUiState> = _uiState.asStateFlow()

    init {
        loadAvailablePlayers()
        viewModelScope.launch {
            sharedViewModel.refreshEvent.collect {
                loadAvailablePlayers()
            }
        }
    }

    fun loadAvailablePlayers() {
        val leagueId = sharedViewModel.currentLeagueId.value
        if (leagueId == null) {
            _uiState.update { it.copy(error = "No league selected.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val players = repository.getAvailablePlayersForLeague(leagueId)
                _uiState.update { 
                    it.copy(
                        availablePlayers = players, 
                        filteredPlayers = applyFiltersAndSort(players, null, SortOption.Price, ""),
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
        _uiState.update { 
            val newState = it.copy(filterRole = role)
            newState.copy(filteredPlayers = applyFiltersAndSort(newState.availablePlayers, role, newState.sortBy, newState.searchQuery))
        }
    }

    fun sortBy(option: SortOption) {
        _uiState.update { 
            val newState = it.copy(sortBy = option)
            newState.copy(filteredPlayers = applyFiltersAndSort(newState.availablePlayers, newState.filterRole, option, newState.searchQuery))
        }
    }

    fun search(query: String) {
        _uiState.update { 
            val newState = it.copy(searchQuery = query)
            newState.copy(filteredPlayers = applyFiltersAndSort(newState.availablePlayers, newState.filterRole, newState.sortBy, query))
        }
    }

    fun refresh() {
        loadAvailablePlayers()
    }

    private fun applyFiltersAndSort(
        players: List<Player>,
        role: String?,
        sortBy: SortOption,
        query: String
    ): List<Player> {
        var filtered = players

        if (role != null) {
            filtered = filtered.filter { it.role.name.equals(role, ignoreCase = true) }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        return when (sortBy) {
            SortOption.Price -> filtered.sortedBy { it.price }
            SortOption.Rating -> filtered.sortedByDescending { it.rating }
            SortOption.ExpectedPerformance -> filtered.sortedByDescending { it.expectedPerformance }
            SortOption.Name -> filtered.sortedBy { it.name }
        }
    }
}
