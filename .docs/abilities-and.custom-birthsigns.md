# Menhir Abilities and Custom Birthsigns Documentation

This document lists the available abilities in the Menhir mod and their configurable properties. Abilities are assigned to birthsigns as passive or active abilities. A birthsign can have as many passive or active abilities as desired, but active abilities are all triggered at the same time. These properties are defined in the JSON files located in `src/main/resources/assets/menhir/birthsigns`.

Custom birthsigns can be added by creating new JSON files in the `config/menhir/` directory. The mod will automatically load them at startup.

**About Ability Usage Types:**
- **Passive only**: These abilities work automatically in the background and must be placed in the `"passive"` array.
- **Active only**: These abilities must be manually triggered by the player and must be placed in the `"active"` array.
- **Passive or Active**: These abilities can be used either way depending on configuration (e.g., `potion_effect` can provide permanent buffs as passive or temporary buffs as active).

**About Passive Ability Charges:**

Some passive abilities consume charges when they trigger. These are reactive abilities that activate automatically in response to specific conditions (e.g., taking fire damage, falling from a height). Birthsigns with charge-consuming passive abilities must specify `"passive_daily_uses"` in their JSON configuration.

- **Charge-consuming passive abilities**: `fire_immunity`, `spatial_slip`
- **Always-active passive abilities**: All other passive abilities work continuously without consuming charges

## Common Properties

Most active abilities support the following common property:

-   `chargeup`: (Integer) The time in ticks the player must channel the ability before it activates. A value of `0` makes the activation instant, but for instant activations it can also be omitted.

---

## Minecraft Abilities (`minercaft`)

### AOEPotionEffectAbility

**Type:** `aoe_potion_effect`  
**Usage:** Active only

Applies a potion effect to entities in an area around the player.

-   `potioneffect`: (String) The resource location of the potion effect (e.g., `minecraft:speed`).
-   `amplifier`: (Integer) The amplifier level of the potion effect. `0` is level I.
-   `duration`: (Integer) The duration of the effect in ticks.
-   `radius`: (Double) The radius of the area of effect in blocks.
-   `targets`: (Array of Strings) Who to target. Can be `SELF`, `ALLIES`, `ENEMIES`.

### AttributeModifierAbility

**Type:** `attribute_modifier`  
**Usage:** Passive only

Applies a permanent attribute modifier to the player.

-   `attribute`: (String) The name of the attribute to modify (e.g., `minecraft:generic.max_health`).
-   `amount`: (Double) The value of the modifier.
-   `operation`: (Integer) The operation to apply: `0` for additive, `1` for multiplicative base, `2` for multiplicative total.
-   `attributeClass`: (String, Optional) The class where the attribute is defined if it's not a standard one.
-   `attributeField`: (String, Optional) The field name of the attribute if it's not a standard one.

### BlazeFireballAbility

**Type:** `blaze_fireball`  
**Usage:** Active only

Launches a small fireball, similar to a Blaze.

-   `speed_multiplier`: (Double) Multiplier for the fireball's speed.
-   `damage_multiplier`: (Double) Multiplier for the fireball's damage.
-   `spawn_distance`: (Double) How far from the player the fireball should spawn.

### BlinkAbility

**Type:** `blink`  
**Usage:** Active only

Teleports the player a short distance in the direction they are looking.

-   `range`: (Double) The maximum teleportation distance in blocks.

### BlockPlacementAbility

**Type:** `block_placement`  
**Usage:** Active only

Places a specified block at the location the player is looking at.

-   `block`: (String) The resource location of the block to place (e.g., `minecraft:torch`).

### BurningAttackAbility

**Type:** `burning_attack`  
**Usage:** Passive only

Gives melee attacks a chance to set the target on fire. This is a passive ability.

-   `ignite_chance`: (Double) The chance (0.0 to 1.0) to ignite the target on each hit.
-   `ignite_duration`: (Integer) The duration of the fire in seconds.

### CommandAbility

**Type:** `command_ability`  
**Usage:** Active only

Executes a server command.

-   `command`: (String) The command to execute. Can use `@p` for the player's name.
-   `execute_as`: (String) How to execute the command. Can be `console` or `player`.
-   `player_placeholder`: (String, Optional) A custom placeholder to be replaced with the player's name.

### FireImmunityAbility

**Type:** `fire_immunity`  
**Usage:** Passive only  
**Charges:** Yes - Consumes one passive charge each time it activates

Grants temporary fire immunity when the player takes fire damage. This ability triggers automatically when needed and requires the birthsign to have `"passive_daily_uses"` configured.

-   `fire_immunity_duration`: (Integer) The duration of fire immunity in ticks (20 ticks = 1 second).

### GetItemAbility

**Type:** `give_item`  
**Usage:** Active only

Gives the player a specified item.

-   `item`: (String) The resource location of the item to give (e.g., `minecraft:apple`).
-   `count`: (Integer) The number of items to give.

### HealOnKillAbility

**Type:** `heal_on_kill`  
**Usage:** Passive only

Heals the player when they kill an entity. This is a passive ability.

