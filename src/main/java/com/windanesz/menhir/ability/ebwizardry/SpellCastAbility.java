package com.windanesz.menhir.ability.ebwizardry;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import electroblob.wizardry.data.WizardData;
import electroblob.wizardry.spell.Spell;
import electroblob.wizardry.util.SpellModifiers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Map;

public class SpellCastAbility implements IBirthsignActiveAbility {

	private final String spellName;
	private final float potency;
	private final float duration;
	private final float blast;
	private final float range;
	private final float cooldown;

	public SpellCastAbility(String spellName, float potency, float duration, float blast, float range, float cooldown) {
		this.spellName = spellName;
		this.potency = potency;
		this.duration = duration;
		this.blast = blast;
		this.range = range;
		this.cooldown = cooldown;
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		String spell = ParameterUtils.getStringParameter(params, "spell", "");
		float potency = (float) ParameterUtils.getDoubleParameter(params, "potency", 1.0);
		float duration = (float) ParameterUtils.getDoubleParameter(params, "duration", 1.0);
		float blast = (float) ParameterUtils.getDoubleParameter(params, "blast", 1.0);
		float range = (float) ParameterUtils.getDoubleParameter(params, "range", 1.0);
		float cooldown = (float) ParameterUtils.getDoubleParameter(params, "cooldown", 1.0);
		return new SpellCastAbility(spell, potency, duration, blast, range, cooldown);
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		if (spellName == null || spellName.isEmpty()) {
			sendNoSpellConfiguredMessage(player);
			return false;
		}

		Spell spell = Spell.get(spellName);
		if (spell == null) {
			sendSpellNotFoundMessage(player, spellName);
			return false;
		}

		return attemptSpellCast(player, spell);
	}

	private void sendNoSpellConfiguredMessage(EntityPlayer player) {
		player.sendMessage(new TextComponentString(
				TextFormatting.RED + "No spell configured for this ability!"
		));
	}

	private void sendSpellNotFoundMessage(EntityPlayer player, String spellName) {
		player.sendMessage(new TextComponentString(
				TextFormatting.RED + "Error: Could not find spell: " + spellName
		));
	}

	private boolean attemptSpellCast(EntityPlayer player, Spell spell) {
		try {
			SpellModifiers modifiers = createSpellModifiers();
			boolean success = spell.cast(player.world, player, EnumHand.MAIN_HAND, 0, modifiers);

			if (success && spell.isContinuous) {
				startContinuousCasting(player, spell, modifiers);
			}

			return success;
		} catch (Exception e) {
			Menhir.logger.error("Error casting spell with SpellCastAbility: " + e.getMessage());
			return false;
		}
	}

	private SpellModifiers createSpellModifiers() {
		SpellModifiers modifiers = new SpellModifiers();
		// Apply configured modifiers if they differ from the default (1.0 = no change)
		if (potency != 1.0f) {
			modifiers.set("potency", potency, true);
		}
		if (duration != 1.0f) {
			modifiers.set("duration", duration, true);
		}
		if (blast != 1.0f) {
			modifiers.set("blast", blast, true);
		}
		if (range != 1.0f) {
			modifiers.set("range", range, true);
		}
		if (cooldown != 1.0f) {
			modifiers.set("cooldown", cooldown, true);
		}
		return modifiers;
	}

	private void startContinuousCasting(EntityPlayer player, Spell spell, SpellModifiers modifiers) {
		WizardData data = WizardData.get(player);
		if (data != null) {
			data.startCastingContinuousSpell(spell, modifiers, 80);
		}
	}

}
