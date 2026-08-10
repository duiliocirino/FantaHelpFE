package com.example.fantahelpfe.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.fantahelpfe.data.remote.dto.LineUp
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

    override val auctionSettingsFlow: Flow<AuctionSettings>
        get() = context.dataStore.data.map { prefs ->
            val favsString = prefs[FAVORITE_PLAYERS_KEY] ?: ""
            val favs = if (favsString.isEmpty()) {
                emptyList()
            } else {
                favsString.split(",").mapNotNull { it.toIntOrNull() }
            }
            val multiplier = prefs[MULTIPLIER_KEY] ?: 2
            val defenders = prefs[LINEUP_DEFENDERS_KEY] ?: 4
            val midfielders = prefs[LINEUP_MIDFIELDERS_KEY] ?: 3
            val attackers = prefs[LINEUP_ATTACKERS_KEY] ?: 3

            AuctionSettings(
                globalFavoritePlayerIds = favs,
                creditsDistributionMultiplier = multiplier,
                lineUp = LineUp(defenders = defenders, midfielders = midfielders, attackers = attackers)
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
        val multiplier = prefs[MULTIPLIER_KEY] ?: 2
        val defenders = prefs[LINEUP_DEFENDERS_KEY] ?: 4
        val midfielders = prefs[LINEUP_MIDFIELDERS_KEY] ?: 3
        val attackers = prefs[LINEUP_ATTACKERS_KEY] ?: 3

        return AuctionSettings(
            globalFavoritePlayerIds = favs,
            creditsDistributionMultiplier = multiplier,
            lineUp = LineUp(defenders = defenders, midfielders = midfielders, attackers = attackers)
        )
    }

    override suspend fun saveAuctionSettings(settings: AuctionSettings) {
        context.dataStore.edit { prefs ->
            prefs[FAVORITE_PLAYERS_KEY] = settings.globalFavoritePlayerIds.joinToString(",")
            prefs[MULTIPLIER_KEY] = settings.creditsDistributionMultiplier
            prefs[LINEUP_DEFENDERS_KEY] = settings.lineUp.defenders
            prefs[LINEUP_MIDFIELDERS_KEY] = settings.lineUp.midfielders
            prefs[LINEUP_ATTACKERS_KEY] = settings.lineUp.attackers
        }
    }
}