-   `amount`: (Double) The amount of health (in half-hearts) to restore on a kill.

### HeroOfVillageAbility

**Type:** `hero_of_village`  
**Usage:** Active only

Charms nearby villagers, causing them to throw items at the player.

-   `duration`: (Integer) The duration of the effect in seconds.

### NaturesEmbraceAbility

**Type:** `natures_embrace`  
**Usage:** Active only

Accelerates the growth of plants and turns dirt into grass in an area around the player.

-   `radius`: (Integer) The radius of the effect in blocks.

### ParticleEffectAbility

**Type:** `particle_effect`  
**Usage:** Active only

Creates a visual particle effect around the player.

-   `particle_type`: (String) The name of the particle type (e.g., `SMOKE_NORMAL`).
-   `particle_count`: (Integer) The number of particles to spawn.
-   `particle_spread_x`: (Double) The spread of particles on the X-axis.
-   `particle_spread_y`: (Double) The spread of particles on the Y-axis.
-   `particle_spread_z`: (Double) The spread of particles on the Z-axis.

### PotionEffectAbility

**Type:** `potion_effect`  
**Usage:** Passive or Active

Applies a potion effect to the player. When used as a passive ability, typically has a long duration or `-1` for permanent effects. When used as an active ability, provides temporary buffs.

-   `potioneffect`: (String) The resource location of the potion effect.
-   `amplifier`: (Integer) The amplifier level of the effect.
-   `duration`: (Integer) The duration of the effect in ticks.

### RepairItemAbility

**Type:** `repair_item`  
**Usage:** Active only

Repairs a damaged item held by the player.

-   `restore_percent`: (Double) The percentage (0.0 to 1.0) of the item's maximum durability to restore.

### RevelationAbility

**Type:** `revelation`  
**Usage:** Active only

Allows the player to learn a spell from a spell book or scroll without consuming it. (Requires EBWizardry)

-   No specific properties other than `chargeup`.

### SpatialSlipAbility

**Type:** `spatial_slip`  
**Usage:** Passive only  
**Charges:** Yes - Consumes one passive charge each time it activates

Passively negates lethal fall damage by teleporting the player to safety. This ability only triggers when fall damage would be fatal and requires the birthsign to have `"passive_daily_uses"` configured.

-   No specific properties.

### SpellshatterAbility

**Type:** `spellshatter`  
**Usage:** Active only

Removes all active potion effects from the player.

-   No specific properties other than `chargeup`.

### TeleportAbility

**Type:** `lodestone_starbound`, `channeling_teleport`  
**Usage:** Active only

Teleports the player to their spawn point.

-   `teleport_destination`: (String, Optional) The destination. Currently only `spawn` is supported.

### ThreatSenseAbility

**Type:** `threat_sense`  
**Usage:** Passive only

Passively highlights nearby hostile entities, making them easier to detect.

-   No specific properties.

### UndergroundHasteAbility

**Type:** `underground_haste`  
**Usage:** Passive only

Passively grants the Haste effect when the player is deep underground and holding a pickaxe.

-   No specific properties.

### VerdantBondAbility

**Type:** `verdant_bond`  
**Usage:** Passive only

Passively accelerates the growth of nearby plants.

-   `radius`: (Integer) The radius of the passive growth effect in blocks.

---

## EBWizardry Abilities (`ebwizardry`)

These abilities require the "Electroblob's Wizardry" mod to be installed.

### ArcaneEchoAbility

**Type:** `arcane_echo`  
**Usage:** Active only

Allows the player to bind a spell from a spell book, which can then be cast once per day for free.

-   No specific properties other than `chargeup`.

### SpellCastAbility

**Type:** `spell_cast`  
**Usage:** Active only

Casts a specified spell from Electroblob's Wizardry with optional spell modifiers.

-   `spell`: (String) The registry name of the spell to cast (e.g., `ebwizardry:magic_missile`).
-   `potency`: (Double, Optional) Spell potency multiplier. Default is 1.0 (no change). Values > 1.0 increase potency, values < 1.0 decrease it.
-   `duration`: (Double, Optional) Spell duration multiplier. Default is 1.0 (no change). Values > 1.0 increase duration, values < 1.0 decrease it.
-   `blast`: (Double, Optional) Spell blast/AoE multiplier. Default is 1.0 (no change). Values > 1.0 increase area of effect, values < 1.0 decrease it.
-   `range`: (Double, Optional) Spell range multiplier. Default is 1.0 (no change). Values > 1.0 increase range, values < 1.0 decrease it.
-   `cooldown`: (Double, Optional) Spell cooldown multiplier. Default is 1.0 (no change). Values > 1.0 increase cooldown, values < 1.0 reduce cooldown.

### WizardrySpellModifierAbility

**Type:** `wizardry_spell_modifier`  
**Usage:** Passive only

Applies permanent modifiers to all spells cast by the player.

-   `name`: (String) The modifier type to apply. Options include `potency`, `duration`, `cost`, `blast`, `range`, `cooldown`.
-   `amount`: (Double) The modifier value. Positive values increase the stat, negative values decrease it. For example, `0.10` for potency increases spell potency by 10%, `-0.20` for cost reduces mana cost by 20%.

