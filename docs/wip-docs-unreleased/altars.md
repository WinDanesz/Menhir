# Menhir Altar System

The Altar system provides randomly generated rare shrines that give various bonuses to players. Altars are defined in JSON files and spawn naturally in the world with configurable rarity tiers.

## Features

### Rarity Tiers
Altars come in five rarity tiers, each with different spawn weights:
- **Common** (White) - Most frequent
- **Uncommon** (Green)
- **Rare** (Blue)
- **Epic** (Purple)
- **Legendary** (Gold) - Extremely rare

### Usage Modes
Altars can have three types of usage limits:
1. **Unlimited** - Can be used infinitely
2. **Times per Player** - Each player can use it X times
3. **Times by Anyone** - Total uses across all players

### Guardians
Altars can spawn guardian mobs when activated:
- **Mob Spawner** - Spawns continuously (not yet implemented)
- **First Interaction** - Spawns only when first used
- **Every Interaction** - Spawns each time it's used

Guardian spawns support:
- Configurable entity types
- Count ranges (min/max)
- Custom NBT data
- Individual spawn chances
- Global guardian chance override

### Effects
Multiple effect types are supported:
- **Potion Effects** - Apply status effects to players
- **Command Execution** - Run server commands with placeholders
- **Teleport Recall** - Mark location for later teleportation
- **Teleport Twin** - Link two altars for instant travel
- **Prayer** (not yet fully implemented) - Require text recitation

Effect options:
- `once_per_player` - Effect only applies on first use
- `unique_per_world` - Only one instance can exist per world
- `single_use` - Effect can only be triggered once total
- `obfuscated` - Effect details hidden until activated

### Requirements
Altars can require:
- **Items** - Specific items with count, metadata, and NBT
  - Can be consumed or just required in inventory
- **XP Levels/Amount** - Require and optionally consume XP
- **Advancements** - Require specific advancement completion
- **Birthsign** - Require a specific birthsign (Menhir feature)

### Time Restrictions
Altars can be restricted to specific times of day:
- `day` - Daytime only
- `noon` - Around midday
- `dusk` - Evening
- `night` - Night time
- `midnight` - Deep night
- `dawn` - Early morning

### Channeling
Altars can require continuous interaction for a duration before activating. Specified in ticks (20 ticks = 1 second).

### Special Features
- **Obfuscation** - Displays gibberish name until first use
- **Unique per World** - Only one instance spawns per world
- **Chaotic Altars** - Can rebind to different effects daily (use `exclude_from_chaotic` to opt-out)
- **Required Mods** - Only load altar if specific mods are present

## JSON Format

### Basic Structure
```json
{
  "id": "unique_altar_id",
  "name": "Display Name",
  "rarity": "common|uncommon|rare|epic|legendary",
  "in_tier_weight": 1.0,
  "unique_per_world": false,
  "obfuscated": false,
  "exclude_from_chaotic": false,
  "channel_time": 0,
  "usage": {
    "type": "unlimited|times_per_player|times_by_anyone",
    "limit": 5
  },
  "allowed_times_of_day": ["day", "night"],
  "required_birthsign": "the_warrior",
  "required_mods": ["modid"],
  "global_guardian_chance": 0.8,
  "guardians": [],
  "requirements": {},
  "effects": []
}
```

### Guardian Definition
```json
{
  "entity": "minecraft:zombie",
  "min_count": 2,
  "max_count": 4,
  "spawn_type": "first_interaction|every_interaction|mob_spawner",
  "spawn_chance": 1.0,
  "nbt": {}
}
```

### Requirements
```json
{
  "items": [
    {
      "item": "minecraft:diamond",
      "min_count": 5,
      "max_count": 10,
      "data": 0,
      "consumed": true,
      "nbt": {}
    }
  ],
  "xp_levels": 10,
  "xp_amount": 100,
  "consume_xp": true,
  "birthsign": "the_warrior",
  "advancements": ["minecraft:story/mine_diamond"]
}
```

