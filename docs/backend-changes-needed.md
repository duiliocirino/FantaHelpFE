# Backend Changes Required for FantaHelpFE Alignment

The frontend audit found 3 missing fields in Read DTOs that cause Moshi deserialization crashes on the FE side, plus 1 tracked feature gap.

---

## Status

| # | Issue | Status | Notes |
|---|---|---|---|
| 1 | `TeamReadDto` missing `OwnerId` | ✅ RESOLVED | BE added `OwnerId` property, mapped from `Team.OwnerId` |
| 2 | `TeamPlayerReadDto` missing `PlayerRole` | ✅ RESOLVED | BE added `PlayerRole` property, mapped from `TeamPlayer.Player.Role` |
| 3 | `LeagueReadDto` missing `GoalBonusPerRole` | ✅ RESOLVED | BE added `GoalBonusPerRole` property, mapped from `League.GoalBonusPerRole` |
| 4 & 5 | `SuggestionRequest.AuctionedPlayer` & Single-Call Contract | ✅ RESOLVED | BE merged single-call contract: `SuggestionResult` contains `score` (base optimal) and `potentialScore` (`SuggestionResult?`, potential optimal). Shared per-role DP tables. Returns `potentialScore = null` if budget is exceeded or `auctionedPlayer` is null. Integrated on FE. |
| 6 | `GET /api/teams/{id}` Roster Inclusion | ✅ RESOLVED | BE added `.Include(t => t.TeamPlayers)` to `GET /api/teams/{id}` endpoint. Roster items map properly to `TeamReadDto.Players`. Integrated on FE. |
| 9 | Per-format expected prices on player read endpoints | 📋 BACKLOG | 26-27+ contract: `expectedPrice`/`expectedStd` on `GET /api/players` & `GET /api/leagues/{id}/players` are reference-format (800_8) values. BE will add an optional format query param; FE to request its league's format. No FE action until implemented. |
| 10 | `PlayerReadDto.integrity` new nullable field | ℹ️ INFO | Additive field (1–5, higher = more robust, null = unknown). Moshi-safe; FE may display it, no action required. |
| 11 | `regularness` moves to 0–100 expected-starting-% | ℹ️ INFO | 26-27+: `regularness` is now 0–100 (multiples of 5), was int 1–5. FE's `parseRegularnessValue` (PlayerMapper.kt) already handles the 0–100 scale -- **no FE action needed**. Values like 0 (bench) and 65/90 (real spread) will appear. |

---

## Resolved Changes (Items 1–6)

### 1. `TeamReadDto` — Missing `OwnerId` ✅

**File**: `Fantahelp.API/Models/Dtos/Read/TeamReadDto.cs`

**Added**:
```csharp
public required int OwnerId { get; set; }
```

**Mapper updated** to include `OwnerId = team.OwnerId`.

---

### 2. `TeamPlayerReadDto` — Missing `PlayerRole` ✅

**File**: `Fantahelp.API/Models/Dtos/Read/TeamPlayerReadDto.cs`

**Added**:
```csharp
public required string PlayerRole { get; set; }
```

**Mapper updated** to include `PlayerRole = teamPlayer.Player.Role`.

---

### 3. `LeagueReadDto` — Missing `GoalBonusPerRole` ✅

**File**: `Fantahelp.API/Models/Dtos/Read/LeagueReadDto.cs`

**Added**:
```csharp
public bool GoalBonusPerRole { get; set; }
```

**Mapper updated** to include `GoalBonusPerRole = league.GoalBonusPerRole`.

---

## Action Required (Item 6) 🚨

### 6. `GET /api/teams/{id}` — Include Roster (`Players` collection)

**File**: `Fantahelp.API/Controllers/TeamsController.cs` (or `GetTeamById` handler)

**Issue**: While `POST /api/teams/getOptimal` correctly includes assigned team players when generating squad suggestions, `GET /api/teams/{id}` returns an empty `players: []` array on `TeamReadDto`.

**Fix Required**:
Ensure EF Core explicitly includes `TeamPlayers` / `Players` when retrieving the team entity:

```csharp
var team = await _context.Teams
    .Include(t => t.TeamPlayers)
    .ThenInclude(tp => tp.Player)
    .FirstOrDefaultAsync(t => t.Id == id);
```

And map `team.TeamPlayers` to `TeamReadDto.Players`.


---

## Agreed Contract (Items 4 & 5) 🤝

### 4 & 5. Single-Call Convenience Score Contract (`POST /api/teams/getOptimal`)

**Endpoint**: `POST /api/teams/getOptimal`

**Contract Specification**:
When `auctionedPlayer` is present in `SuggestionRequest`, the backend returns a **single `SuggestionResult` object** containing two score properties:

```csharp
public class SuggestionResult
{
    public List<PlayerReadDto> SuggestedPlayers { get; set; }
    public int TotalExpectedPrice { get; set; }
    public int TotalExpectedPriceStd { get; set; }
    public Score Score { get; set; } // Base optimal score (current roster only)
    public Score? PotentialScore { get; set; } // Potential optimal score (with auctioned player)
}
```

