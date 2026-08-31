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
| 9 | Per-format expected prices on player read endpoints | 🔄 PARTIAL | The **league-scoped** endpoint is now format-aware (optional `starters` query param) and goal-bonus adjusted — see #14. The global `GET /api/players` still returns reference-format (800_8) values (no league context). |
| 10 | `PlayerReadDto.integrity` new nullable field | ℹ️ INFO | Additive field (1–5, higher = more robust, null = unknown). Moshi-safe; FE may display it, no action required. |
| 11 | `regularness` moves to 0–100 expected-starting-% | ℹ️ INFO | 26-27+: `regularness` is now 0–100 (multiples of 5), was int 1–5. FE's `parseRegularnessValue` (PlayerMapper.kt) already handles the 0–100 scale -- **no FE action needed**. Values like 0 (bench) and 65/90 (real spread) will appear. |
| 12 | `StrategyWeights` in `getOptimal` + extended strategy presets | 📋 ACTION | New optional `weights` object on `SuggestionRequest` (6 knobs, 0–10; absent = legacy behavior). 8 reference presets provided (see detail below); FE to pick/arrange as it sees fit. |
| 13 | Goal-bonus toggle UI | 📋 ACTION | `PUT /api/leagues/{id}/goal-bonus` (body `{"goalBonusPerRole": bool}`). FE already maps the flag into its `League` model; needs a settings-screen toggle. When on, engine + market tab adjust D/C values and prices. |
| 14 | Market tab: league-adjusted prices, `starters` param, `baseExpectedPrice` | 📋 ACTION | `GET /api/leagues/{id}/players?starters=N` now returns format-resolved + goal-bonus-adjusted `expectedPrice`/`expectedStd`/`expectedPerformance`, plus new optional `baseExpectedPrice` (raw ML estimate). FE should pass the lineup total starters. See detail below. |

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

### 9. `PlayerReadDto` — Per-Format Expected Prices (26-27+ contract) — PARTIAL

**Context**: From the 26-27 season, ML produces **per-format** expected prices — the BE stores them in a `PlayerPrice` table (one row per player × league format: credits + starters). The suggestion engine (`POST /api/teams/getOptimal`) resolves the league's format internally from `(League.InitialBudget, lineup total starters)` (exact match, else closest), and `SuggestionResult.SuggestedPlayers` prices are format-correct.