---

## Comprehensive Example: The Paladin

This section provides a complete walkthrough for creating a custom birthsign, including the JSON definition, a custom icon, and localization keys.

### 1. Create the Birthsign JSON

First, create a file named `the_paladin.json` in the `config/menhir/` directory. This file defines the core properties and abilities of the birthsign.

**`config/menhir/the_paladin.json`**
```json
{
  "name": "the_paladin",
  "active_daily_uses": 1,
  "passive": [
    {
      "effect": {
        "type": "attribute_modifier",
        "attribute": "attack_damage",
        "amount": 0.10,
        "operation": 1
      }
    },
    {
      "effect": {
        "type": "attribute_modifier",
        "attribute": "armor",
        "amount": 4.0,
        "operation": 0
      }
    }
  ],
  "active": [
    {
      "effect": {
        "type": "potion_effect",
        "potioneffect": "strength",
        "amplifier": 1,
        "duration": 300,
        "chargeup": 20
      }
    },
    {
      "effect": {
        "type": "potion_effect",
        "potioneffect": "minecraft:resistance",
        "amplifier": 0,
        "duration": 200,
        "chargeup": 20
      }
    }
  ]
}
```
This configuration defines:
- A **name**: `"the_paladin"`, used for icons and lang keys.
- **One use per day** for its active ability.
- **Passive effects**: A permanent 10% bonus to attack damage and +4 armor points.
- **Active effects**: An ability that, after a 1-second `chargeup` (20 ticks), applies Strength II for 15 seconds and Resistance I for 10 seconds.

**Note on Passive Charges:**  
This example does not use `"passive_daily_uses"` because the passive abilities (`attribute_modifier`) are always-active and don't consume charges. If you were using charge-consuming passive abilities like `fire_immunity` or `spatial_slip`, you would need to add:
```json
"passive_daily_uses": 3,
```
This would give the player 3 uses per day of their passive ability that recharge at midnight.

### 2. Add Mod Dependencies (Optional)

If your birthsign relies on abilities or effects from other mods, you can make the birthsign only load if those mods are present. Add a `required_mods` array to your JSON file.

For example, if the Paladin's active ability used a potion from the `potioncore` mod, you would modify the JSON like this:

**`config/menhir/the_paladin.json`**
```json
{
  "name": "the_paladin",
  "required_mods": [
    "potioncore"
  ],
  "active_daily_uses": 1,
  "passive": [
    {
      "effect": {
        "type": "attribute_modifier",
        "attribute": "max_health",
        "amount": 0.2,
        "operation": 1
      }
    }
  ],
  "active": [
    {
      "effect": {
        "type": "potion_effect",
        "potioneffect": "potioncore:purity",
        "amplifier": 1,
        "duration": 300
      }
    }
  ]
}
```
If the `potioncore` mod is not installed, the Paladin birthsign will be skipped and will not appear in the game. This prevents errors from missing content. You can list multiple mod IDs in the array.

### 3. Create a Custom Icon

Next, you can give your birthsign a unique icon that appears in the GUI.

1.  Create a 32x32 PNG image for the constellation. For best results, look at the game's default icons to match the style (e.g., yellow stars on a transparent background).
2.  Name the file to match the birthsign name: `the_paladin.png`.
3.  Place the image in the `config/menhir/textures/` directory.

The mod will automatically detect and use this icon.

### 4. Add Localization

You have two options for adding translations to your custom birthsign:

#### Option A: Using Config Directory (Easier, No Resource Pack Needed)

Create a file named `menhir.lang` in the `config/menhir/lang/` directory.

**`config/menhir/lang/menhir.lang`**
```
# Custom Birthsign - The Paladin
birthsign.the_paladin.name=The Paladin
birthsign.the_paladin.desc=A holy warrior, blending martial prowess with divine magic to protect the innocent.
birthsign.the_paladin.lore=The Paladin follows the path of righteousness, a beacon of hope against the encroaching darkness.
birthsign.the_paladin.passive=Passively grants increased attack damage and armor.
birthsign.the_paladin.active=Once per day, can call upon divine might to gain immense strength and resilience for a short time.
```

The mod will automatically load the `menhir.lang` file from this directory when the game starts.

#### Option B: Using a Resource Pack (Traditional Method)

Create or edit a `.lang` file (e.g., `en_us.lang`) within a resource pack.

**`assets/your_resource_pack/lang/en_us.lang`**
```
# Custom Birthsign - The Paladin
birthsign.the_paladin.name=The Paladin
birthsign.the_paladin.desc=A holy warrior, blending martial prowess with divine magic to protect the innocent.
birthsign.the_paladin.lore=The Paladin follows the path of righteousness, a beacon of hope against the encroaching darkness.
birthsign.the_paladin.passive=Passively grants increased attack damage and armor.
birthsign.the_paladin.active=Once per day, can call upon divine might to gain immense strength and resilience for a short time.
```
The game uses these keys to display the name, description, lore, and ability summaries in the birthsign GUI.
