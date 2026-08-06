# FantaHelpFE - Action Plan to Auction-Ready State

## Goal
Make the frontend fully usable for conducting a fantasy football auction. The user should be able to:
- Select a league and enter the auction
- See available players and their details
- Search, bid on, and assign players to teams
- View their team roster and manage it (add/remove players)
- See real-time budget and remaining players
- Configure auction settings (favorites, lineup, budget spread)
- Get optimal team suggestions at any point

---

## Current Status (Post-Phases 0-7 Implementation)

Phases 0 through 7 have been implemented. The core auction flow exists with four tabs (Buy, My Team, Market, Settings), shared state via `AuctionSharedViewModel`, and DataStore persistence. However, a post-implementation review uncovered several bugs and incomplete integrations that prevent the app from compiling and functioning correctly.

**The plan below replaces the old Phase 0-7 items** (which are now done, albeit with bugs) and focuses on what remains to reach a working, polished state.

---

## Phase A: Compilation Fixes (BLOCKER)

These must be resolved before any other work. The app does not compile.

### A.1 Fix `AuctionBuyPlayersViewModel` -- Unresolved `userPreferencesRepository`
- **File**: `AuctionBuyPlayersViewModel.kt:100`
- **Problem**: The `combine` block references `userPreferencesRepository.auctionSettingsFlow`, but `userPreferencesRepository` is not injected into the ViewModel constructor. The constructor only receives `repository`, `sharedViewModel`, and `savedStateHandle`.
- **Fix**: Replace `userPreferencesRepository.auctionSettingsFlow` with `sharedViewModel.settings`. The `AuctionSharedViewModel` already exposes settings as a `StateFlow<AuctionSettings>`, which is the correct shared source of truth.
- **Impact**: The entire `combine` collector (lines 98-124) needs to be updated to use `sharedViewModel.settings` instead. The `Triple` should become `Pair(state, settings)`.

### A.2 Fix `AuctionMyTeamViewModel` -- Missing `.value` on StateFlow
- **File**: `AuctionMyTeamViewModel.kt:85` and `:115`
- **Problem line 85**: `sharedViewModel.currentLeagueId!!` force-unwraps the `StateFlow<Int?>` object itself, not its value. Passes this Flow object to `getTeamsFromLeague()` which expects an `Int`.
- **Fix line 85**: Change to `sharedViewModel.currentLeagueId.value!!`
- **Problem line 115**: `val leagueId = sharedViewModel.currentLeagueId ?: return` checks if the Flow object is null (never is), not the contained value. The null check is useless.
- **Fix line 115**: Change to `val leagueId = sharedViewModel.currentLeagueId.value ?: return`

---

## Phase B: Bug Fixes & Cleanups

These are non-blocking but incorrect behavior or dead code.

### B.1 Remove unused `pagerState` in `AuctionFlowScreen`
- **File**: `AuctionFlowScreen.kt:34`
- **Problem**: `rememberPagerState` is called and `HorizontalPager` is imported, but the tab content is rendered via a `when` block. The pager is never used.
- **Fix**: Remove the `pagerState` variable, the `rememberPagerState` import, and the `HorizontalPager` import.

### B.2 Verify `goalBonusPerRole` type alignment with backend
- **Risk**: The frontend defines `goalBonusPerRole` as `Map<String, Int>` (DTO) / `Map<PlayerRole, Int>` (domain). The Android Studio review claims the backend returns a `Boolean`.
- **Action**: Check the BE repo (`FantaHelpBE`) to confirm the actual type. If it is a `Boolean`, change the frontend DTO to match and add a default mapping in `LeagueMappers.kt`.
- **Priority**: Medium -- only surfaces a crash if the backend type truly mismatches.

### B.3 Owner filtering for teams (Deferred per decision)
- **Current state**: `AuctionMyTeamViewModel.loadTeams()` shows all teams in the league. A TODO comment acknowledges owner filtering is pending.
- **Decision**: Per the single-user tool decision (`ownerId = 1`), this is deferred. The user manually selects their team from the list.
- **Future**: If `ownerId` filtering is added, add `ownerId` to the `Team` domain model and filter in `loadTeams()`.

