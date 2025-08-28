package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import electroblob.wizardry.data.WizardData;
import electroblob.wizardry.event.DiscoverSpellEvent;
import electroblob.wizardry.item.ItemScroll;
import electroblob.wizardry.item.ItemSpellBook;
import electroblob.wizardry.spell.Spell;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;

public class RevelationAbility implements IBirthsignActiveAbility {

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		if (player == null || player.world.isRemote) return false;

		// Check if ElectroBlob's Wizardry is loaded
		if (!Loader.isModLoaded("ebwizardry")) {
			player.sendMessage(new TextComponentTranslation(Menhir.MODID + ":" + "the_seer.ebwizardry_required"));
			return false;
		}

		// Check if player has WizardData
		WizardData data = WizardData.get(player);
		if (data == null) {
			player.sendMessage(new TextComponentTranslation(Menhir.MODID + ":" + "the_seer.no_wizard_data"));
			return false;
		}

		// Check main hand for spell book or scroll
		ItemStack mainHandStack = player.getHeldItemMainhand();
		if (mainHandStack.isEmpty()) {
			player.sendMessage(new TextComponentTranslation(Menhir.MODID + ":" + "the_seer.no_item_mainhand"));
			return false;
		}

		// Check if the item is a spell book or scroll
		if (!(mainHandStack.getItem() instanceof ItemSpellBook) && !(mainHandStack.getItem() instanceof ItemScroll)) {
			player.sendMessage(new TextComponentTranslation(Menhir.MODID + ":" + "the_seer.not_spell_item"));
			return false;
		}

		// Get the spell from the item
		Spell spell = Spell.byMetadata(mainHandStack.getItemDamage());
		if (spell == null) {
			player.sendMessage(new TextComponentTranslation(Menhir.MODID + ":" + "the_seer.invalid_spell"));
			return false;
		}

		// Check if the spell has already been discovered
		if (data.hasSpellBeenDiscovered(spell)) {
			player.sendMessage(new TextComponentTranslation(Menhir.MODID + ":" + "the_seer.spell_already_known", spell.getNameForTranslationFormatted()));
			return false;
		}

		// Fire the discovery event
		if (!MinecraftForge.EVENT_BUS.post(new DiscoverSpellEvent(player, spell, DiscoverSpellEvent.Source.IDENTIFICATION_SCROLL))) {
			// Discover the spell
			data.discoverSpell(spell);
			data.sync();
			// Play discovery sound
			player.playSound(net.minecraft.init.SoundEvents.ENTITY_PLAYER_LEVELUP, 1.25f, 1);

			// Send discovery message
			player.sendMessage(new TextComponentTranslation(Menhir.MODID + ":" + "the_seer.spell_discovered", spell.getNameForTranslationFormatted()));

			// Send success message
			player.sendMessage(new TextComponentTranslation(Menhir.MODID + ":" + "the_seer.revelation_success"));

			return true;
		}
		return false;
	}
}
