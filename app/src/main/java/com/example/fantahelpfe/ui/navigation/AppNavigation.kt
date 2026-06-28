package com.example.fantahelpfe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fantahelpfe.ui.auction.AuctionBuyPlayersViewModel
import com.example.fantahelpfe.ui.auction.AuctionScreen
import com.example.fantahelpfe.ui.auction.AuctionBuyPlayersScreen
import com.example.fantahelpfe.ui.auction.AuctionSettingsScreen
import com.example.fantahelpfe.ui.auction.AuctionSharedViewModel
import com.example.fantahelpfe.ui.home.HomeScreen
import com.example.fantahelpfe.ui.leagues.CreateLeagueScreen
import com.example.fantahelpfe.ui.leagues.LoadLeagueScreen
import com.example.fantahelpfe.ui.teams.LeagueTeamsScreen

@Composable
fun AppNavigation() {
    // 1. Create the NavController
    val navController = rememberNavController()

    // 2. Create the NavHost, which is the stage
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route // Set the starting screen
    ) {
        // 3. Define each screen in the graph (the script)
        composable(route = Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.CreateLeague.route) {
            CreateLeagueScreen(navController = navController) // Pass the same navController or specific callbacks
        }
        composable(Screen.LoadLeague.route) {
            LoadLeagueScreen(navController = navController)
        }
        // --- Define the Auction Nested Navigation Graph ---
        // This is triggered when navigating to Screen.AuctionFlow.createRoute(leagueId)
        navigation(
            // The route for this entire nested graph. It includes the leagueId argument.
            route = Screen.AuctionFlow.route,
            // The starting screen *within* this auction graph.
            startDestination = Screen.AuctionBuyPlayers.route,
            arguments = listOf(navArgument(Screen.AuctionFlow.NAV_ARG_LEAGUE_ID) { type = NavType.IntType })
        ) {
            // 'this' NavGraphBuilder is now for the "auction_flow_graph"
            // The leagueId from Screen.AuctionFlow.route is available to all composables in this graph
            // via backStackEntry.arguments or more easily via the AuctionSharedViewModel.

            // Composable for the "Buy Players" screen within the auction graph
            composable(Screen.AuctionBuyPlayers.route) {
                // Get the shared ViewModel scoped to this nested graph ("auction_flow_graph")
                val sharedViewModel: AuctionSharedViewModel = hiltViewModel(
                    remember { navController.getBackStackEntry(Screen.AuctionFlow.route) } // Use the graph's route
                )
                // Initialize the shared VM with leagueId if it's not already set or needs it
                val arguments = navController.currentBackStackEntry?.arguments
                val leagueIdFromNav = arguments?.getInt(Screen.AuctionFlow.NAV_ARG_LEAGUE_ID)
                LaunchedEffect(leagueIdFromNav, sharedViewModel) {
                    if (leagueIdFromNav != null) {
                        sharedViewModel.initializeForLeague(leagueIdFromNav) // Add this method to AuctionSharedViewModel
                    }
                }

                AuctionBuyPlayersScreen(
                    navController = navController, // Pass if needed for internal navigation within auction tabs
                    sharedViewModel = sharedViewModel
                    // AuctionBuyPlayersViewModel is injected via hiltViewModel() inside the screen
                )
            }

            // Composable for the "Auction Settings" screen within the auction graph
            composable(Screen.AuctionSettings.route) {
                val sharedViewModel: AuctionSharedViewModel = hiltViewModel(
                    remember { navController.getBackStackEntry(Screen.AuctionFlow.route) }
                )
                // You might not need to explicitly initialize leagueId here if it's already done
                // when entering the graph, unless settings screen specifically needs it.
                AuctionSettingsScreen(sharedViewModel = sharedViewModel)
            }

            // TODO: Add composable entries for your other 3 auction screens here
            // They will all share the same AuctionSharedViewModel instance
            /*
            composable(Screen.AuctionMyTeam.route) {
                val sharedViewModel: AuctionSharedViewModel = hiltViewModel(
                    remember { navController.getBackStackEntry(Screen.AuctionFlow.route) }
                )
                AuctionMyTeamScreen(sharedViewModel = sharedViewModel, ...)
            }
            composable(Screen.AuctionMarket.route) { ... }
            composable(Screen.AuctionLiveBids.route) { ... }
            */
        }

        composable(
            route = Screen.LeagueTeams.route,
            arguments = listOf(navArgument("leagueId") { type = NavType.IntType })
        ) { backStackEntry ->
            val leagueId = backStackEntry.arguments?.getInt("leagueId")
            if (leagueId != null) {
                LeagueTeamsScreen(navController = navController, leagueId = leagueId)
            }
        }
    }
}