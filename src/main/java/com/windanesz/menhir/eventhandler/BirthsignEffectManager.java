package com.windanesz.menhir.eventhandler;

import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.api.BirthsignAttributeModifier;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class BirthsignEffectManager {

	/**
	 * Reapplies birthsign effects when a player's birthsign changes.
	 * This removes the old birthsign's effects and applies the new birthsign's effects.
	 */
	public static void reapplyBirthsignEffects(EntityPlayer player, String oldBirthsignName, String newBirthsignName) {
		// Remove old birthsign effects
		if (oldBirthsignName != null && !oldBirthsignName.isEmpty()) {
			removeBirthsignEffects(player, oldBirthsignName);
		}

		// Apply new birthsign effects
		if (newBirthsignName != null && !newBirthsignName.isEmpty()) {
			applyPassiveBirthsignEffects(player, newBirthsignName);
			// Recharge charges to full when a new birthsign is assigned
			rechargeBirthsignCharges(player);
		}
	}

	/**
	 * Applies all data-driven effects for a given birthsign to a player, for now this is mostly just attribute modifiers
	 * and potions for the first time (afterward every few ticks in {@link BirthsignCoreEventHandler#onPlayerTick(TickEvent.PlayerTickEvent)}
	 */
	public static void applyPassiveBirthsignEffects(EntityPlayer player, String birthsignName) {
		Birthsign birthsign = Birthsign.registry.getValue(new net.minecraft.util.ResourceLocation(birthsignName));

		if (birthsign == null || birthsign.passive == null) {
			return;
		}

		for (Birthsign.BirthsignEffect effect : birthsign.passive) {
			// only handles data driven effects
			Birthsign.EffectDetail eff = effect.effect;
			if (eff.type == Birthsign.EffectType.ATTRIBUTE_MODIFIER) {
				String attribute = eff.getParameter("attribute", "");
				double amount = eff.getParameter("amount", 0.0);
				int operation = getOperation(String.valueOf(eff.getParameter("operation", 0)));
				   BirthsignAttributeModifier mod = new BirthsignAttributeModifier(attribute, amount, operation, birthsign.name);
				   mod.apply(player, birthsign.name);
			} else if (eff.type == Birthsign.EffectType.FALL_DAMAGE_REDUCTION) {
				// Fall damage reduction effects are handled by the event handler
				// This just marks that the player has this ability
				// The actual fall damage handling is done in BirthsignAbilityEventHandler
			} else if (eff.type == Birthsign.EffectType.SPATIAL_SLIP) {
				// Spatial slip effects are handled by the event handler
				// This just marks that the player has this ability
				// The actual spatial slip handling is done in BirthsignAbilityEventHandler
			} else if (eff.type == Birthsign.EffectType.FIRE_IMMUNITY) {
				// Fire immunity effects are handled by the event handler
				// This just marks that the player has this ability
				// The actual fire immunity handling is done in BirthsignAbilityEventHandler
			} else if (eff.type == Birthsign.EffectType.UNDERGROUND_HASTE) {
				// Underground haste effects are handled by the event handler
				// This just marks that the player has this ability
				// The actual underground haste handling is done in BirthsignAbilityEventHandler
			} else if (eff.type == Birthsign.EffectType.BLOCK_PLACEMENT) {
				// Block placement effects are handled by the event handler
				// This just marks that the player has this ability
				// The actual block placement handling is done in BirthsignAbilityEventHandler
			}
		}
	}

	/**
	 * Gets the remaining birthsign active charges for the player.
	 */
	public static int getBirthsignRemainingCharges(net.minecraft.entity.player.EntityPlayer player) {
		IBirthsignData data = BirthsignDataProvider.get(player);
		return data != null ? data.getInt("birthsign_remaining_charges") : 0;
	}

	/**
	 * Sets the remaining birthsign active charges for the player.
	 */
	public static void setBirthsignRemainingCharges(net.minecraft.entity.player.EntityPlayer player, int value) {
		IBirthsignData data = BirthsignDataProvider.get(player);
		if (data != null) {
			data.setInt("birthsign_remaining_charges", value);
		}
	}

	/**
	 * Decrements the remaining birthsign active charges for the player by 1.
	 * Returns the new value.
	 */
	public static void decrementBirthsignRemainingCharges(EntityPlayer player) {
		int current = getBirthsignRemainingCharges(player);
		int newValue = current - 1;
		setBirthsignRemainingCharges(player, newValue);
			}

	/**
	 * Gets the remaining birthsign passive charges for the player.
	 */
	public static int getBirthsignRemainingPassiveCharges(net.minecraft.entity.player.EntityPlayer player) {
		IBirthsignData data = BirthsignDataProvider.get(player);
		return data != null ? data.getInt("birthsign_remaining_passive_charges") : 0;
	}

	/**
	 * Sets the remaining birthsign passive charges for the player.
	 */
	public static void setBirthsignRemainingPassiveCharges(net.minecraft.entity.player.EntityPlayer player, int value) {
		IBirthsignData data = BirthsignDataProvider.get(player);
		if (data != null) {
			data.setInt("birthsign_remaining_passive_charges", value);
		}
	}

	/**
	 * Decrements the remaining birthsign passive charges for the player by 1.
	 * Returns the new value.
	 */
	public static void decrementBirthsignRemainingPassiveCharges(EntityPlayer player) {
		int current = getBirthsignRemainingPassiveCharges(player);
		int newValue = current - 1;
		setBirthsignRemainingPassiveCharges(player, newValue);
	}

	/**
	 * Gets the maximum birthsign active charges for the player's birthsign.
	 */
	public static int getBirthsignMaxCharges(EntityPlayer player) {
		IBirthsignData data = BirthsignDataProvider.get(player);
		if (data == null) return 0;

		String birthsign = data.getBirthsign();
		if (birthsign == null || birthsign.isEmpty()) return 0;

		Birthsign birthsignFromString = Birthsign.getBirthsignFromString(birthsign);
		return birthsignFromString != null ? birthsignFromString.active_daily_uses : 0;
	}

	/**
	 * Gets the maximum birthsign passive charges for the player's birthsign.
	 */
	public static int getBirthsignMaxPassiveCharges(EntityPlayer player) {
		IBirthsignData data = BirthsignDataProvider.get(player);
		if (data == null) return 0;

		String birthsignName = data.getBirthsign();
		if (birthsignName == null || birthsignName.isEmpty()) return 0;

		Birthsign birthsign = Birthsign.getBirthsignFromString(birthsignName);
		return birthsign != null ? birthsign.passive_daily_uses : 0;
	}

	/**
	 * Recharges the player's birthsign active charges to full capacity.
	 */
	public static void rechargeBirthsignCharges(EntityPlayer player) {
		int maxCharges = getBirthsignMaxCharges(player);
		int currentCharges = getBirthsignRemainingCharges(player);

		// Only send message if charges were actually recharged
		if (currentCharges < maxCharges) {
			setBirthsignRemainingCharges(player, maxCharges);
			player.sendMessage(new net.minecraft.util.text.TextComponentString("§aYour birthsign active abilities have been recharged!"));
					}
	}

	/**
	 * Recharges the player's birthsign passive charges to full capacity.
	 */
	public static void rechargeBirthsignPassiveCharges(EntityPlayer player) {
		int maxCharges = getBirthsignMaxPassiveCharges(player);
		int currentCharges = getBirthsignRemainingPassiveCharges(player);

		// Only send message if charges were actually recharged
		if (currentCharges < maxCharges) {
			setBirthsignRemainingPassiveCharges(player, maxCharges);
			player.sendMessage(new net.minecraft.util.text.TextComponentString("§aYour birthsign passive abilities have been recharged!"));
		}
	}

	/**
	 * Gets a formatted string showing the player's current and maximum birthsign charges.
	 */
	public static String getBirthsignChargesStatus(EntityPlayer player) {
		int current = getBirthsignRemainingCharges(player);
		int max = getBirthsignMaxCharges(player);
		return current + "/" + max + " charges remaining";
	}

	/**
	 * Gets a formatted string showing the player's current and maximum birthsign passive charges.
	 */
	public static String getBirthsignPassiveChargesStatus(EntityPlayer player) {
		int current = getBirthsignRemainingPassiveCharges(player);
		int max = getBirthsignMaxPassiveCharges(player);
		return current + "/" + max + " passive charges remaining";
	}

	public static void applyBirthsignActiveEffects(EntityPlayer player, String birthsignName) {
		if (getBirthsignRemainingCharges(player) <= 0) {
			// Send message to player that they have no charges remaining
			player.sendMessage(new net.minecraft.util.text.TextComponentString(
				net.minecraft.util.text.TextFormatting.RED + "You have no active ability charges remaining!"
			));
			return;
		}
		boolean decrement = false;
		Birthsign birthsignFromString = Birthsign.getBirthsignFromString(birthsignName);
		if (birthsignFromString == null || birthsignFromString.activeAbilities == null) {
			return;
		}
		for (IBirthsignActiveAbility ability : birthsignFromString.activeAbilities) {
			if (ability.activate(player, null)) {
				decrement = true;
			}
		}

		if (decrement) {
			decrementBirthsignRemainingCharges(player);
			
			// Sync to client with full capability data
			if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
				IBirthsignData data = BirthsignDataProvider.get(player);
				net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
				if (data != null) {
					data.writeToNBT(nbt);
				}
				com.windanesz.menhir.network.NetworkHandler.INSTANCE.sendTo(
					new com.windanesz.menhir.network.PacketSyncBirthsignData(birthsignName, nbt), 
					(net.minecraft.entity.player.EntityPlayerMP) player
				);
			}
		}
	}

	/**
	 * Removes all effects for a given birthsign from a player.
	 */
	public static void removeBirthsignEffects(EntityPlayer player, String birthsignName) {
		Birthsign birthsignFromString = Birthsign.getBirthsignFromString(birthsignName);

		if (birthsignFromString == null || birthsignFromString.passive == null) {
			return;
		}

		for (Birthsign.BirthsignEffect effect : birthsignFromString.passive) {

			Birthsign.EffectDetail eff = effect.effect;
			if (eff.type == Birthsign.EffectType.ATTRIBUTE_MODIFIER) {
				String attribute = eff.getParameter("attribute", "");
				double amount = eff.getParameter("amount", 0.0);
				int operation = getOperation(String.valueOf(eff.getParameter("operation", 0)));
				   BirthsignAttributeModifier mod = new BirthsignAttributeModifier(attribute, amount, operation, birthsignFromString.name);
				   mod.remove(player, birthsignFromString.name);
			}
		}
	}

	/**
	 * Helper to convert operation string to int
	 */
	private static int getOperation(String op) {
		switch (op) {
			case "multiply_base":
			case "1":
				return 1;
			case "multiply_total":
			case "2":
				return 2;
			default:
				return 0;
		}
	}
} 