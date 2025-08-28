package com.windanesz.menhir.api;

import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.List;
import java.util.Map;

public class Birthsign extends IForgeRegistryEntry.Impl<Birthsign> {
	public static IForgeRegistry<Birthsign> registry;

	public String name;
	public List<BirthsignEffect> passive;
	public List<BirthsignEffect> active;

	// New standardized active ability list
	public List<IBirthsignActiveAbility> activeAbilities;

	// Spell modifiers for this birthsign
	public Map<String, Float> spell_modifiers;

	// Maximum daily uses for active abilities
	public int active_daily_uses;

	// Maximum daily uses for passive abilities
	public int passive_daily_uses;

	// List of required mod IDs for this birthsign to load
	public List<String> required_mods;

	public Birthsign() {
	}

	public Birthsign(String name) {
		this.name = name;
	}

	public static void createRegistry() {
		registry = new RegistryBuilder<Birthsign>().setName(new net.minecraft.util.ResourceLocation("menhir:birthsigns")).setType(Birthsign.class).setIDRange(0, 256).create();
	}

	/**
	 * This can return null
	 */
	public static Birthsign getBirthsignFromString(String birthsign) {
		return registry.getValue(new net.minecraft.util.ResourceLocation(birthsign));
	}

	/**
	 * Enum for the different types of effects that can be applied.
	 */
	public enum EffectType {
		ATTRIBUTE_MODIFIER("attribute_modifier"),
		BLAZE_FIREBALL("blaze_fireball"),
		SPELL_CAST("spell_cast"),
		POTION_EFFECT("potion_effect"),
		WIZARDRY_SPELL_MODIFIER("wizardry_spell_modifier"),
		FALL_DAMAGE_REDUCTION("fall_damage_reduction"),
		SPATIAL_SLIP("spatial_slip"),
		FIRE_IMMUNITY("fire_immunity"),
		PARTICLE_EFFECT("particle_effect"),
		CHANNELING_TELEPORT("channeling_teleport"),
		ARCANE_ECHO("arcane_echo"),
		THREAT_SENSE("threat_sense"),
		REVELATION("revelation"),
		BURNING_ATTACK("burning_attack"),
		REPAIR_ITEM("repair_item"),
		HERO_OF_VILLAGE("hero_of_village"),
		VERDANT_BOND("verdant_bond"),
		NATURES_EMBRACE("natures_embrace"),
		UNDERGROUND_HASTE("underground_haste"),
		GIVE_ITEM("give_item"),
		BLOCK_PLACEMENT("block_placement"),
		HEAL_ON_KILL("heal_on_kill"),
		COMMAND_ABILITY("command_ability");

		private final String jsonName;

		EffectType(String jsonName) {
			this.jsonName = jsonName;
		}

		public static EffectType fromJsonName(String jsonName) {
			for (EffectType type : values()) {
				if (type.jsonName.equals(jsonName)) {
					return type;
				}
			}
			return null;
		}

		public String getJsonName() {
			return this.jsonName;
		}
	}

	/**
	 * Represents a single effect in a birthsign's passive, active, or weakness list.
	 */
	public static class BirthsignEffect {
		public boolean datadriven;
		public EffectDetail effect;
	}

	/**
	 * Represents the details of a birthsign effect.
	 * Uses a dynamic map to store all parameters instead of hardcoded fields.
	 */
	public static class EffectDetail {
		public EffectType type;

		// Dynamic parameter storage - no need to define every field
		public java.util.Map<String, Object> parameters;

		// Constructor to initialize the parameters map
		public EffectDetail() {
			this.parameters = new java.util.HashMap<>();
		}

		// Helper method to get a parameter with type safety
		@SuppressWarnings("unchecked")
		public <T> T getParameter(String key, T defaultValue) {
			Object value = parameters.get(key);
			if (value != null && defaultValue.getClass().isInstance(value)) {
				return (T) value;
			}
			return defaultValue;
		}

		// Helper method to set a parameter
		public void setParameter(String key, Object value) {
			parameters.put(key, value);
		}
	}
} 