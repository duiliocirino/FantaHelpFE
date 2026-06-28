package com.example.fantahelpfe.ui.players

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fantahelpfe.model.Player

@Composable
fun PlayersScreen(
    viewModel: PlayersViewModel = hiltViewModel()
) {
    // Collect the state from the ViewModel.
    // Whenever uiState changes, this Composable will automatically "recompose" (redraw).
    val uiState by viewModel.uiState.collectAsState()

    // A Surface is a basic container from Material Design 3
    Surface(modifier = Modifier.fillMaxSize()) {
        // We use a `when` statement to handle the different states
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                // Show a loading spinner
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PlayerUiState.Success -> {
                // Show the list of players
                PlayerList(players = state.players)
            }
            is PlayerUiState.Error -> {
                // Show an error message
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun PlayerList(players: List<Player>) {
    // LazyColumn is Compose's efficient way to display long, scrollable lists.
    // It's the equivalent of RecyclerView.
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(players) { player ->
            PlayerListItem(player = player)
        }
    }
}

@Composable
fun PlayerListItem(player: Player) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = player.name, style = MaterialTheme.typography.titleMedium)
                Text(text = player.squad, style = MaterialTheme.typography.bodySmall)
            }
            Text(text = player.role.name, style = MaterialTheme.typography.bodyLarge)
        }
    }
}