**Key Behaviors**:
1. **Performance**: Heavy per-role DP tables run **once** and are shared between `Score` and `PotentialScore`. Only the cheap final combination step runs twice, achieving the **50% CPU reduction**.
2. **`auctionedPlayer == null`**: `PotentialScore` is `null` (standard squad suggestion behavior).
3. **Budget Exceeded Edge Case**: If `acquisitionPrice > remainingBudget`, `PotentialScore` is set to `null` (signals to FE that the team cannot afford the bid).

---

## Future Enhancements / Feature Backlog 🔮

### 7. `SuggestionResult` — `RecommendedMaxBid` Break-Even Calculation

**File**: `Fantahelp.API/Models/Dtos/Read/SuggestionResult.cs` & `TeamSuggestionService.cs`

**Concept**: When `auctionedPlayer` is provided in `POST /api/teams/getOptimal`, compute the maximum acquisition price (`RecommendedMaxBid`) before the net point advantage turns neutral or negative due to remaining budget penalties.

**Proposed DTO Field**:
```csharp
public int? RecommendedMaxBid { get; set; }
```

**Behavior**:
Return the break-even maximum bid price alongside `PotentialScore`. The FE will display AI bidding advice to the user during live auctions (e.g. *"Your bid of 35 CR is 13 CR below your maximum profitable bid of 48 CR"*).

---

### 8. `TeamSuggestionService` — Base Score Candidate Exclusion for Auctioned Player

**File**: `Fantahelp.API/Services/TeamSuggestionService.cs`

**Concept**: When `auctionedPlayer` (`playerId`, `acquisitionPrice`) is specified in `SuggestionRequest`, the solver evaluates `potentialScore` WITH `auctionedPlayer` at `acquisitionPrice`. For the base `score` calculation, filter out `auctionedPlayer.playerId` from the available market candidate pool.

**Benefit**: This ensures `score` represents the true Plan B benchmark optimal team **without the auctioned player**, making $\Delta = \text{PotentialScore} - \text{BaseScore}$ the exact net advantage of winning the auctioned player vs letting them go to a rival manager.

---

### 9. `PlayerReadDto` — Per-Format Expected Prices (26-27+ contract)

**Files**: `Fantahelp.API/Controllers/PlayersController.cs`, `Fantahelp.API/Services/PlayerService.cs`

**Context**: From the 26-27 season, ML produces **per-format** expected prices — the BE stores them in a `PlayerPrice` table (one row per player × league format: credits + starters). The suggestion engine (`POST /api/teams/getOptimal`) already resolves the league's format internally from `(League.InitialBudget, lineup total starters)` (exact match, else closest), and `SuggestionResult.SuggestedPlayers` prices are format-correct.

**Current behavior**: the player read endpoints (`GET /api/players`, `GET /api/players/{id}`, `GET /api/leagues/{id}/players`) return a single **reference-format (800_8)** value in `expectedPrice`/`expectedStd` for every player, regardless of the consuming league's format.

**Requested change**: add an optional format query parameter (e.g. `?credits=1000&starters=10`) to the player read endpoints; when present, return that format's `expectedPrice`/`expectedStd` (closest available format when no exact match). When absent, keep today's reference-format behavior for backward compatibility.

**FE side**: the FE knows each league's format (budget + lineup), so it can pass it once the param lands. Until then, displayed expected prices are 800_8 values — fine for 800-credit leagues, approximate for others.

**Status**: BE backlog (tracked in `FantaHelpBE/docs/FUTURE_STEPS.md`).

---

### 10. `PlayerReadDto.integrity` — New Nullable Field (26-27+)

**Context**: `integrity` is ML's injury-proneness consensus: 1–5, higher = more robust (5 = iron man). Null when no consensus exists — it is intentionally never defaulted.

**Impact on FE**: purely additive on `PlayerReadDto` (and the league players endpoint). Moshi ignores unknown fields, so nothing breaks. If the FE wants to surface robustness on scouting cards, model it as `Integrity: Int?`.

**Status**: informational — no BE/FE action required.

---

### 11. `regularness` — 0–100 Expected-Starting-% Scale (26-27+)

**Context:** From the 26-27 season, ML ships `regularness` as the **expected starting percentage, 0–100 in multiples of 5** (was: int 1–5). Source: the LLM squad assessment for all 515 players; the community 1–5 consensus is now the validation anchor, not the data source. Distribution: 0–95, filled for all players, with real within-bucket spread (e.g. Barella 90, Dybala 65, a bench player 0).

**Impact on FE: none.** `parseRegularnessValue` in `PlayerMapper.kt` already branches on the 0–100 scale (`else -> d.toInt()`), so values like 65 or 0 parse correctly as-is. The old 1–5 rescaling branch remains for backward compatibility with older data.

**Display note:** values are now a true percentage — render them as-is (e.g. "65%"), without any 1–5 → % remapping. A value of `0` is a real signal (player is a bench player / not expected to start), not missing data.

**Status**: informational — confirmed compatible, no action required.