### Effects

#### Potion Effect
```json
{
  "type": "potion",
  "id": "unique_effect_id",
  "potion": "minecraft:strength",
  "duration": 600,
  "amplifier": 1,
  "once_per_player": false
}
```

#### Command Effect
```json
{
  "type": "command",
  "id": "unique_effect_id",
  "command": "/give {player} minecraft:diamond 5"
}
```

Placeholders:
- `{player}` - Player username
- `{x}`, `{y}`, `{z}` - Altar coordinates

#### Teleport Recall
```json
{
  "type": "teleport_recall",
  "id": "unique_effect_id",
  "max_uses": 3
}
```

#### Teleport Twin
```json
{
  "type": "teleport_twin",
  "id": "unique_effect_id",
  "twin_id": "other_altar_id"
}
```

#### Prayer (Coming Soon)
```json
{
  "type": "prayer_displayed|prayer_hidden",
  "id": "unique_effect_id",
  "prayer_text": "Text to recite",
  "success_effects": []
}
```

## File Locations

Altar definitions should be placed in:
```
config/menhir/altars/*.json
```

## Implementation Classes

### API Classes
- `AltarDefinition` - Main altar configuration
- `AltarRarity` - Rarity tier enum
- `AltarEffect` - Base effect class with implementations
- `AltarRequirements` - Requirements configuration
- `GuardianSpawn` - Guardian spawn configuration

### Core Classes
- `AltarRegistry` - Loads and manages altar definitions
- `BlockAltar` - Generic altar block that delegates to effect handlers
- `TileEntityAltar` - Stores altar state and usage data using generic NBT storage
- `AltarWorldGenerator` - Spawns altars in world generation
- `AltarEffectHandlerRegistry` - Registry for custom effect handlers
- `IAltarEffectHandler` - Interface for custom altar interaction logic

### Architecture: Handler Pattern

The altar system uses a **handler pattern** to support different altar behaviors without cluttering the block class:

**BlockAltar** (Generic)
- Handles common altar logic (channeling, requirements, guardians)
- Delegates to `AltarEffectHandlerRegistry` to find the appropriate handler
- Falls back to standard effect processing if no handler exists

**IAltarEffectHandler** (Interface)
- `canHandle(AltarDefinition)` - Check if handler should process this altar
- `handleInteraction(...)` - Custom interaction logic (returns true if fully handled)
- `postInteraction(...)` - Optional post-processing after standard effects

**Example Handlers:**
- `TeleportTwinHandler` - Generates twin altar on first use, teleports between them

### Adding a New Altar Type

1. **Create a handler class** implementing `IAltarEffectHandler`:
```java
public class MyCustomHandler implements IAltarEffectHandler {
    @Override
    public String getEffectType() {
        return "my_custom_effect";
    }
    
    @Override
    public boolean canHandle(AltarDefinition definition) {
        // Check if this altar uses your effect type
        return definition.getEffects().stream()
            .anyMatch(e -> e instanceof MyCustomEffect);
    }
    
    @Override
    public boolean handleInteraction(World world, BlockPos pos, 
                                      TileEntityAltar altar,
                                      AltarDefinition definition, 
                                      EntityPlayer player) {
        // Your custom logic here
        // Return true to skip standard effect processing
        // Return false to continue with standard effects
        
        // Update usage counts if you return true:
        altar.incrementPlayerUses(player.getUniqueID());
        altar.incrementTotalUses();
        
        return true;
    }
}
```

2. **Register the handler** in `Menhir.preInit()`:
```java
AltarEffectHandlerRegistry.registerHandler(new MyCustomHandler());
```

3. **Store custom data** using the tile entity's generic storage:
```java
NBTTagCompound customData = new NBTTagCompound();
customData.setString("my_key", "my_value");
altar.setDataValue("my_effect_data", customData);

// Later retrieve it:
NBTTagCompound data = altar.getDataValue("my_effect_data");
```

