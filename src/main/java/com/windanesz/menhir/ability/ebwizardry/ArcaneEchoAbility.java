package com.windanesz.menhir.ability.ebwizardry;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.ability.ChannelingAbility;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import electroblob.wizardry.data.WizardData;
import electroblob.wizardry.item.ItemSpellBook;
import electroblob.wizardry.spell.Spell;
import electroblob.wizardry.util.SpellModifiers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Map;

public class ArcaneEchoAbility extends ChannelingAbility {

	private static final String BOUND_SPELL_KEY = "arcane_echo_spell";
	private static final String USES_KEY = "arcane_echo_uses";
	private static final int MAX_USES_PER_DAY = 1;

	public ArcaneEchoAbility(int chargeup) {
		super(chargeup);
	}

	public static ArcaneEchoAbility create(Map<String, Object> params, String birthsignName) {
		int chargeup = getChargeup(params, 0);
		return new ArcaneEchoAbility(chargeup);
	}

	@Override
	protected boolean executeAbility(EntityPlayer player, @Nullable Entity target) {
		IBirthsignData birthsignData = BirthsignDataProvider.get(player);
		if (birthsignData == null) return false;

		String boundSpell = birthsignData.getString(BOUND_SPELL_KEY);
		ItemStack heldItem = player.getHeldItemMainhand();

		if (shouldBindSpell(player, heldItem, birthsignData)) {
			bindSpell(player, heldItem, birthsignData);
			return false;
		}

		if (boundSpell == null || boundSpell.isEmpty()) {
			handleNoSpellBound(player, heldItem);
		} else {
			return castBoundSpell(player, boundSpell, birthsignData);
		}

		return false;
	}

	private boolean shouldBindSpell(EntityPlayer player, ItemStack heldItem, IBirthsignData birthsignData) {
		return isValidSpellBook(heldItem) && player.isSneaking();
	}

	private void bindSpell(EntityPlayer player, ItemStack heldItem, IBirthsignData birthsignData) {
		Spell spell = Spell.byMetadata(heldItem.getItemDamage());
		if (spell == null) {
			Menhir.logger.info("Attempting to bind an invalid spell to ArcaneEcho");
			return;
		}

		birthsignData.setString(BOUND_SPELL_KEY, spell.getRegistryName().toString());
		birthsignData.setInt(USES_KEY, MAX_USES_PER_DAY);

		sendBindingSuccessMessage(player, spell);
		consumeSpellBook(player);
	}

	private void sendBindingSuccessMessage(EntityPlayer player, Spell spell) {
		player.sendMessage(new TextComponentString(
				TextFormatting.GREEN + "Arcane Echo bound to: " +
						TextFormatting.GOLD + spell.getDisplayName()
		));
	}

	private void consumeSpellBook(EntityPlayer player) {
		ItemStack copy = player.inventory.getStackInSlot(player.inventory.currentItem).copy();
		copy.shrink(1);
		player.inventory.setInventorySlotContents(player.inventory.currentItem, copy);
	}

	private void handleNoSpellBound(EntityPlayer player, ItemStack heldItem) {
		if (heldItem.isEmpty()) {
			player.sendMessage(new TextComponentString(
					TextFormatting.RED + "Hold a spell book to bind as your Arcane Echo! (The spell book will be lost)"
			));
		}
	}

	private boolean isValidSpellBook(ItemStack stack) {
		return stack.getItem() instanceof ItemSpellBook;
	}

	private boolean castBoundSpell(EntityPlayer player, String spellName, IBirthsignData birthsignData) {
		Spell spell = Spell.get(spellName);
		if (spell == null) {
			sendSpellNotFoundMessage(player, spellName);
			return false;
		}

		return attemptSpellCast(player, spell);
	}

	private void sendSpellNotFoundMessage(EntityPlayer player, String spellName) {
		player.sendMessage(new TextComponentString(
				TextFormatting.RED + "Error: Could not find bound spell: " + spellName
		));
	}

	private boolean attemptSpellCast(EntityPlayer player, Spell spell) {
		try {
			SpellModifiers modifiers = createFreeSpellModifiers();
			boolean success = spell.cast(player.world, player, EnumHand.MAIN_HAND, 0, modifiers);

			if (success && spell.isContinuous) {
				startContinuousCasting(player, spell, modifiers);
			}

			return success;
		} catch (Exception e) {
			Menhir.logger.error("Error casting spell with Arcane Echo: " + e.getMessage());
			return false;
		}
	}

	private SpellModifiers createFreeSpellModifiers() {
		SpellModifiers modifiers = new SpellModifiers();
		modifiers.set("cost", 0.0f, false); // Free casting
		return modifiers;
	}

	private void startContinuousCasting(EntityPlayer player, Spell spell, SpellModifiers modifiers) {
		WizardData data = WizardData.get(player);
		if (data != null) {
			data.startCastingContinuousSpell(spell, modifiers, 80);
		}
	}

	public void onNewDay(EntityPlayer player, IBirthsignData birthsignData) {
		birthsignData.setInt(USES_KEY, MAX_USES_PER_DAY);
	}
}