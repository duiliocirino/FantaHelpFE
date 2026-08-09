package com.example.fantahelpfe.ui.auction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.utils.capitalizeFirstLetter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionSettingsScreen(
    sharedViewModel: AuctionSharedViewModel
) {
    val settings by sharedViewModel.settings.collectAsState()
    val allPlayers by sharedViewModel.allPlayers.collectAsState()

    var multiplierInput by remember { mutableStateOf(settings.creditsDistributionMultiplier.toString()) }
    var searchQuery by remember { mutableStateOf("") }

    // LineUp inputs
    var keepersInput by remember { mutableStateOf(settings.lineUp.keepers.toString()) }
    var defendersInput by remember { mutableStateOf(settings.lineUp.defenders.toString()) }
    var midfieldersInput by remember { mutableStateOf(settings.lineUp.midfielders.toString()) }
    var attackersInput by remember { mutableStateOf(settings.lineUp.attackers.toString()) }

    LaunchedEffect(settings.creditsDistributionMultiplier) {
        multiplierInput = settings.creditsDistributionMultiplier.toString()
    }

    LaunchedEffect(settings.lineUp) {
        keepersInput = settings.lineUp.keepers.toString()
        defendersInput = settings.lineUp.defenders.toString()
        midfieldersInput = settings.lineUp.midfielders.toString()
        attackersInput = settings.lineUp.attackers.toString()
    }

    // Filter matching search suggestions
    val searchResults = remember(searchQuery, allPlayers) {
        if (searchQuery.isBlank()) emptyList()
        else allPlayers.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.realTeam.contains(searchQuery, ignoreCase = true)
        }.take(6)
    }

    // Map favorite IDs to Player objects
    val favoritePlayers = remember(settings.globalFavoritePlayerIds, allPlayers) {
        val playerMap = allPlayers.associateBy { it.id }
        settings.globalFavoritePlayerIds.map { id ->
            playerMap[id] ?: Player(
                id = id,
                name = "Player #$id",
                realTeam = "Unknown",
                role = com.example.fantahelpfe.model.PlayerRole.UNKNOWN,
                price = 0,
                rating = 0.0,
                expectedPerformance = 0.0
            )
        }
    }

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Global Favorite Players Section
            item {
                Text("Global Favorite Players ★", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Search player by name to add to favorites.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Player by Name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Search Overlay Suggestions
                AnimatedVisibility(visible = searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            searchResults.forEach { player ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            sharedViewModel.addGlobalFavoritePlayer(player.id)
                                            searchQuery = ""
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RoleBadge(roleName = player.role.name)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(player.name, fontWeight = FontWeight.Bold)
                                        Text(" (${player.realTeam})", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.Default.Add, contentDescription = "Add Favorite", tint = MaterialTheme.colorScheme.primary)
                                }
                                Divider()
                            }
                        }
                    }
                }
            }

            // Favorites List
            if (favoritePlayers.isEmpty()) {
                item {
                    Text("No global favorite players set.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(favoritePlayers, key = { it.id }) { player ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RoleBadge(roleName = player.role.name)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(player.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text("${player.realTeam} · Price: ${player.price} CR", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { sharedViewModel.removeGlobalFavoritePlayer(player.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Favorite", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Multiplier Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Credits Distribution Multiplier", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            }

            // LineUp Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("LineUp Target Roster", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = keepersInput,
                        onValueChange = {
                            keepersInput = it
                            it.toIntOrNull()?.let { num -> sharedViewModel.updateLineUp(settings.lineUp.copy(keepers = num)) }
                        },
                        label = { Text("GK") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = defendersInput,
                        onValueChange = {
                            defendersInput = it
                            it.toIntOrNull()?.let { num -> sharedViewModel.updateLineUp(settings.lineUp.copy(defenders = num)) }
                        },
                        label = { Text("DEF") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = midfieldersInput,
                        onValueChange = {
                            midfieldersInput = it
                            it.toIntOrNull()?.let { num -> sharedViewModel.updateLineUp(settings.lineUp.copy(midfielders = num)) }
                        },
                        label = { Text("MID") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = attackersInput,
                        onValueChange = {
                            attackersInput = it
                            it.toIntOrNull()?.let { num -> sharedViewModel.updateLineUp(settings.lineUp.copy(attackers = num)) }
                        },
                        label = { Text("ATT") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(roleName: String) {
    val (badgeColor, textColor) = when (roleName.uppercase()) {
        "P" -> Color(0xFFFFB74D) to Color.Black
        "D" -> Color(0xFF64B5F6) to Color.Black
        "C" -> Color(0xFF81C784) to Color.Black
        "A" -> Color(0xE5E57373) to Color.White
        else -> Color.Gray to Color.White
    }
    Surface(
        color = badgeColor,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = roleName.capitalizeFirstLetter(),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
