package com.example.fantahelpfe.ui.auction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionSettingsScreen(
    sharedViewModel: AuctionSharedViewModel // Injected via NavGraph
) {
    val settings by sharedViewModel.settings.collectAsState()
    var multiplierInput by remember { mutableStateOf(settings.creditsDistributionMultiplier.toString()) }
    var favoritePlayerIdInput by remember { mutableStateOf("") } // For adding new fav player ID

    // Update multiplierInput when settings change from VM
    LaunchedEffect(settings.creditsDistributionMultiplier) {
        multiplierInput = settings.creditsDistributionMultiplier.toString()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Auction Settings") }) }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Credits Distribution Multiplier", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = multiplierInput,
                onValueChange = {
                    multiplierInput = it
                    it.toIntOrNull()?.let { num ->
                        sharedViewModel.updateCreditsDistributionMultiplier(num)
                    }
                },
                label = { Text("Multiplier (e.g., 1, 5, 10)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Global Favorite Players", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = favoritePlayerIdInput,
                    onValueChange = { favoritePlayerIdInput = it },
                    label = { Text("Enter Player ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    favoritePlayerIdInput.toIntOrNull()?.let { id ->
                        sharedViewModel.addGlobalFavoritePlayer(id)
                        favoritePlayerIdInput = "" // Clear input
                    }
                }, modifier = Modifier.padding(start = 8.dp)) {
                    Text("Add Fav")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (settings.globalFavoritePlayerIds.isEmpty()) {
                Text("No global favorite players set.")
            } else {
                LazyColumn {
                    items(settings.globalFavoritePlayerIds, key = { it }) { playerId ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Player ID: $playerId")
                            IconButton(onClick = { sharedViewModel.removeGlobalFavoritePlayer(playerId) }) {
                                Icon(Icons.Filled.Delete, "Remove favorite")
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

