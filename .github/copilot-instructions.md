# GitHub Copilot Context

## Guidelines

- Never make changes to files in the `build` directory. These are temporary files.
- Don't introduce new Interfaces when it can be solved in a simpler way such as an abstract class or lambda.

### Developing Abilities
- When adding new abilities, ensure to document them in `.docs/abilities.md` with their type and properties.
- Follow the existing structure and formatting in the documentation for consistency.
- Abilities must be registered in `com.windanesz.menhir.api.Birthsign.EffectType` and `com.windanesz.menhir.ability.AbilityFactory#FACTORIES`





## Adding a New Altar Type

### Step 1: Create Handler Class
```java
package com.windanesz.menhir.altar.handler;

public class MyCustomHandler implements IAltarEffectHandler {
    @Override
    public String getEffectType() {
        return "my_custom_type";
    }
    
    @Override
    public boolean canHandle(AltarDefinition definition) {
        return definition.getEffects().stream()
            .anyMatch(e -> e instanceof MyCustomEffect);
    }
    
    @Override
    public boolean handleInteraction(...) {
        // Your logic here
        
        // Store data if needed:
        NBTTagCompound data = new NBTTagCompound();
        data.setString("key", "value");
        altar.setDataValue("my_handler_data", data);
        
        // Update usage if you return true:
        altar.incrementPlayerUses(player.getUniqueID());
        altar.incrementTotalUses();
        
        return true; // or false to continue with standard effects
    }
}
```

### Step 2: Register Handler
In `Menhir.preInit()`:
```java
AltarEffectHandlerRegistry.registerHandler(new MyCustomHandler());
```

### Step 3: Create JSON Configuration
```json
{
  "id": "my_custom_altar",
  "name": "My Custom Altar",
  "rarity": "rare",
  "effects": [
    {
      "type": "my_custom_type",
      "custom_property": "value"
    }
  ]
}
```

That's it! No modifications to `BlockAltar` or tile entity required.

## Generic Data Storage

Handlers store custom data using the tile entity's generic NBT storage:

```java
// Store
NBTTagCompound data = new NBTTagCompound();
data.setInteger("x", pos.getX());
data.setLong("timestamp", world.getTotalWorldTime());
altar.setDataValue("my_key", data);

// Retrieve
NBTTagCompound data = altar.getDataValue("my_key");
if (data != null && data.hasKey("x")) {
    int x = data.getInteger("x");
    long time = data.getLong("timestamp");
}
```

Benefits:
- No need to add fields to TileEntityAltar
- Automatically persists and syncs to client
- Each handler manages its own data namespace

## Comparison with Alternatives

### Alternative 1: Switch Statement in BlockAltar
```java
switch (altarType) {
    case "teleport_twin": /* 100 lines */ break;
    case "prayer": /* 100 lines */ break;
    case "blessing": /* 100 lines */ break;
}
```
**Problem:** BlockAltar becomes massive and hard to maintain

### Alternative 2: Effect.apply() Does Everything
```java
class TeleportTwinEffect {
    boolean apply(...) {
        // Handle ALL teleport logic here
        // But effects shouldn't modify world or tile entities!
    }
}
```
**Problem:** Violates separation of concerns (effects should be data, not behavior)

### Alternative 3: Separate Block Classes (discussed above)
**Problem:** Too many blocks, complex registration, duplicate code

### Our Solution: Handler Registry ✓
**Result:** Clean, extensible, follows SOLID principles

## Future Extensions

Easy to add:
- **RitualHandler** - Multi-step interactions
- **TimedBlessingHandler** - Buffs that expire
- **QuestAltarHandler** - Integration with quest mods
- **GroupPrayerHandler** - Requires multiple players
- **SacrificeHandler** - Requires entity sacrifices

All without touching `BlockAltar`!