### Benefits of This Architecture
- ✓ Single block type, no need to register multiple blocks
- ✓ Clean separation of concerns
- ✓ Easy to add new altar types without modifying core classes
- ✓ Handlers are stateless and testable
- ✓ JSON configurations remain simple
- ✓ Can mix custom handlers with standard effects

## Example Altars

See the `config/menhir/altars/` directory for complete examples of each rarity tier:
- `altar_of_restoration.json` - Common tier
- `altar_of_the_warrior.json` - Uncommon tier
- `altar_of_shadows.json` - Rare tier
- `altar_of_fortune.json` - Epic tier
- `altar_of_ascension.json` - Legendary tier
- `altar_of_passage.json` - Twin teleport example

## Implemented Effect Types

### 1. Potion Effects (Simple)
Apply potion effects to players. Handled by standard `PotionEffect.apply()`.
- **No handler needed** - works directly in effect class

### 2. Command Effects (Simple)
Execute server commands with placeholders (`{player}`, `{x}`, `{y}`, `{z}`).
- **No handler needed** - works directly in effect class

### 3. Teleport Twin (Handler: TeleportTwinHandler)
- First use: Generates twin altar 1000-5000 blocks away
- Subsequent uses: Teleports between twin altars
- Cross-dimension teleportation supported
- Data stored in: `altar.getDataValue("teleport_twin")`

### 4. Prayer (Handler: PrayerHandler)
- Displays prayer text to player (or hidden mode)
- Applies success effects (potions, commands, etc.)
- Per-player cooldown (default 1 Minecraft day = 20 minutes)
- Data stored in: `altar.getDataValue("prayer_data")`
- Future: Could require typing prayer text in chat

### 5. Teleport Recall (Handler: TeleportRecallHandler)
- Marks location in player's NBT data
- Player can recall via command: `/menhir recall <altar_id>` (not yet implemented)
- Tracks remaining uses per player
- Data stored in: player NBT `MenhirRecallAltars`

### 6. Temporary Item (Handler: TemporaryItemHandler)
- Gives player items that expire after a duration
- Items marked with special NBT tags
- Can be set to remove on logout
- Supports enchantments, custom names, and NBT
- Tracks items in player NBT `MenhirTemporaryItems`
- **Note:** Requires event handler to actually remove expired items (TODO)

## TODO / Future Enhancements

1. **Temporary Item Cleanup** - Event handler to remove expired temporary items from player inventory
2. **Recall Command** - Implement `/menhir recall` command to use stored recall points
3. **Recall Item** - Create item that can trigger recall without command
4. **Prayer Text Input** - Require player to type prayer in chat for authentication
5. **Mob Spawner Type** - Add continuous spawning guardian type
6. **Chaotic Altars** - Daily effect rebinding system
7. **World Tracking** - Implement unique-per-world enforcement
8. **Custom Altar Models** - Add proper 3D models and textures
9. **Sound Effects** - Add custom sounds for different rarity tiers
10. **Particle Effects** - Visual feedback for altar activation
11. **Config GUI** - In-game configuration interface

## Completed Features

- ✓ **All 6 Effect Types Implemented:**
  - Potion Effects (simple)
  - Command Effects (simple)
  - Teleport Twin (handler)
  - Prayer (handler)
  - Teleport Recall (handler)
  - Temporary Item (handler)
- ✓ Twin Teleportation - Fully implemented with handler
- ✓ Prayer System - Implemented with cooldowns and success effects
- ✓ Recall Marking - Stores location in player data (command to use it needed)
- ✓ Temporary Items - Gives items with expiry tracking (cleanup event needed)
- ✓ Generic data storage - Tile entity uses NBT compound for flexible altar-specific data
- ✓ Handler architecture - Clean, extensible effect handling system