---

## Phase C: Feature Completions

Functionality that exists in skeleton form but is not wired up end-to-end.

### C.1 Wire Market "Buy" button to Buy tab
- **File**: `AuctionMarketScreen.kt:107-112`
- **Problem**: The `onBuyClick` callback on `MarketPlayerItem` is an empty lambda. Clicking "Buy" does nothing.
- **Design**: Tapping "Buy" on a player should switch to the Buy tab and pre-select that player.
- **Implementation**:
  1. Add a method to `AuctionSharedViewModel`: `fun selectPlayerForBuy(player: Player)` that stores the player in a `MutableStateFlow<Player?>`.
  2. In `AuctionMarketScreen`, call `sharedViewModel.selectPlayerForBuy(player)` in the `onBuyClick` callback.
  3. In `AuctionFlowScreen`, observe `sharedViewModel.selectedPlayerForBuy` via a `LaunchedEffect` or `DisposableEffect`. When it emits a non-null value, switch `selectedTabIndex` to 0 (Buy tab) and clear the flow.
  4. In `AuctionBuyPlayersViewModel`, observe `sharedViewModel.selectedPlayerForBuy`. When non-null, call `onPlayerSelectedFromSearch()` to populate the UI state.

### C.2 Wire Market refresh on mutation events
- **Problem**: `AuctionMarketViewModel` does not collect `sharedViewModel.refreshEvent`. After a player is assigned or removed, the market list is stale.
- **Fix**: In `AuctionMarketViewModel.init`, add:
  ```kotlin
  viewModelScope.launch {
      sharedViewModel.refreshEvent.collect {
          loadAvailablePlayers()
      }
  }
  ```

---

## Phase D: UI/UX Polish

Improvements to the user experience. These do not affect correctness but make the app usable in a real auction scenario.

### D.1 Shared loading/error composables
- Create reusable composables in a shared location (e.g., `ui/components/`):
  - `LoadingState()` -- centered `CircularProgressIndicator`
  - `ErrorState(message: String, onRetry: () -> Unit)` -- error text + retry button
- Replace ad-hoc loading/error rendering in each screen with these shared composables.

### D.2 Snackbar feedback for mutations
- Add `SnackbarHost` to each screen's `Scaffold`.
- Use a `MutableSharedFlow<String>` in each ViewModel to emit success/error messages.
- Messages to surface:
  - "Player assigned to {team}" (Buy tab)
  - "Player removed from team" (My Team tab)
  - "Settings saved" (Settings tab)
  - Error messages from API failures

### D.3 Confirmation dialog for player removal
- **File**: `AuctionMyTeamScreen.kt`
- Before calling `removePlayer(playerId)`, show an `AlertDialog`:
  - Title: "Remove {playerName}?"
  - Message: "This player will be removed from {teamName} and returned to the available pool."
  - Actions: "Cancel" / "Remove"

### D.4 Budget display in Buy tab
- Show the selected team's remaining budget prominently in `AuctionBuyPlayersScreen`.
- Display it near the bid amount input so the user can see if they can afford the bid.
- Format: "{remainingBudget} CR remaining" with color coding (green if sufficient, red if bid > budget).

### D.5 Expanded player details card
- In `AuctionBuyPlayersScreen`, when a player is selected, show:
  - Name, squad, role
  - Market price, rating, expected performance
  - Convenience score delta (if calculated)
- Currently the selected player card may be minimal. Expand it to show all relevant data.

### D.6 Dark theme audit
- Verify dark theme colors across all auction screens.
- Fix any hardcoded light-theme colors (e.g., error text that uses a light color on dark backgrounds).

---

## Phase E: Error Handling & Resilience

### E.1 Retrofit error handling (DONE)
- `DataRepositoryImpl.safeApiCall` already catches `HttpException`, `IOException`, and maps them to `AppError` subclasses. This is complete.

