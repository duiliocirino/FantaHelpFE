// com.example.fantahelpfe.ui.auction.AuctionBuyPlayersScreen.kt
package com.example.fantahelpfe.ui.auction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel // For hiltViewModel()
import androidx.navigation.NavController
import com.example.fantahelpfe.model.Player
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionBuyPlayersScreen(
    // Option 1: Pass NavController to get parent backStackEntry for shared VM
    navController: NavController, // If you have a nested graph for auction
    // Option 2: If sharedViewModel is directly injectable to this screen's scope (less common for cross-screen sharing)
    // buyPlayersViewModel: AuctionBuyPlayersViewModel = hiltViewModel(),
    sharedViewModel: AuctionSharedViewModel = hiltViewModel()
) {
    // --- ViewModel Injection Strategy ---
    val buyPlayersViewModel: AuctionBuyPlayersViewModel = hiltViewModel() // Scoped to this screen or its NavHostController

    val uiState by buyPlayersViewModel.uiState.collectAsState()
    val auctionSettings by sharedViewModel.settings.collectAsState() // Observe shared settings
    val focusManager = LocalFocusManager.current

    // --- LaunchedEffect to Reset Assignment Status ---
    LaunchedEffect(uiState.assignmentStatus) {
        if (uiState.assignmentStatus == ActionStatus.Success) {
            delay(2000)
            buyPlayersViewModel.resetAssignmentStatus()
        }
    }

    // --- LaunchedEffect to Trigger Convenience Score Calculation ---
    LaunchedEffect(
        uiState.selectedPlayer,
        uiState.selectedTeamForAssignment,
        uiState.lineUpForSuggestion,
        uiState.currentBidAmount,
        auctionSettings.globalFavoritePlayerIds, // From sharedViewModel
        auctionSettings.creditsDistributionMultiplier  // From sharedViewModel
    ) {
        val player = uiState.selectedPlayer
        val team = uiState.selectedTeamForAssignment
        val lineup = uiState.lineUpForSuggestion
        val bid = uiState.currentBidAmount

        // Only proceed if all necessary data is available
        if (player != null && team != null && lineup != null) {
            if (bid > 0) {
                // Apply a small debounce to avoid rapid firing if multiple states change quickly
                // (e.g., user typing bid amount AND shared settings changing)
                // This could also be handled by debouncing individual state flows within the VM
                // if they were all combined there.
                delay(300) // Small debounce directly in LaunchedEffect

                buyPlayersViewModel.calculateConvenienceScore(
                    playerToConsider = player,
                    bidForPlayer = bid,
                    teamForContext = team,
                    lineUp = lineup,
                    globalFavoritePlayerIds = auctionSettings.globalFavoritePlayerIds,
                    creditsDistributionMultiplier = auctionSettings.creditsDistributionMultiplier
                )
            } else {
                // If player is selected but bid is invalid, clear previous scores
                buyPlayersViewModel.clearConvenienceScores()
            }
        } else {
            // If essential selection (player, team, or lineup) is missing, clear scores
            buyPlayersViewModel.clearConvenienceScores()
        }
    }


    // --- Main UI Column ---
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        // --- League Name (Optional) ---
        uiState.leagueDetails?.let {
            Text(
                "League: ${it.name}", // Assuming your League model has 'name'
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )
        }
        if (uiState.lineUpForSuggestion == null && uiState.leagueDetails == null && !uiState.isLoadingTeams) {
            Text(
                "Loading league configuration...",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // --- Player Search and Selection ---
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = buyPlayersViewModel::onSearchQueryChanged,
            label = { Text("Search Player Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, "Search Icon") },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        buyPlayersViewModel.onSearchQueryChanged("")
                        buyPlayersViewModel.clearSelectedPlayer()
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Filled.Clear, "Clear Search")
                    }
                }
            },
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )

        if (uiState.isSearchingPlayers) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        } else if (uiState.searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp)
                    .padding(vertical = 4.dp)
            ) {
                items(uiState.searchResults, key = { it.id }) { player ->
                    Text(
                        text = "${player.name} (${player.role.name.capitalizeFirstLetter()})",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                buyPlayersViewModel.onPlayerSelectedFromSearch(player)
                                focusManager.clearFocus()
                            }
                            .padding(8.dp)
                    )
                    Divider()
                }
            }
        }

        uiState.selectedPlayer?.let { player ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Selected: ${player.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        // Assuming 'price' is the correct field on your Player model for its current value
                        "Role: ${player.role.name.capitalizeFirstLetter()}, Current Price: ${player.price}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Bid Amount ---
        if (uiState.selectedPlayer != null) {
            Text("Bid Amount:", style = MaterialTheme.typography.titleSmall)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { buyPlayersViewModel.adjustBid(-5) },
                    modifier = Modifier.weight(1f)
                ) { Text("-5") }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { buyPlayersViewModel.adjustBid(-1) },
                    modifier = Modifier.weight(1f)
                ) { Text("-1") }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = uiState.bidAmountInput,
                    onValueChange = buyPlayersViewModel::onBidAmountChanged,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.width(100.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { buyPlayersViewModel.adjustBid(1) },
                    modifier = Modifier.weight(1f)
                ) { Text("+1") }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { buyPlayersViewModel.adjustBid(5) },
                    modifier = Modifier.weight(1f)
                ) { Text("+5") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Team Assignment Dropdown ---
        if (uiState.selectedPlayer != null) {
            var expandedTeamDropdown by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedTeamDropdown,
                onExpandedChange = { expandedTeamDropdown = !expandedTeamDropdown },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.selectedTeamForAssignment?.name ?: "Select Team to Assign",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assign to Team") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTeamDropdown)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedTeamDropdown,
                    onDismissRequest = { expandedTeamDropdown = false }
                ) {
                    if (uiState.isLoadingTeams) {
                        DropdownMenuItem(
                            text = { Text("Loading teams...") },
                            onClick = {},
                            enabled = false
                        )
                    } else if (uiState.teamsInLeague.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No teams available/configured") },
                            onClick = {},
                            enabled = false
                        )
                    }
                    uiState.teamsInLeague.forEach { team ->
                        DropdownMenuItem(
                            text = { Text(team.name) },
                            onClick = {
                                buyPlayersViewModel.onTeamSelectedForAssignment(team)
                                expandedTeamDropdown = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Convenience Score Display ---
        if (uiState.selectedPlayer != null && uiState.currentBidAmount > 0 && uiState.selectedTeamForAssignment != null && uiState.lineUpForSuggestion != null) {
            when {
                uiState.isLoadingConvenienceScore -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(
                            " Calculating score advantage...",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                uiState.convenienceScoreError != null -> {
                    Text(
                        "Score Error: ${uiState.convenienceScoreError}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.baseOptimalScore != null && uiState.potentialOptimalScore != null -> {
                    val delta = uiState.convenienceScoreDelta ?: 0.0
                    val deltaColor = when {
                        delta > 1.0 -> Color(0xFF4CAF50) // Green
                        delta < -1.0 -> Color(0xFFF44336) // Red
                        else -> LocalContentColor.current
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Projected Optimal Score (Current Team): ${
                                String.format(
                                    "%.1f",
                                    uiState.baseOptimalScore!!
                                )
                            } pts", style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Projected Optimal Score (With ${uiState.selectedPlayer!!.name}): ${
                                String.format(
                                    "%.1f",
                                    uiState.potentialOptimalScore!!
                                )
                            } pts", style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Advantage: ${String.format("%+.1f", delta)} pts",
                            color = deltaColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f)) // Push button to bottom

        // --- Assign Button ---
        if (uiState.assignmentStatus == ActionStatus.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = buyPlayersViewModel::assignPlayer,
                enabled = uiState.selectedPlayer != null &&
                        uiState.selectedTeamForAssignment != null &&
                        uiState.currentBidAmount > 0 &&
                        uiState.assignmentStatus == ActionStatus.Idle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Assign Player")
            }
        }
        uiState.assignmentError?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
        if (uiState.assignmentStatus == ActionStatus.Success) {
            Text(
                "Player Assigned Successfully!",
                color = Color(0xFF4CAF50), // Green
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold
            )
        }

        // --- General Error ---
        uiState.generalError?.let {
            Text(
                "Error: $it",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}
