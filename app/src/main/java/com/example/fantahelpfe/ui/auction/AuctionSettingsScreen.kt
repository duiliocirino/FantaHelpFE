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
import androidx.compose.material3.FilterChip
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
import com.example.fantahelpfe.data.remote.dto.LineUp
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
            it.name.contains(searchQuery, ignoreCase = true) || it.squad.contains(searchQuery, ignoreCase = true)
        }.take(6)
    }

    // Map favorite IDs to Player objects
    val favoritePlayers = remember(settings.globalFavoritePlayerIds, allPlayers) {
        val playerMap = allPlayers.associateBy { it.id }
        settings.globalFavoritePlayerIds.map { id ->
            playerMap[id] ?: Player(
                id = id,
                name = "Player #$id",
                squad = "Unknown",
                role = com.example.fantahelpfe.model.PlayerRole.UNKNOWN,
                price = 0,
                rating = 0.0,
                regularness = 0,
                fvm = 0,
                expectedPerformance = 0.0,
                expectedStd = 0.0,
                expectedPrice = 0
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
                                        Text(" (${player.squad})", style = MaterialTheme.typography.bodySmall)
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
                                    Text("${player.squad} · Price: ${player.price} CR", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { sharedViewModel.removeGlobalFavoritePlayer(player.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Favorite", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // LineUp Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Target Formation LineUp ⚽", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Select a standard fantasy football formation preset.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                val formationPresets = remember {
                    listOf(
                        LineUpPreset("4-3-3",
                            LineUp(keepers = 1, defenders = 4, midfielders = 3, attackers = 3)
                        ),
                        LineUpPreset("3-4-3", LineUp(keepers = 1, defenders = 3, midfielders = 4, attackers = 3)),
                        LineUpPreset("4-4-2", LineUp(keepers = 1, defenders = 4, midfielders = 4, attackers = 2)),
                        LineUpPreset("3-5-2", LineUp(keepers = 1, defenders = 3, midfielders = 5, attackers = 2)),
                        LineUpPreset("4-5-1", LineUp(keepers = 1, defenders = 4, midfielders = 5, attackers = 1)),
                        LineUpPreset("5-3-2", LineUp(keepers = 1, defenders = 5, midfielders = 3, attackers = 2)),
                        LineUpPreset("5-4-1", LineUp(keepers = 1, defenders = 5, midfielders = 4, attackers = 1))
                    )
                }

                var isCustomLineup by remember { mutableStateOf(false) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        formationPresets.take(4).forEach { preset ->
                            val isSelected = !isCustomLineup && settings.lineUp == preset.lineUp
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    isCustomLineup = false
                                    sharedViewModel.updateLineUp(preset.lineUp)
                                },
                                label = { Text(preset.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        formationPresets.drop(4).forEach { preset ->
                            val isSelected = !isCustomLineup && settings.lineUp == preset.lineUp
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    isCustomLineup = false
                                    sharedViewModel.updateLineUp(preset.lineUp)
                                },
                                label = { Text(preset.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        FilterChip(
                            selected = isCustomLineup,
                            onClick = { isCustomLineup = !isCustomLineup },
                            label = { Text("Custom", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isCustomLineup) {
                        Spacer(modifier = Modifier.height(4.dp))
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

            // Multiplier Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Credits Budget Strategy 💰", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Choose how AI algorithms distribute credits and penalize price variance across starters.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val strategyPresets = remember {
                    listOf(
                        StrategyPreset(
                            title = "🌟 Stars & Fillers (All-In Strikers)",
                            spreadPenalty = 0,
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.06, defenders = 0.25, midfielders = 0.50, attackers = 0.80),
                            description = "No variance penalty. Allows buying top-tier expensive superstars alongside cheap 1-CR fillers."
                        ),
                        StrategyPreset(
                            title = "⚡ Star-Oriented (Aggressive Attack)",
                            spreadPenalty = 1,
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.08, defenders = 0.25, midfielders = 0.55, attackers = 0.70),
                            description = "Low variance penalty. Prioritizes high-scoring forwards while keeping slight spending balance."
                        ),
                        StrategyPreset(
                            title = "⚖️ Standard Balanced (Recommended)",
                            spreadPenalty = 2,
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.10, defenders = 0.30, midfielders = 0.60, attackers = 0.60),
                            description = "Default strategy balancing top starters with solid depth across all 4 squad roles."
                        ),
                        StrategyPreset(
                            title = "🛡️ Defense Modifier & Solid Depth",
                            spreadPenalty = 3,
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.12, defenders = 0.40, midfielders = 0.65, attackers = 0.50),
                            description = "Allocates more budget to defenders and midfielders for defense modifier points and depth."
                        ),
                        StrategyPreset(
                            title = "🏦 Uniform Price Spread (Low Risk)",
                            spreadPenalty = 5,
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.15, defenders = 0.45, midfielders = 0.60, attackers = 0.45),
                            description = "Aggressively penalizes price gaps within roles, building a uniform, deep 25-man squad."
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    strategyPresets.forEach { preset ->
                        val isSelected = settings.creditsDistributionMultiplier == preset.spreadPenalty &&
                                settings.budgetAllocation == preset.budgetAllocation
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sharedViewModel.updateCreditsDistributionStrategy(preset.spreadPenalty, preset.budgetAllocation)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        preset.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            "Spread: ${preset.spreadPenalty}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    preset.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Caps ➔ GK: ${(preset.budgetAllocation.goalkeepers * 100).toInt()}% · DEF: ${(preset.budgetAllocation.defenders * 100).toInt()}% · MID: ${(preset.budgetAllocation.midfielders * 100).toInt()}% · ATT: ${(preset.budgetAllocation.attackers * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LineUpPreset(
    val name: String,
    val lineUp: LineUp
)

private data class StrategyPreset(
    val title: String,
    val spreadPenalty: Int,
    val budgetAllocation: com.example.fantahelpfe.data.remote.dto.BudgetAllocation,
    val description: String
)

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
