package com.example.fantahelpfe.ui.auction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.model.PlayerRole // Ensure this is imported

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionScreen(
    navController: NavHostController, // Not used yet, but good to have for future navigation
    leagueId: Int, // Passed directly now, ViewModel gets it from SavedStateHandle
    viewModel: AuctionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val leagueName = (uiState as? AuctionScreenUiState.Success)?.leagueDetails?.name
                    Text(text = leagueName ?: "Auction") // Show league name if available
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is AuctionScreenUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AuctionScreenUiState.Success -> {
                    AvailablePlayersList(players = state.availablePlayers)
                }
                is AuctionScreenUiState.NoPlayersAvailable -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No players currently available for auction.")
                        // Maybe a button to "Refresh" or other actions
                    }
                }
                is AuctionScreenUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.fetchAvailablePlayers() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AvailablePlayersList(players: List<Player>) {
    if (players.isEmpty()) { // This check might be redundant if NoPlayersAvailable state is handled
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No players available at the moment.")
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(players, key = { player -> player.id }) { player ->
            PlayerAuctionItem(player = player)
        }
    }
}

@Composable
fun PlayerAuctionItem(player: Player) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Role: ${player.role.name.capitalizeFirstLetter()}", // Helper extension needed
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Price: ${player.price}", // Or initialPrice for auction start
                    style = MaterialTheme.typography.bodyMedium
                )
                // Add other relevant player info: nationality, age (if available), etc.
            }
            // Placeholder for Bid button or other actions in the future
            Button(onClick = { /* TODO: Handle bid/draft action */ }) {
                Text("Bid") // Placeholder
            }
        }
    }
}

// Helper extension function (place in a utility file)
fun String.capitalizeFirstLetter(): String {
    return this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}