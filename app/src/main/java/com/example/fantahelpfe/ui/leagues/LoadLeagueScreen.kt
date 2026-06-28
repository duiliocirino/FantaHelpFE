package com.example.fantahelpfe.ui.leagues

import com.example.fantahelpfe.ui.leagues.LeaguesUiState
import com.example.fantahelpfe.ui.leagues.LoadLeaguesViewModel

import androidx.compose.foundation.clickable
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
import androidx.navigation.NavController
import com.example.fantahelpfe.model.League
import com.example.fantahelpfe.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadLeagueScreen(
    navController: NavController,
    viewModel: LoadLeaguesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("FantaHelp") }) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                is LeaguesUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is LeaguesUiState.Success -> {
                    LeagueList(
                        leagues = state.leagues,
                        onLeagueClick = { leagueId ->
                            navController.navigate(Screen.AuctionFlow.createRoute(leagueId))
                        }
                    )
                }
                is LeaguesUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun LeagueList(leagues: List<League>, onLeagueClick: (Int) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Select a League", style = MaterialTheme.typography.headlineSmall)
        }
        items(leagues) { league ->
            LeagueListItem(league = league, onClick = { onLeagueClick(league.id) })
        }
    }
}

@Composable
fun LeagueListItem(league: League, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = league.name, style = MaterialTheme.typography.titleLarge)
            Text(text = "Budget: ${league.initialBudget}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}