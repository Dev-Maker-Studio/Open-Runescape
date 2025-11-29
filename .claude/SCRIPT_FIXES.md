# KotlinScript Loader Fixes

## Issues Fixed

### 1. Script Instance Extraction
**Problem:** Scripts were returning instances at the end (e.g., `SimpleBanker()`), but the loader wasn't properly extracting them from the evaluation result.

**Solution:** Updated `KotlinScriptLoader.kt` to properly handle the return value:
```kotlin
val returnValue = evalResult.value.returnValue
val scriptInstance = when (returnValue) {
    is ResultValue.Value -> returnValue.value
    else -> returnValue.scriptInstance
}
```

Now checks both `returnValue.value` (for returned instances) and `returnValue.scriptInstance` (fallback).

### 2. SkillMonitor Script Errors

**Problems:**
- Used `Skill.DEFENCE` instead of `Skill.DEFENSE` (American spelling)
- Used `event.xp` and `event.amount` instead of `event.gainedXp`
- Variable name collision with `xp` in multiple scopes

**Solutions:**
- Changed to `Skill.DEFENSE`
- Changed to `event.gainedXp` (correct property name from XPDrop class)
- Renamed variables to avoid collisions (`currentXp`, `gainedXp`)

## Files Modified

1. **src/main/kotlin/com/darkan/bot/scripts/KotlinScriptLoader.kt**
   - Fixed return value extraction from script evaluation

2. **~/.darkan/scripts/SkillMonitor.kts**
   - Fixed enum reference: `DEFENCE` → `DEFENSE`
   - Fixed property access: `event.xp` → `event.gainedXp`
   - Fixed variable names to avoid shadowing

## Testing

Build successful:
```
./gradlew build
BUILD SUCCESSFUL in 15s
```

## Expected Behavior

After these fixes, all three sample scripts should:
1. Compile without errors
2. Appear in the scripts list after clicking "Reload Scripts"
3. Be startable and stoppable from the UI
4. Execute their logic correctly

### Script Status
- ✅ **SimpleBanker.kts** - Should work (opens bank, deposits items)
- ✅ **SkillMonitor.kts** - Should work (tracks XP drops)
- ✅ **AutoClicker.kts** - Should work (configurable clicking)
