package com.example.fantahelpfe.data

import android.util.Log
import com.example.fantahelpfe.data.remote.dto.BudgetAllocation
import com.example.fantahelpfe.data.remote.dto.LineUp
import com.example.fantahelpfe.data.remote.dto.StrategyWeights
import com.example.fantahelpfe.model.League
import com.example.fantahelpfe.model.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuctionSessionManager @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val repository: DataRepository
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _settings = MutableStateFlow(AuctionSettings())
    val settings: StateFlow<AuctionSettings> = _settings.asStateFlow()

    private val _currentLeagueId = MutableStateFlow<Int?>(null)
    val currentLeagueId: StateFlow<Int?> = _currentLeagueId.asStateFlow()

    private val _leagueDetails = MutableStateFlow<League?>(null)
    val leagueDetails: StateFlow<League?> = _leagueDetails.asStateFlow()

    private val _refreshEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshEvent: SharedFlow<Unit> = _refreshEvent.asSharedFlow()

    private val _selectedPlayerForBuy = MutableStateFlow<Player?>(null)
    val selectedPlayerForBuy: StateFlow<Player?> = _selectedPlayerForBuy.asStateFlow()

    init {
        managerScope.launch {
            val loadedSettings = userPreferencesRepository.loadAuctionSettings()
            _settings.update { loadedSettings }
        }
    }

    fun initializeForLeague(leagueId: Int) {
        if (_currentLeagueId.value != leagueId) {
            _currentLeagueId.value = leagueId
            refreshLeagueDetails()
            Log.d("AuctionSessionManager", "Initialized for new league: $leagueId")
        }
    }

    fun refreshLeagueDetails() {
        val leagueId = _currentLeagueId.value ?: return
        managerScope.launch {
            try {
                val league = repository.getLeagueById(leagueId)
                _leagueDetails.update { league }
            } catch (e: Exception) {
                Log.e("AuctionSessionManager", "Error refreshing league details: ${e.message}")
            }
        }
    }

    fun refreshAll() {
        managerScope.launch {
            repository.clearCache()
            refreshLeagueDetails()
            _refreshEvent.emit(Unit)
        }
    }

    fun updateGlobalFavoritePlayerIds(playerIds: List<Int>) {
        _settings.update { it.copy(globalFavoritePlayerIds = playerIds) }
        saveSettings()
    }

    fun addGlobalFavoritePlayer(playerId: Int) {
        if (!_settings.value.globalFavoritePlayerIds.contains(playerId)) {
            _settings.update {
                it.copy(globalFavoritePlayerIds = it.globalFavoritePlayerIds + playerId)
            }
            saveSettings()
        }
    }

    fun removeGlobalFavoritePlayer(playerId: Int) {
        _settings.update {
            it.copy(globalFavoritePlayerIds = it.globalFavoritePlayerIds - playerId)
        }
        saveSettings()
    }

    fun updateCreditsDistributionMultiplier(multiplier: Int) {
        _settings.update {
            it.copy(
                creditsDistributionMultiplier = multiplier.coerceIn(0, 20),
                selectedPresetName = "Custom"
            )
        }
        saveSettings()
    }

    fun updateBudgetAllocation(budgetAllocation: BudgetAllocation) {
        _settings.update {
            it.copy(
                budgetAllocation = budgetAllocation,
                selectedPresetName = "Custom"
            )
        }
        saveSettings()
    }

    fun updateCreditsDistributionStrategy(multiplier: Int, budgetAllocation: BudgetAllocation) {
        _settings.update {
            it.copy(
                creditsDistributionMultiplier = multiplier.coerceIn(0, 20),
                budgetAllocation = budgetAllocation,
                selectedPresetName = "Custom"
            )
        }
        saveSettings()
    }

    fun updateStrategyPreset(
        presetName: String,
        weights: StrategyWeights,
        spreadPenalty: Int,
        budgetAllocation: BudgetAllocation
    ) {
        _settings.update {
            it.copy(
                selectedPresetName = presetName,
                strategyWeights = weights,
                creditsDistributionMultiplier = spreadPenalty.coerceIn(0, 20),
                budgetAllocation = budgetAllocation
            )
        }
        saveSettings()
    }

    fun updateStrategyWeights(weights: StrategyWeights) {
        _settings.update {
            it.copy(
                strategyWeights = weights,
                selectedPresetName = "Custom"
            )
        }
        saveSettings()
    }

    fun toggleGoalBonus(leagueId: Int, enabled: Boolean) {
        managerScope.launch {
            try {
                val updatedLeague = repository.updateGoalBonus(leagueId, enabled)
                _leagueDetails.update { updatedLeague }
                refreshAll()
                Log.d("AuctionSessionManager", "Successfully updated goal bonus to $enabled for league $leagueId")
            } catch (e: Exception) {
                Log.e("AuctionSessionManager", "Error updating goal bonus: ${e.message}")
            }
        }
    }

    fun updateLineUp(lineUp: LineUp) {
        _settings.update { it.copy(lineUp = lineUp) }
        saveSettings()
    }

    private fun saveSettings() {
        managerScope.launch {
            userPreferencesRepository.saveAuctionSettings(_settings.value)
        }
    }

    fun selectPlayerForBuy(player: Player) {
        _selectedPlayerForBuy.value = player
    }

    fun clearSelectedPlayerForBuy() {
        _selectedPlayerForBuy.value = null
    }
}