### E.2 Suggestion timeout UX
- The `getSuggestion` endpoint can take up to 60s.
- **Action**: In the convenience score loading state, show a message like "Calculating optimal score... this may take a moment."
- Allow cancellation: expose a `cancelSuggestion()` method that cancels the `convenienceScoreJob`.

### E.3 Offline detection
- **Action**: Before API calls, check `ConnectivityManager.isActiveNetworkAvailable`.
- Show "No internet connection" instead of a generic timeout error.
- **Priority**: Low for a single-user tool used at a known location with network.

### E.4 Graceful degradation for suggestion API
- If suggestion fails, the app should still allow buying, removing, and browsing.
- **Action**: Show a warning banner ("Suggestion service unavailable") but do not block any flow.

---

## Phase F: Testing

### F.1 Unit tests for ViewModels
- `AuctionBuyPlayersViewModel`: test state transitions for search, selection, assignment
- `AuctionSharedViewModel`: test settings persistence and refresh event propagation
- `AuctionMyTeamViewModel`: test team loading, player removal, suggestion request
- `AuctionMarketViewModel`: test filtering, sorting, search logic

### F.2 Unit tests for Repository
- Mock `ApiService`, test `safeApiCall` error mapping
- Test cache behavior: `clearCache()` invalidates all caches

### F.3 UI smoke tests
- Critical path: search player → select → set bid → assign → verify refresh
- My Team: load teams → select team → remove player → verify refresh
- Market: load → filter by role → sort → search

---

## Execution Order

```
Phase A (Compilation Fixes)        <---- MUST do first, app won't compile
    |
Phase B (Bug Fixes & Cleanups)     <---- quick fixes, do while context is fresh
    |
Phase C (Feature Completions)      <---- wire up incomplete features
    |
Phase D (UI/UX Polish)             <---- improve usability
    |
Phase E (Error Handling)           <---- can be done incrementally alongside D
    |
Phase F (Testing)                  <---- final validation
```

---

## Decisions

1. **Single-user tool** -- No authentication. `ownerId = 1` is acceptable. Skip user selection screens.
2. **LineUp per-league** -- LineUp configuration is stored globally in `AuctionSettings` (DataStore). If per-league storage is needed later, key settings by league ID.
3. **Owner filtering deferred** -- User manually selects their team from the league's team list.

## Tracked Backend Dependencies

- **`SuggestionRequest.auctionedPlayer` missing** (FantaHelpBE): The frontend sends `auctionedPlayer` in the convenience-score request, but the backend `SuggestionRequest` does not have this property -- it is silently dropped during deserialization. The "potential score" call returns the same value as the "base score" call, making the convenience delta always zero. Fix required in `FantaHelpBE/Fantahelp.API/Models/Dtos/SuggestionRequest.cs` and `TeamSuggestionService.cs`. Handle in the BE repo separately.

## Risks & Open Questions

1. **Budget enforcement**: The backend does NOT enforce budget constraints when adding a player to a team. The frontend enforces this client-side (`AuctionBuyPlayersViewModel.assignPlayer()` checks `team.remainingBudget < bid`).
2. **Concurrent auctions**: No backend locking. Single-user usage assumed.
3. **Player data freshness**: The suggestion algorithm uses `AuctionPrice` for current team players and standard `ExpectedPrice` for available players. Intentional per backend design.
4. **`goalBonusPerRole` type mismatch**: Unverified. Frontend uses `Map<String, Int>`. Backend may return `Boolean`. Needs confirmation from BE repo.

---

## Estimated Effort

| Phase | Complexity | Estimated Time |
|-------|-----------|----------------|
| A - Compilation Fixes | Low | 30 min - 1 hour |
| B - Bug Fixes & Cleanups | Low | 30 min |
| C - Feature Completions | Medium | 2-3 hours |
| D - UI/UX Polish | Medium | 3-4 hours |
| E - Error Handling | Low-Medium | 1-2 hours |
| F - Testing | Medium | 2-3 hours |
| **Total** | | **~9-13 hours** |
