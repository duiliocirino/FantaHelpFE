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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.fantahelpfe.data.remote.dto.BudgetAllocation
import com.example.fantahelpfe.data.remote.dto.LineUp
import com.example.fantahelpfe.data.remote.dto.StrategyWeights
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.utils.capitalizeFirstLetter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionSettingsScreen(
    sharedViewModel: AuctionSharedViewModel
) {
    val settings by sharedViewModel.settings.collectAsState()
    val allPlayers by sharedViewModel.allPlayers.collectAsState()
    val leagueDetails by sharedViewModel.leagueDetails.collectAsState()

    var multiplierInput by remember { mutableStateOf(settings.creditsDistributionMultiplier.toString()) }
    var searchQuery by remember { mutableStateOf("") }
    var showCustomWeights by remember { mutableStateOf(false) }

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

    LazyColumn(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // League Rules & Scoring Settings Section (Goal Bonus Toggle)
            if (leagueDetails != null) {
                item {
                    val league = leagueDetails!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "🏆 League Rules: ${league.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Initial Budget: ${league.initialBudget} CR · ${league.teams.size} Teams",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        "⚽ Goal Bonus Per Role",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Extra bonus for goals scored by role (GK +6, DEF +5, MID +4, ATT +3). When active, market prices (+10% DEF, +5% MID) and performance values (+2% DEF, +3% MID) adjust automatically.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Switch(
                                    checked = league.goalBonusPerRole,
                                    onCheckedChange = { isChecked ->
                                        sharedViewModel.toggleGoalBonus(isChecked)
                                    }
                                )
                            }
                        }
                    }
                }
            }

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
                        LineUpPreset("4-3-3", LineUp(keepers = 1, defenders = 4, midfielders = 3, attackers = 3)),
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

            // Strategy Weights & Optimization Presets Section (BE Scoring Rework)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Strategy & Scoring Weights 🧠", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Choose a tactical strategy preset or fine-tune algorithmic objective knobs (0–10 scale).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                val strategyPresets = remember {
                    listOf(
                        StrategyPreset(
                            name = "Balanced",
                            title = "⚖️ Balanced (Recommended)",
                            spreadPenalty = 3,
                            weights = StrategyWeights(starterWeight = 6, benchWeight = 3, strategyWeight = 1, squadDiversity = 2, mateWeight = 1, reliabilityWeight = 10),
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.08, defenders = 0.38, midfielders = 0.55, attackers = 0.70),
                            description = "Expected-contribution optimum: balanced starting XI with solid depth across all 4 roles."
                        ),
                        StrategyPreset(
                            name = "Max Points",
                            title = "🚀 Max Points (Pure Quality)",
                            spreadPenalty = 1,
                            weights = StrategyWeights(starterWeight = 7, benchWeight = 2, strategyWeight = 1, squadDiversity = 1, mateWeight = 1, reliabilityWeight = 0),
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.06, defenders = 0.32, midfielders = 0.50, attackers = 0.75),
                            description = "Pure quality-when-playing: maximizes theoretical point ceiling without reliability discount."
                        ),
                        StrategyPreset(
                            name = "Star Line",
                            title = "🌟 Star Line (Starters Focus)",
                            spreadPenalty = 0,
                            weights = StrategyWeights(starterWeight = 8, benchWeight = 2, strategyWeight = 2, squadDiversity = 2, mateWeight = 1, reliabilityWeight = 10),
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.05, defenders = 0.30, midfielders = 0.45, attackers = 0.85),
                            description = "Line-biased focus: prioritizes elite top-tier forwards; accepts a thinner rotation bench."
                        ),
                        StrategyPreset(
                            name = "Deep Bench",
                            title = "🛡️ Deep Bench (Squad Depth)",
                            spreadPenalty = 5,
                            weights = StrategyWeights(starterWeight = 4, benchWeight = 8, strategyWeight = 2, squadDiversity = 2, mateWeight = 1, reliabilityWeight = 10),
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.10, defenders = 0.48, midfielders = 0.65, attackers = 0.60),
                            description = "Bench-biased depth: redistributes credits to guarantee a reliable 25-man squad."
                        ),
                        StrategyPreset(
                            name = "Diversified",
                            title = "🌐 Diversified (Club Diversity)",
                            spreadPenalty = 3,
                            weights = StrategyWeights(starterWeight = 6, benchWeight = 3, strategyWeight = 3, squadDiversity = 10, mateWeight = 1, reliabilityWeight = 10),
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.08, defenders = 0.38, midfielders = 0.55, attackers = 0.70),
                            description = "Club diversification: strongly penalizes clustering multiple players from the same Serie A club."
                        ),
                        StrategyPreset(
                            name = "Mate Collector",
                            title = "🤝 Mate Collector (Teammate Pairs)",
                            spreadPenalty = 3,
                            weights = StrategyWeights(starterWeight = 6, benchWeight = 3, strategyWeight = 3, squadDiversity = 2, mateWeight = 10, reliabilityWeight = 10),
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.08, defenders = 0.38, midfielders = 0.55, attackers = 0.70),
                            description = "Club pairings focus: prioritizes buying teammate / mate pairs for insurance on the bench."
                        ),
                        StrategyPreset(
                            name = "Tempered",
                            title = "⚖️ Tempered (Half-Reliability)",
                            spreadPenalty = 3,
                            weights = StrategyWeights(starterWeight = 6, benchWeight = 3, strategyWeight = 1, squadDiversity = 2, mateWeight = 1, reliabilityWeight = 5),
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.08, defenders = 0.38, midfielders = 0.55, attackers = 0.70),
                            description = "Half-strength reliability discount: moderate balance between upside quality and starter regularity."
                        ),
                        StrategyPreset(
                            name = "Mate & Variety",
                            title = "🎭 Mate & Variety (Hybrid)",
                            spreadPenalty = 3,
                            weights = StrategyWeights(starterWeight = 6, benchWeight = 3, strategyWeight = 3, squadDiversity = 5, mateWeight = 5, reliabilityWeight = 10),
                            budgetAllocation = BudgetAllocation(goalkeepers = 0.08, defenders = 0.38, midfielders = 0.55, attackers = 0.70),
                            description = "Balanced hybrid: combines mild mate pairings with moderate club diversification."
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    strategyPresets.forEach { preset ->
                        val isSelected = settings.selectedPresetName == preset.name &&
                                settings.strategyWeights == preset.weights &&
                                settings.creditsDistributionMultiplier == preset.spreadPenalty &&
                                settings.budgetAllocation == preset.budgetAllocation

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sharedViewModel.updateStrategyPreset(
                                        presetName = preset.name,
                                        weights = preset.weights,
                                        spreadPenalty = preset.spreadPenalty,
                                        budgetAllocation = preset.budgetAllocation
                                    )
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
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
                                            "S/B/T: ${preset.weights.starterWeight}/${preset.weights.benchWeight}/${preset.weights.strategyWeight}",
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
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    WeightPill("Rel: ${preset.weights.reliabilityWeight}/10", Color(0xFF1976D2))
                                    WeightPill("Div: ${preset.weights.squadDiversity}/10", Color(0xFF388E3C))
                                    WeightPill("Mate: ${preset.weights.mateWeight}/10", Color(0xFFF57F17))
                                    WeightPill("Spread: ${preset.spreadPenalty}", Color(0xFF7B1FA2))
                                }

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

            // Expandable Custom Tuning Section (Weights, Spread, & Budget Percentages)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCustomWeights = !showCustomWeights },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, contentDescription = "Tuning", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Custom Knobs & Strategy Tuning",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (settings.selectedPresetName == "Custom") "Active Mode: Custom Settings" else "Active Preset: ${settings.selectedPresetName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showCustomWeights) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Custom Tuning"
                            )
                        }

                        AnimatedVisibility(visible = showCustomWeights) {
                            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // 1. Spread Parameter (creditsDistributionMultiplier)
                                Text(
                                    "💰 Credits Spread Penalty (Variance Multiplier)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                ValueSlider(
                                    label = "Spread Multiplier",
                                    value = settings.creditsDistributionMultiplier,
                                    range = 0f..10f,
                                    suffix = "",
                                    description = "Penalizes price gaps between starters & fillers (0 = superstar heavy / 1-CR fillers, 10 = uniform squad spend)",
                                    onValueChange = { sharedViewModel.updateCreditsDistributionMultiplier(it) }
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                                // 2. Budget Allocation Role Caps (%)
                                val currentBudget = settings.budgetAllocation
                                Text(
                                    "📊 Budget Allocation Role Caps (% of Total Budget)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                ValueSlider(
                                    label = "Goalkeepers Cap (GK)",
                                    value = (currentBudget.goalkeepers * 100).toInt(),
                                    range = 2f..25f,
                                    suffix = "%",
                                    description = "Maximum percentage of budget allowed for goalkeepers",
                                    onValueChange = {
                                        sharedViewModel.updateBudgetAllocation(currentBudget.copy(goalkeepers = it / 100.0))
                                    }
                                )

                                ValueSlider(
                                    label = "Defenders Cap (DEF)",
                                    value = (currentBudget.defenders * 100).toInt(),
                                    range = 10f..65f,
                                    suffix = "%",
                                    description = "Maximum percentage of budget allowed for defenders",
                                    onValueChange = {
                                        sharedViewModel.updateBudgetAllocation(currentBudget.copy(defenders = it / 100.0))
                                    }
                                )

                                ValueSlider(
                                    label = "Midfielders Cap (MID)",
                                    value = (currentBudget.midfielders * 100).toInt(),
                                    range = 20f..80f,
                                    suffix = "%",
                                    description = "Maximum percentage of budget allowed for midfielders",
                                    onValueChange = {
                                        sharedViewModel.updateBudgetAllocation(currentBudget.copy(midfielders = it / 100.0))
                                    }
                                )

                                ValueSlider(
                                    label = "Attackers Cap (ATT)",
                                    value = (currentBudget.attackers * 100).toInt(),
                                    range = 30f..95f,
                                    suffix = "%",
                                    description = "Maximum percentage of budget allowed for forwards / strikers",
                                    onValueChange = {
                                        sharedViewModel.updateBudgetAllocation(currentBudget.copy(attackers = it / 100.0))
                                    }
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                                // 3. Strategy Objective Weights (0–10)
                                val currentWeights = settings.strategyWeights
                                Text(
                                    "🧠 Algorithmic Objective Weights (0–10 Scale)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                ValueSlider(
                                    label = "Starter Weight (S)",
                                    value = currentWeights.starterWeight,
                                    range = 0f..10f,
                                    suffix = " / 10",
                                    description = "Weight on starting XI players' reliable performance",
                                    onValueChange = { sharedViewModel.updateStrategyWeights(currentWeights.copy(starterWeight = it)) }
                                )

                                ValueSlider(
                                    label = "Bench Depth Weight (B)",
                                    value = currentWeights.benchWeight,
                                    range = 0f..10f,
                                    suffix = " / 10",
                                    description = "Weight on 0.5x bench rotation performance",
                                    onValueChange = { sharedViewModel.updateStrategyWeights(currentWeights.copy(benchWeight = it)) }
                                )

                                ValueSlider(
                                    label = "Strategy & Diversity Block (T)",
                                    value = currentWeights.strategyWeight,
                                    range = 0f..10f,
                                    suffix = " / 10",
                                    description = "Weight on tactical bonuses (mates, club diversity)",
                                    onValueChange = { sharedViewModel.updateStrategyWeights(currentWeights.copy(strategyWeight = it)) }
                                )

                                ValueSlider(
                                    label = "Squad Club Diversity",
                                    value = currentWeights.squadDiversity,
                                    range = 0f..10f,
                                    suffix = " / 10",
                                    description = "Penalty strength for stacking players from same club",
                                    onValueChange = { sharedViewModel.updateStrategyWeights(currentWeights.copy(squadDiversity = it)) }
                                )

                                ValueSlider(
                                    label = "Mate Pairings Weight",
                                    value = currentWeights.mateWeight,
                                    range = 0f..10f,
                                    suffix = " / 10",
                                    description = "Bonus for having club teammates on the bench",
                                    onValueChange = { sharedViewModel.updateStrategyWeights(currentWeights.copy(mateWeight = it)) }
                                )

                                ValueSlider(
                                    label = "Reliability Discount (Rel)",
                                    value = currentWeights.reliabilityWeight,
                                    range = 0f..10f,
                                    suffix = " / 10",
                                    description = "10 = full expected starting discount, 0 = pure quality ceiling",
                                    onValueChange = { sharedViewModel.updateStrategyWeights(currentWeights.copy(reliabilityWeight = it)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun ValueSlider(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    suffix: String = "",
    description: String,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(
                "$value$suffix",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val stepsCount = ((range.endInclusive - range.start).toInt() - 1).coerceAtLeast(0)
        Slider(
            value = value.toFloat().coerceIn(range.start, range.endInclusive),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range,
            steps = stepsCount
        )
    }
}

@Composable
private fun WeightPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

private data class LineUpPreset(
    val name: String,
    val lineUp: LineUp
)

private data class StrategyPreset(
    val name: String,
    val title: String,
    val spreadPenalty: Int,
    val weights: StrategyWeights,
    val budgetAllocation: BudgetAllocation,
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
