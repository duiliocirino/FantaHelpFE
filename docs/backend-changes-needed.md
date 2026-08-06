# Backend Changes Required for FantaHelpFE Alignment

The frontend audit found 3 missing fields in Read DTOs that cause Moshi deserialization crashes on the FE side, plus 1 tracked feature gap.

---

## 1. `TeamReadDto` — Missing `OwnerId` (CRASH)

**File**: `Fantahelp.API/Models/Dtos/Read/TeamReadDto.cs`

The FE expects `ownerId` to filter teams by owner. The `Team` entity has `OwnerId`, but it is not mapped into the ReadDto.

**Add**:
```csharp
public required int OwnerId { get; set; }
```

**Update `TeamMapper.ToReadDto()`** to include:
```csharp
OwnerId = team.OwnerId
```

---

## 2. `TeamPlayerReadDto` — Missing `PlayerRole` (CRASH)

**File**: `Fantahelp.API/Models/Dtos/Read/TeamPlayerReadDto.cs`

The FE expects `playerRole` (String) to display player roles in the roster view. The `TeamPlayer` entity has a `Role` column, but it is not mapped into the ReadDto.

**Add**:
```csharp
public required string PlayerRole { get; set; }
```

**Update the mapper** to include:
```csharp
PlayerRole = teamPlayer.Role
```

---

## 3. `LeagueReadDto` — Missing `GoalBonusPerRole` (CRASH)

**File**: `Fantahelp.API/Models/Dtos/Read/LeagueReadDto.cs`

The FE expects `goalBonusPerRole` (Boolean). The `League` entity has `GoalBonusPerRole: bool` (default `true`), but it is not mapped into the ReadDto.

**Add**:
```csharp
public bool GoalBonusPerRole { get; set; }
```

**Update `LeagueMapper.ToReadDto()`** to include:
```csharp
GoalBonusPerRole = league.GoalBonusPerRole
```

---

## 4. `SuggestionRequest` — Missing `AuctionedPlayer` (WRONG DATA, TRacked)

**File**: `Fantahelp.API/Models/Dtos/SuggestionRequest.cs`

The FE sends `auctionedPlayer` (nullable object with `playerId` and `acquisitionPrice`) in the "potential score" convenience-score request. The BE silently drops this field during deserialization, so the "potential score" returns the same value as the "base score" — making the convenience delta always zero.

**Add**:
```csharp
public AuctionedPlayerInfo? AuctionedPlayer { get; set; }
```

Where `AuctionedPlayerInfo` is a new DTO:
```csharp
public class AuctionedPlayerInfo
{
    public int PlayerId { get; set; }
    public int AcquisitionPrice { get; set; }
}
```

Then use it in `TeamSuggestionService` when computing the potential score — inject the player with the given acquisition price into the suggestion algorithm.

---

## Impact

Items 1-3 cause **hard crashes** on the FE side (Moshi `JsonDataException`) for every league/team/player fetch. These are blocking — the auction flow cannot function until they are resolved.

Item 4 is non-blocking but makes the convenience score feature return incorrect data (delta always zero).