**Done (league-scoped endpoint)**: `GET /api/leagues/{id}/players` now takes an optional `starters` query param and returns that league's format-resolved `expectedPrice`/`expectedStd` (same resolver as the engine; without the param, closest by credits then ascending starters). It also applies the league's goal-bonus adjustment (see #13) and exposes the raw estimate as `baseExpectedPrice` (see #14).

**Remaining**: the global endpoints (`GET /api/players`, `GET /api/players/{id}`) still return reference-format (800_8) values — they have no league context, so this is by design; the market tab uses the league-scoped endpoint and is unaffected.

**Status**: league-scoped part DONE; global endpoints intentionally left on the reference format.

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

---

### 12. `StrategyWeights` — Personal-Preference Knobs on `POST /api/teams/getOptimal`

**Context**: The scoring engine gained user-tunable weights. The objective is:

```
Total = (StarterWeight·S + BenchWeight·B + StrategyWeight·T) / 10
```

with an optional `weights` object on the request:

```jsonc
{
  "teamId": 1,
  "lineUp": { "keepers": 1, "defenders": 4, "midfielders": 3, "attackers": 3 },
  "creditsDistribution": 4,
  "budgetAllocation": { "goalkeepers": 0.10, "defenders": 0.30, "midfielders": 0.60, "attackers": 0.60 },
  "weights": {                       // entirely optional; absent = defaults below
    "starterWeight": 6,              // 0-10, default 6  (starters block)
    "benchWeight": 3,                // 0-10, default 3  (bench depth block)
    "strategyWeight": 1,             // 0-10, default 1  (strategy/preferences block)
    "squadDiversity": 2,             // 0-10, default 2  (same-club aversion)
    "mateWeight": 1,                 // 0-10, default 1  (value of a mate on the bench)
    "reliabilityWeight": 10          // 0-10, default 10 (reliability discount on player value)
  }
}
```

**Player value (the core of the engine)**: every player is scored by their *reliable value*

```
V = expmf × (regularness/100)^(Rel/10) × (1 + 0.03·(Rel/10)·(integrity−3))   [integrity null → factor 1]
```

(expmf = expected performance **when playing** — start probability not included, by ML contract; the market price already reflects availability, so the regularness exponent converts quality into *expected contribution per fixture* without double-counting). `ReliabilityWeight` is the strength of this discount:
- `10` (default) = full expected contribution — a reg-50 player is worth half their quality. Economically consistent with market prices.
- `5` = tempered (square-root discount); `0` = pure quality-when-playing (overvalues irregular players).

**Blocks**:
- **S (starters)** = Σ V of the 11 starters + back-4 defense bonus (4+DEF lineups only; threshold on the base expmf, unchanged).
- **B (bench)** = **0.5 × Σ V of the bench** (rotation factor: a bench player plays about half the fixtures). Note the scale change vs the old ratio: B is now ~35–45, not 0–4. The 0.5 keeps the S/B ratio a (weak) line-vs-bench trade-off: effective value per point is `StarterWeight/10` for the line vs `BenchWeight×0.5/10` for the bench. Verified: the ratio flips only marginal picks and saturates once `BenchWeight×0.5 ≥ StarterWeight` — the team is dominated by V itself, the T knobs, and the budget allocation.
- **T (strategy)** = mates + graduated same-club penalty + credit spread. The old regularness term is **gone** — regularness now lives in V where it has real weight in both S and B. Squad penalty: each same-club player beyond the 1st in a role costs `SquadDiversity/5`, beyond the 3rd club-wide `2×` that.

**⚠️ Behavior change vs the previous version**: requests *without* `weights` no longer reproduce the pre-rework legacy score — the default now applies the full reliability discount (Rel 10) and the new bench block. The old behavior is reachable with `reliabilityWeight: 0` (and is otherwise a different objective). Score magnitudes changed accordingly (totals ~45–55 on 26-27 data, not ~48 on the old scale). The auction `potentialScore`/`withoutPlayerScore` inherit the weights automatically.

**Reference presets** (all verified to produce distinct teams on the 26-27 data; the FE is free to use a subset, rename or add its own — every knob is 0–10):

| Preset | S/B/T | SquadDiv | Mate | Rel | Character (verified team on 26-27) |
|---|---|---|---|---|---|
| **Balanced** (default) | 6/3/1 | 2 | 1 | 10 | expected-contribution optimum: Dimarco/Mancini/N'Dicka/Tiago Gabriel D line (all reg ≥ 90) |
| **Max Points** | 7/2/1 | 1 | 1 | 0 | pure quality-when-playing: Molina/Spence/Fortini enter, S 80 vs 71.5 |
| **Star Line** | 8/2/2 | 2 | 1 | 10 | line-biased ratio: strongest line, weaker bench (S 71.9 / B 37.5) |
| **Deep Bench** | 4/8/2 | 2 | 1 | 10 | bench-biased ratio: marginal line credits move to the bench (Cristante enters) |
| **Diversified** | 6/3/3 | 10 | 1 | 10 | strong same-club aversion: Vasquez replaces N'Dicka in the D line |
| **Mate Collector** | 6/3/3 | 2 | 10 | 10 | buys ~11 mate pairs, deliberately weakens the line (S 64.8, T +35.7) |
| **Tempered** | 6/3/1 | 2 | 1 | 5 | half-strength reliability: Molina re-enters the D line, S 74.6 |
| **Mate & Variety** | 6/3/3 | 5 | 5 | 10 | mild mates + mild diversity mix (S 68.0, T +6.7) |

**FE side**: send `weights` from the selected preset (extend the presets in `AuctionSettingsScreen` with these knobs). The `Score` DTO is unchanged (starterScore / benchScore / penaltyScore / totalScore).

**Status**: BE DONE; presets finalized from the trial matrix (2026-08-24, second pass after the reliability rework).

---

### 13. Goal-Bonus Toggle — `PUT /api/leagues/{id}/goal-bonus`

**Endpoint**: `PUT /api/leagues/{leagueId}/goal-bonus`, body `{ "goalBonusPerRole": true|false }`. Returns the updated `LeagueReadDto` (FE already maps `goalBonusPerRole` into its `League` model — item 3).

**Effect when on** (the league awards the per-role goal bonus GK 6 / DEF 5 / MID 4 / ATT 3 per goal, with 3 as the base already inside `expmf`):
- **Values**: defender expected performance ×1.02, midfielder ×1.03 (attackers/keepers unchanged) — the net bonus over the base 3.
- **Market prices** (players being bought, in the engine *and* the league-scoped market endpoint): defenders ×1.10, midfielders ×1.05 (empirical market uplift in bonus leagues).
- The `Score` in suggestions no longer contains a separate goal-bonus term; the adjustment is baked into values and prices.

**FE side**: add a toggle in the league/settings screen that calls the endpoint and refreshes the league. The market tab and suggestions then reflect the adjusted prices automatically (no extra plumbing).

**Status**: BE DONE; FE action required.

---

### 14. Market Tab — League-Adjusted Prices, `starters` Param, `baseExpectedPrice`

**Context**: with the goal-bonus rework (#13), the league-scoped market endpoint now returns the same prices the engine scores with, so the market tab and the computed teams are consistent.

**Changes on `GET /api/leagues/{id}/players`**:
- Optional query param `starters` (lineup total, e.g. `11`): selects the price format the same way the engine does. Without it, the closest format by credits is used (deterministic tie-break, ascending starters).
- `expectedPrice` / `expectedStd` / `expectedPerformance` are now **league-adjusted** (goal-bonus uplift when the flag is on) and **format-resolved** (previously always reference-format 800_8).
- New optional field `baseExpectedPrice: Int?` — the raw ML estimate before the league adjustment (null on the global endpoints). Moshi-safe (additive nullable).

**FE side**:
- Pass `starters` from the current lineup setting (sum of GK+DEF+MID+ATT) when fetching league players.
- Optionally surface `baseExpectedPrice` as a secondary line / tooltip ("base estimate 96, league price 106"). No display change is required for correctness.

**Status**: BE DONE; FE action required (small).


