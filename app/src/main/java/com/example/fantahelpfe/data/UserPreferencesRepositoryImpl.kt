package com.example.fantahelpfe.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.fantahelpfe.data.remote.dto.BudgetAllocation
import com.example.fantahelpfe.data.remote.dto.LineUp
import com.example.fantahelpfe.data.remote.dto.StrategyWeights
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private val FAVORITE_PLAYERS_KEY = stringPreferencesKey("global_favorite_player_ids")
    private val MULTIPLIER_KEY = intPreferencesKey("credits_distribution_multiplier")
    private val LINEUP_DEFENDERS_KEY = intPreferencesKey("lineup_defenders")
    private val LINEUP_MIDFIELDERS_KEY = intPreferencesKey("lineup_midfielders")
    private val LINEUP_ATTACKERS_KEY = intPreferencesKey("lineup_attackers")

    private val STRATEGY_STARTER_WEIGHT_KEY = intPreferencesKey("strategy_starter_weight")
    private val STRATEGY_BENCH_WEIGHT_KEY = intPreferencesKey("strategy_bench_weight")
    private val STRATEGY_STRATEGY_WEIGHT_KEY = intPreferencesKey("strategy_strategy_weight")
    private val STRATEGY_SQUAD_DIVERSITY_KEY = intPreferencesKey("strategy_squad_diversity")
    private val STRATEGY_MATE_WEIGHT_KEY = intPreferencesKey("strategy_mate_weight")
    private val STRATEGY_RELIABILITY_WEIGHT_KEY = intPreferencesKey("strategy_reliability_weight")
    private val STRATEGY_SELECTED_PRESET_KEY = stringPreferencesKey("strategy_selected_preset")

    private val BUDGET_GK_KEY = doublePreferencesKey("budget_allocation_gk")
    private val BUDGET_DEF_KEY = doublePreferencesKey("budget_allocation_def")
    private val BUDGET_MID_KEY = doublePreferencesKey("budget_allocation_mid")
    private val BUDGET_ATT_KEY = doublePreferencesKey("budget_allocation_att")

    override val auctionSettingsFlow: Flow<AuctionSettings>
        get() = context.dataStore.data.map { prefs ->
            val favsString = prefs[FAVORITE_PLAYERS_KEY] ?: ""
            val favs = if (favsString.isEmpty()) {
                emptyList()
            } else {
                favsString.split(",").mapNotNull { it.toIntOrNull() }
            }
            val multiplier = prefs[MULTIPLIER_KEY] ?: 3
            val defenders = prefs[LINEUP_DEFENDERS_KEY] ?: 4
            val midfielders = prefs[LINEUP_MIDFIELDERS_KEY] ?: 3
            val attackers = prefs[LINEUP_ATTACKERS_KEY] ?: 3

            val weights = StrategyWeights(
                starterWeight = prefs[STRATEGY_STARTER_WEIGHT_KEY] ?: 6,
                benchWeight = prefs[STRATEGY_BENCH_WEIGHT_KEY] ?: 3,
                strategyWeight = prefs[STRATEGY_STRATEGY_WEIGHT_KEY] ?: 1,
                squadDiversity = prefs[STRATEGY_SQUAD_DIVERSITY_KEY] ?: 2,
                mateWeight = prefs[STRATEGY_MATE_WEIGHT_KEY] ?: 1,
                reliabilityWeight = prefs[STRATEGY_RELIABILITY_WEIGHT_KEY] ?: 10
            )
            val budget = BudgetAllocation(
                goalkeepers = prefs[BUDGET_GK_KEY] ?: 0.08,
                defenders = prefs[BUDGET_DEF_KEY] ?: 0.38,
                midfielders = prefs[BUDGET_MID_KEY] ?: 0.55,
                attackers = prefs[BUDGET_ATT_KEY] ?: 0.70
            )
            val presetName = prefs[STRATEGY_SELECTED_PRESET_KEY] ?: "Balanced"

            AuctionSettings(
                globalFavoritePlayerIds = favs,
                creditsDistributionMultiplier = multiplier,
                budgetAllocation = budget,
                lineUp = LineUp(defenders = defenders, midfielders = midfielders, attackers = attackers),
                strategyWeights = weights,
                selectedPresetName = presetName
            )
        }

    override suspend fun loadAuctionSettings(): AuctionSettings {
        val prefs = context.dataStore.data.first()
        val favsString = prefs[FAVORITE_PLAYERS_KEY] ?: ""
        val favs = if (favsString.isEmpty()) {
            emptyList()
        } else {
            favsString.split(",").mapNotNull { it.toIntOrNull() }
        }
        val multiplier = prefs[MULTIPLIER_KEY] ?: 3
        val defenders = prefs[LINEUP_DEFENDERS_KEY] ?: 4
        val midfielders = prefs[LINEUP_MIDFIELDERS_KEY] ?: 3
        val attackers = prefs[LINEUP_ATTACKERS_KEY] ?: 3

        val weights = StrategyWeights(
            starterWeight = prefs[STRATEGY_STARTER_WEIGHT_KEY] ?: 6,
            benchWeight = prefs[STRATEGY_BENCH_WEIGHT_KEY] ?: 3,
            strategyWeight = prefs[STRATEGY_STRATEGY_WEIGHT_KEY] ?: 1,
            squadDiversity = prefs[STRATEGY_SQUAD_DIVERSITY_KEY] ?: 2,
            mateWeight = prefs[STRATEGY_MATE_WEIGHT_KEY] ?: 1,
            reliabilityWeight = prefs[STRATEGY_RELIABILITY_WEIGHT_KEY] ?: 10
        )
        val budget = BudgetAllocation(
            goalkeepers = prefs[BUDGET_GK_KEY] ?: 0.08,
            defenders = prefs[BUDGET_DEF_KEY] ?: 0.38,
            midfielders = prefs[BUDGET_MID_KEY] ?: 0.55,
            attackers = prefs[BUDGET_ATT_KEY] ?: 0.70
        )
        val presetName = prefs[STRATEGY_SELECTED_PRESET_KEY] ?: "Balanced"

        return AuctionSettings(
            globalFavoritePlayerIds = favs,
            creditsDistributionMultiplier = multiplier,
            budgetAllocation = budget,
            lineUp = LineUp(defenders = defenders, midfielders = midfielders, attackers = attackers),
            strategyWeights = weights,
            selectedPresetName = presetName
        )
    }

    override suspend fun saveAuctionSettings(settings: AuctionSettings) {
        context.dataStore.edit { prefs ->
            prefs[FAVORITE_PLAYERS_KEY] = settings.globalFavoritePlayerIds.joinToString(",")
            prefs[MULTIPLIER_KEY] = settings.creditsDistributionMultiplier
            prefs[LINEUP_DEFENDERS_KEY] = settings.lineUp.defenders
            prefs[LINEUP_MIDFIELDERS_KEY] = settings.lineUp.midfielders
            prefs[LINEUP_ATTACKERS_KEY] = settings.lineUp.attackers
            prefs[STRATEGY_STARTER_WEIGHT_KEY] = settings.strategyWeights.starterWeight
            prefs[STRATEGY_BENCH_WEIGHT_KEY] = settings.strategyWeights.benchWeight
            prefs[STRATEGY_STRATEGY_WEIGHT_KEY] = settings.strategyWeights.strategyWeight
            prefs[STRATEGY_SQUAD_DIVERSITY_KEY] = settings.strategyWeights.squadDiversity
            prefs[STRATEGY_MATE_WEIGHT_KEY] = settings.strategyWeights.mateWeight
            prefs[STRATEGY_RELIABILITY_WEIGHT_KEY] = settings.strategyWeights.reliabilityWeight
            prefs[STRATEGY_SELECTED_PRESET_KEY] = settings.selectedPresetName
            prefs[BUDGET_GK_KEY] = settings.budgetAllocation.goalkeepers
            prefs[BUDGET_DEF_KEY] = settings.budgetAllocation.defenders
            prefs[BUDGET_MID_KEY] = settings.budgetAllocation.midfielders
            prefs[BUDGET_ATT_KEY] = settings.budgetAllocation.attackers
        }
    }
}