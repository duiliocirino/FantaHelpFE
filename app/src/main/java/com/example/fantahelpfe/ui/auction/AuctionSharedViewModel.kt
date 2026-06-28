package com.example.fantahelpfe.ui.auction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.data.UserPreferencesRepository // You'll need this for persistence
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuctionSettings(
    val globalFavoritePlayerIds: List<Int> = emptyList(),
    val creditsDistributionMultiplier: Int = 1 // Default
)

@HiltViewModel
class AuctionSharedViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository // For saving/loading settings
) : ViewModel() {

    // Initialize with default or loaded values
    private val _settings = MutableStateFlow(AuctionSettings())
    val settings: StateFlow<AuctionSettings> = _settings.asStateFlow()

    private val _currentLeagueId = MutableStateFlow<Int?>(null)
    val currentLeagueId: StateFlow<Int?> = _currentLeagueId.asStateFlow()

    // Example using StateFlow from a Repository (if UserPreferencesRepository exposes a Flow)
    // val settings: StateFlow<AuctionSettings> =
    //     userPreferencesRepository.auctionSettingsFlow
    //         .stateIn(
    //             scope = viewModelScope,
    //             started = SharingStarted.WhileSubscribed(5000),
    //             initialValue = AuctionSettings() // Default if flow is slow to emit
    //         )


    init {
        // Load initial settings if using a simple MutableStateFlow approach
        viewModelScope.launch {
            // This is a simplified loading. Ideally, UserPreferencesRepository provides a Flow.
            val loadedSettings = userPreferencesRepository.loadAuctionSettings() // Suspend fun
            _settings.update { loadedSettings }
        }
    }

    /**
     * Initializes the shared ViewModel with the current league ID for the auction.
     * This should be called when entering the auction flow.
     */
    fun initializeForLeague(leagueId: Int) {
        if (_currentLeagueId.value != leagueId) {
            _currentLeagueId.value = leagueId
            // TODO: Potentially load league-specific auction settings if any,
            // or reset certain states based on the new league.
            // For now, it just stores the leagueId.
            // You might also want to re-load global favorites if they could be league-specific
            // or ensure they are truly global.
            Log.d("AuctionSharedVM", "Initialized for new league: $leagueId")
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
}
