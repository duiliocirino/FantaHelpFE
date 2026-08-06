package com.example.fantahelpfe.ui.auction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.data.DataRepository
import com.example.fantahelpfe.data.UserPreferencesRepository
import com.example.fantahelpfe.data.remote.dto.LineUp
import com.example.fantahelpfe.model.League
import com.example.fantahelpfe.model.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuctionSettings(
    val globalFavoritePlayerIds: List<Int> = emptyList(),
    val creditsDistributionMultiplier: Int = 1,
    val lineUp: LineUp = LineUp(defenders = 4, midfielders = 3, attackers = 3)
)

@HiltViewModel
class AuctionSharedViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val repository: DataRepository
) : ViewModel() {

    // Initialize with default or loaded values
    private val _settings = MutableStateFlow(AuctionSettings())
    val settings: StateFlow<AuctionSettings> = _settings.asStateFlow()

    private val _currentLeagueId = MutableStateFlow<Int?>(null)
    val currentLeagueId: StateFlow<Int?> = _currentLeagueId.asStateFlow()

    private val _leagueDetails = MutableStateFlow<League?>(null)
    val leagueDetails: StateFlow<League?> = _leagueDetails.asStateFlow()

    val refreshEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Example using StateFlow from a Repository (if UserPreferencesRepository exposes a Flow)
    // val settings: StateFlow<AuctionSettings> =
    //     userPreferencesRepository.auctionSettingsFlow
    //         .stateIn(
    //             scope = viewModelScope,
    //             started = SharingStarted.WhileSubscribed(5000),
    //             initialValue = AuctionSettings() // Default if flow is slow to emit
    //         )


    init {
        viewModelScope.launch {
            val loadedSettings = userPreferencesRepository.loadAuctionSettings()
            _settings.update { loadedSettings }
        }
    }

    fun initializeForLeague(leagueId: Int) {
        if (_currentLeagueId.value != leagueId) {
            _currentLeagueId.value = leagueId
            refreshLeagueDetails()
            Log.d("AuctionSharedVM", "Initialized for new league: $leagueId")
        }
    }

    fun refreshLeagueDetails() {
        val leagueId = _currentLeagueId.value ?: return
        viewModelScope.launch {
            try {
                val league = repository.getLeagueById(leagueId)
                _leagueDetails.update { league }
            } catch (e: Exception) {
                Log.e("AuctionSharedVM", "Error refreshing league details: ${e.message}")
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            repository.clearCache()
            refreshLeagueDetails()
            refreshEvent.emit(Unit)
        }
    }

    fun updateGlobalFavoritePlayerIds(playerIds: List<Int>) {
        _settings.update { it.copy(globalFavoritePlayerIds = playerIds) }
        viewModelScope.launch {
            userPreferencesRepository.saveAuctionSettings(_settings.value)
        }
    }

    fun addGlobalFavoritePlayer(playerId: Int) {
        if (!_settings.value.globalFavoritePlayerIds.contains(playerId)) {
            _settings.update {
                it.copy(globalFavoritePlayerIds = it.globalFavoritePlayerIds + playerId)
            }
            viewModelScope.launch {
                userPreferencesRepository.saveAuctionSettings(_settings.value)
            }
        }
    }

    fun removeGlobalFavoritePlayer(playerId: Int) {
        _settings.update {
            it.copy(globalFavoritePlayerIds = it.globalFavoritePlayerIds - playerId)
        }
        viewModelScope.launch {
            userPreferencesRepository.saveAuctionSettings(_settings.value)
        }
    }

    fun updateCreditsDistributionMultiplier(multiplier: Int) {
        _settings.update { it.copy(creditsDistributionMultiplier = multiplier.coerceAtLeast(1)) }
        viewModelScope.launch {
            userPreferencesRepository.saveAuctionSettings(_settings.value)
        }
    }

    fun updateLineUp(lineUp: LineUp) {
        _settings.update { it.copy(lineUp = lineUp) }
        viewModelScope.launch {
            userPreferencesRepository.saveAuctionSettings(_settings.value)
        }
    }

    // Cross-tab communication: Market "Buy" button selects a player in the Buy tab
    private val _selectedPlayerForBuy = MutableStateFlow<Player?>(null)
    val selectedPlayerForBuy: StateFlow<Player?> = _selectedPlayerForBuy.asStateFlow()

    fun selectPlayerForBuy(player: Player) {
        _selectedPlayerForBuy.value = player
    }

    fun clearSelectedPlayerForBuy() {
        _selectedPlayerForBuy.value = null
    }
}
