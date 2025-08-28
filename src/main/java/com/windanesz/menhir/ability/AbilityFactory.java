package com.windanesz.menhir.ability;

import com.windanesz.menhir.ability.ebwizardry.ArcaneEchoAbility;
import com.windanesz.menhir.ability.ebwizardry.SpellCastAbility;
import com.windanesz.menhir.ability.minercaft.*;
import com.windanesz.menhir.api.IBirthsignActiveAbility;

import java.util.Map;
import java.util.function.BiFunction;

public class AbilityFactory {
	private static final Map<String, BiFunction<Map<String, Object>, String, IBirthsignActiveAbility>> FACTORIES;

	static {
		// Default birthsigns. It is not a problem to put these mappings in place even if the birthsign itself is not registered
		FACTORIES = new java.util.HashMap<>();
		FACTORIES.put("blaze_fireball", BlazeFireballAbility::create);
		FACTORIES.put("blink", BlinkAbility::create);
		FACTORIES.put("potion_effect", PotionEffectAbility::create);
		FACTORIES.put("attribute_modifier", AttributeModifierAbility::create);
		FACTORIES.put("spellshatter", SpellshatterAbility::create);
		FACTORIES.put("lodestone_starbound", TeleportAbility::create);
		FACTORIES.put("particle_effect", ParticleEffectAbility::create);
		FACTORIES.put("channeling_teleport", TeleportAbility::create);
		FACTORIES.put("arcane_echo", (params, birthsignName) -> ArcaneEchoAbility.create(params));
		FACTORIES.put("spell_cast", SpellCastAbility::create);
		FACTORIES.put("revelation", (params, birthsignName) -> new RevelationAbility());
		FACTORIES.put("burning_attack", BurningAttackAbility::create);
		FACTORIES.put("repair_item", RepairItemAbility::create);
		FACTORIES.put("hero_of_village", HeroOfVillageAbility::create);
		FACTORIES.put("verdant_bond", VerdantBondAbility::create);
		FACTORIES.put("natures_embrace", NaturesEmbraceAbility::create);
		FACTORIES.put("underground_haste", (params, birthsignName) -> new UndergroundHasteAbility());
		FACTORIES.put("block_placement", BlockPlacementAbility::create);
		FACTORIES.put("give_item", GetItemAbility::create);
		FACTORIES.put("heal_on_kill", HealOnKillAbility::create);
		FACTORIES.put("command_ability", CommandAbility::create);
	}

	public static void addFactory(String type, BiFunction<Map<String, Object>, String, IBirthsignActiveAbility> factory) {
		FACTORIES.put(type, factory);
	}

	public static IBirthsignActiveAbility create(String type, Map<String, Object> params, String birthsignName) {
		BiFunction<Map<String, Object>, String, IBirthsignActiveAbility> factory = FACTORIES.get(type);
		if (factory == null) throw new IllegalArgumentException("Unknown ability type: " + type);
		return factory.apply(params, birthsignName);
	}
} 