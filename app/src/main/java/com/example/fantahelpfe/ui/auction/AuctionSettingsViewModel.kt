package com.example.fantahelpfe.ui.auction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.data.AuctionSessionManager
import com.example.fantahelpfe.data.AuctionSettings
import com.example.fantahelpfe.data.DataRepository
import com.example.fantahelpfe.data.remote.dto.LineUp
import com.example.fantahelpfe.model.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuctionSettingsUiState(
    val settings: AuctionSettings = AuctionSettings(),
    val searchQuery: String = "",
    val searchResults: List<Player> = emptyList(),
    val favoritePlayers: List<Player> = emptyList(),
    val isLoadingPlayers: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuctionSettingsViewModel @Inject constructor(
    private val repository: DataRepository,
    private val auctionSessionManager: AuctionSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuctionSettingsUiState())
    val uiState: StateFlow<AuctionSettingsUiState> = _uiState.asStateFlow()

    private var allPlayersMap: Map<Int, Player> = emptyMap()
    private var allPlayersList: List<Player> = emptyList()

    init {
        loadAllPlayers()
        observeSettings()
    }

    private fun loadAllPlayers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlayers = true) }
            try {
                val players = repository.getAllPlayers()
                allPlayersList = players
                allPlayersMap = players.associateBy { it.id }
                updateFavoritePlayersList(_uiState.value.settings.globalFavoritePlayerIds)
                _uiState.update { it.copy(isLoadingPlayers = false) }
            } catch (e: Exception) {
                Log.e("AuctionSettingsVM", "Failed to load players: ${e.message}", e)
                _uiState.update { it.copy(isLoadingPlayers = false, error = "Failed to load player database.") }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            auctionSessionManager.settings.collectLatest { settings ->
                _uiState.update { it.copy(settings = settings) }
                updateFavoritePlayersList(settings.globalFavoritePlayerIds)
            }
        }
    }

    private fun updateFavoritePlayersList(favIds: List<Int>) {
        val resolved = favIds.mapNotNull { id ->
            allPlayersMap[id] ?: Player(
                id = id,
                name = "Player #$id",
                realTeam = "Unknown",
                role = com.example.fantahelpfe.model.PlayerRole.UNKNOWN,
                price = 0,
                rating = 0.0,
                expectedPerformance = 0.0
            )
        }
        _uiState.update { it.copy(favoritePlayers = resolved) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                emptyList()
            } else {
                allPlayersList.filter { player ->
                    player.name.contains(query, ignoreCase = true) ||
                            player.realTeam.contains(query, ignoreCase = true)
                }.take(8)
            }
            state.copy(searchQuery = query, searchResults = filtered)
        }
    }

    fun addFavoritePlayer(player: Player) {
        auctionSessionManager.addGlobalFavoritePlayer(player.id)
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }

    fun removeFavoritePlayer(playerId: Int) {
        auctionSessionManager.removeGlobalFavoritePlayer(playerId)
    }

    fun updateCreditsDistributionMultiplier(multiplier: Int) {
        auctionSessionManager.updateCreditsDistributionMultiplier(multiplier)
    }

    fun updateLineUp(lineUp: LineUp) {
        auctionSessionManager.updateLineUp(lineUp)
    }
}
