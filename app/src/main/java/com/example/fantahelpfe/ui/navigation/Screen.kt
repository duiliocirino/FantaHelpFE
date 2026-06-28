package com.example.fantahelpfe.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home_screen") // Your old new/load game page
    data object CreateLeague : Screen("create_league_screen")
    data object LoadLeague : Screen("load_league_screen")
    data object AuctionFlow : Screen("auction_flow/{leagueId}") {
        const val NAV_ARG_LEAGUE_ID = "leagueId" // For consistency
        fun createRoute(leagueId: Int) = "auction_flow/$leagueId"
    }
    data object AuctionBuyPlayers : Screen("auction_buy_players")
    data object AuctionSettings : Screen("auction_settings")
    data object LeagueTeams : Screen("league_teams_screen/{leagueId}") {
        fun createRoute(leagueId: Int) = "league_teams_screen/$leagueId"
    }
    // Add other screens like Settings, PlayerDetails, etc.
}