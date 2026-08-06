package com.example.fantahelpfe.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.fantahelpfe.ui.auction.AuctionSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private val FAVORITE_PLAYERS_KEY = stringPreferencesKey("global_favorite_player_ids")
    private val MULTIPLIER_KEY = intPreferencesKey("credits_distribution_multiplier")

    override suspend fun loadAuctionSettings(): AuctionSettings {
        val prefs = context.dataStore.data.first()
        val favsString = prefs[FAVORITE_PLAYERS_KEY] ?: ""
        val favs = if (favsString.isEmpty()) {
            emptyList()
        } else {
            favsString.split(",").mapNotNull { it.toIntOrNull() }
        }
        val multiplier = prefs[MULTIPLIER_KEY] ?: 1

        return AuctionSettings(
            globalFavoritePlayerIds = favs,
            creditsDistributionMultiplier = multiplier
        )
    }

    override suspend fun saveAuctionSettings(settings: AuctionSettings) {
        context.dataStore.edit { prefs ->
            prefs[FAVORITE_PLAYERS_KEY] = settings.globalFavoritePlayerIds.joinToString(",")
            prefs[MULTIPLIER_KEY] = settings.creditsDistributionMultiplier
        }
    }
}