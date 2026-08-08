# Backend Changes Required for FantaHelpFE Alignment

The frontend audit found 3 missing fields in Read DTOs that cause Moshi deserialization crashes on the FE side, plus 1 tracked feature gap.

---

## Status

| # | Issue | Status | Notes |
|---|---|---|---|
| 1 | `TeamReadDto` missing `OwnerId` | ✅ RESOLVED | BE added `OwnerId` property, mapped from `Team.OwnerId` |
| 2 | `TeamPlayerReadDto` missing `PlayerRole` | ✅ RESOLVED | BE added `PlayerRole` property, mapped from `TeamPlayer.Player.Role` |
| 3 | `LeagueReadDto` missing `GoalBonusPerRole` | ✅ RESOLVED | BE added `GoalBonusPerRole` property, mapped from `League.GoalBonusPerRole` |
| 4 | `SuggestionRequest` missing `AuctionedPlayer` | 🔶 TRACKED | DTO field added and deserialized. Scoring logic to use it in potential score calculation is in the BE backlog. Until then, convenience delta reads zero. |

---

## Resolved Changes (Items 1–3)

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

## Tracked (Item 4)

### 4. `SuggestionRequest` — Missing `AuctionedPlayer` 🔶

**File**: `Fantahelp.API/Models/Dtos/SuggestionRequest.cs`

The FE sends `auctionedPlayer` (nullable object with `playerId` and `acquisitionPrice`) in the "potential score" convenience-score request. The BE now deserializes this field correctly, but the scoring logic to actually use it in the potential score calculation is tracked in the BE backlog.

**FE-side DTO already present** (`AuctionedPlayerInfo` with `playerId` and `acquisitionPrice`).

**Impact**: Non-blocking. Convenience delta will read zero until the BE implements the scoring logic.

---

## Impact

Items 1–3 were **hard crashes** on the FE side (Moshi `JsonDataException`) for every league/team/player fetch. Now resolved — the auction flow should function without deserialization errors.

Item 4 is non-blocking but makes the convenience score feature return incorrect data (delta always zero) until the BE scoring logic is implemented.
