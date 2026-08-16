package com.windanesz.menhir.eventhandler;

import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.api.BirthsignAttributeModifier;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BirthsignEffectManager {

	/**
	 * Helper method to get all active traits (birthsigns, races, etc.) for a player
	 */
	public static List<Birthsign> getAllActiveTraits(EntityPlayer player) {
		List<Birthsign> traits = new ArrayList<>();
		IBirthsignData data = BirthsignDataProvider.get(player);
		if (data != null) {
			Map<String, String> selectedTraits = data.getAllTraits();
			List<String> traitNames = new ArrayList<>(selectedTraits.values());
			// Sort trait names to ensure deterministic order
			java.util.Collections.sort(traitNames);
			
			for (String traitName : traitNames) {
				if (traitName != null && !traitName.isEmpty()) {
					Birthsign trait = Birthsign.getBirthsignFromString(traitName);
					if (trait != null) {
						traits.add(trait);
					}
				}
			}
		}
		return traits;
	}

	/**
	 * Gets a flattened list of all active abilities from all traits for the player.
	 * The order is deterministic based on the sorted traits.
	 * Abilities with the same 'ability_name' (group name) are grouped together into a single entry.
	 */
	public static List<IBirthsignActiveAbility> getFlattenedAbilities(EntityPlayer player) {
		List<IBirthsignActiveAbility> result = new ArrayList<>();
		java.util.Map<String, com.windanesz.menhir.ability.GroupedActiveAbility> groups = new java.util.HashMap<>();
		
		List<Birthsign> traits = getAllActiveTraits(player);
		for (Birthsign trait : traits) {
			if (trait.activeAbilities != null) {
				for (IBirthsignActiveAbility ability : trait.activeAbilities) {
					String groupName = ability.getGroupName();
					if (groupName != null && !groupName.isEmpty()) {
						if (!groups.containsKey(groupName)) {
							com.windanesz.menhir.ability.GroupedActiveAbility wrapper = new com.windanesz.menhir.ability.GroupedActiveAbility(groupName);
							wrapper.addChild(ability);
							groups.put(groupName, wrapper);
							result.add(wrapper);
						} else {
							groups.get(groupName).addChild(ability);
						}
					} else {
						result.add(ability);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Reapplies trait effects when a player's trait changes.
	 * This removes the old trait's effects and applies the new trait's effects.
	 */
	public static void reapplyTraitEffects(EntityPlayer player, String oldTraitName, String newTraitName) {
		// Remove old birthsign effects
		if (oldTraitName != null && !oldTraitName.isEmpty()) {
			removeBirthsignEffects(player, oldTraitName);
		}

		// Apply new birthsign effects
		if (newTraitName != null && !newTraitName.isEmpty()) {
			applyPassiveBirthsignEffects(player, newTraitName);
			// Recharge charges to full when a new birthsign is assigned
			rechargeBirthsignCharges(player);
		}
	}
	
	@Deprecated
	public static void reapplyBirthsignEffects(EntityPlayer player, String oldBirthsignName, String newBirthsignName) {
		reapplyTraitEffects(player, oldBirthsignName, newBirthsignName);
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
	 * Helper to get the category of an ability
	 */
	public static String getAbilityCategory(IBirthsignActiveAbility ability) {
		if (ability == null) return "birthsign";
		String parentName = ability.getParentName();
		if (parentName == null || parentName.isEmpty()) return "birthsign";
		Birthsign birthsign = Birthsign.getBirthsignFromString(parentName);
		if (birthsign == null) return "birthsign";
		return birthsign.category != null ? birthsign.category : "birthsign";
	}

	/**
	 * Gets the remaining birthsign active charges for the player.
	 * Default implementation for backward compatibility (assumes "birthsign" category).
	 */
	public static int getBirthsignRemainingCharges(net.minecraft.entity.player.EntityPlayer player) {
		return getBirthsignRemainingCharges(player, "birthsign");
	}

	/**
	 * Gets the remaining active charges for a specific category.
	 */
	public static int getBirthsignRemainingCharges(net.minecraft.entity.player.EntityPlayer player, String category) {
		IBirthsignData data = BirthsignDataProvider.get(player);
		String key = "birthsign".equals(category) ? "birthsign_remaining_charges" : "birthsign_remaining_charges_" + category;
		return data != null ? data.getInt(key) : 0;
	}

	/**
	 * Sets the remaining birthsign active charges for the player.
	 */
	public static void setBirthsignRemainingCharges(net.minecraft.entity.player.EntityPlayer player, int value) {
		setBirthsignRemainingCharges(player, "birthsign", value);
	}

	public static void setBirthsignRemainingCharges(net.minecraft.entity.player.EntityPlayer player, String category, int value) {
		IBirthsignData data = BirthsignDataProvider.get(player);
		if (data != null) {
			String key = "birthsign".equals(category) ? "birthsign_remaining_charges" : "birthsign_remaining_charges_" + category;
			data.setInt(key, value);
		}
	}

	/**
	 * Decrements the remaining birthsign active charges for the player by 1.
	 */
	public static void decrementBirthsignRemainingCharges(EntityPlayer player, IBirthsignActiveAbility ability) {
		decrementBirthsignRemainingCharges(player, getAbilityCategory(ability));
	}

	public static void decrementBirthsignRemainingCharges(EntityPlayer player, String category) {
		int current = getBirthsignRemainingCharges(player, category);
		int newValue = current - 1;
		setBirthsignRemainingCharges(player, category, newValue);
	}

	/**
	 * @deprecated Use {@link #decrementBirthsignRemainingCharges(EntityPlayer, IBirthsignActiveAbility)} instead
	 */
	@Deprecated
	public static void decrementBirthsignRemainingCharges(EntityPlayer player) {
		decrementBirthsignRemainingCharges(player, "birthsign");
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
	 * Gets the maximum birthsign active charges for the player's birthsign (sum of all).
	 */
	public static int getBirthsignMaxCharges(EntityPlayer player) {
		int total = 0;
		for (Birthsign trait : getAllActiveTraits(player)) {
			total += trait.active_daily_uses;
		}
		return total;
	}

	public static int getBirthsignMaxCharges(EntityPlayer player, String category) {
		int total = 0;
		for (Birthsign trait : getAllActiveTraits(player)) {
			String traitCategory = trait.category != null ? trait.category : "birthsign";
			if (traitCategory.equals(category)) {
				total += trait.active_daily_uses;
			}
		}
		return total;
	}

	/**
	 * Gets the maximum birthsign passive charges for the player's birthsign.
	 */
	public static int getBirthsignMaxPassiveCharges(EntityPlayer player) {
		int total = 0;
		for (Birthsign trait : getAllActiveTraits(player)) {
			total += trait.passive_daily_uses;
		}
		return total;
	}

	/**
	 * Recharges the player's birthsign active charges to full capacity.
	 */
	public static void rechargeBirthsignCharges(EntityPlayer player) {
		List<Birthsign> traits = getAllActiveTraits(player);
		List<String> categories = new ArrayList<>();
		for (Birthsign trait : traits) {
			String cat = trait.category != null ? trait.category : "birthsign";
			if (!categories.contains(cat)) {
				categories.add(cat);
			}
		}

		boolean rechargedAny = false;
		for (String cat : categories) {
			int max = getBirthsignMaxCharges(player, cat);
			int current = getBirthsignRemainingCharges(player, cat);
			if (current < max) {
				setBirthsignRemainingCharges(player, cat, max);
				rechargedAny = true;
			}
		}

		// Only send message if charges were actually recharged
		if (rechargedAny) {
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
		StringBuilder sb = new StringBuilder();
		
		List<Birthsign> traits = getAllActiveTraits(player);
		List<String> categories = new ArrayList<>();
		for (Birthsign trait : traits) {
			String cat = trait.category != null ? trait.category : "birthsign";
			if (!categories.contains(cat)) {
				categories.add(cat);
			}
		}

		if (categories.isEmpty()) return "0/0 charges remaining";

		for (int i = 0; i < categories.size(); i++) {
			String cat = categories.get(i);
			int current = getBirthsignRemainingCharges(player, cat);
			int max = getBirthsignMaxCharges(player, cat);
			sb.append(current).append("/").append(max);
			if (!"birthsign".equals(cat)) {
				sb.append(" (").append(cat).append(")");
			}
			if (i < categories.size() - 1) {
				sb.append(", ");
			}
		}
		sb.append(" charges remaining");
		return sb.toString();
	}

	/**
	 * Gets a formatted string showing the player's current and maximum birthsign passive charges.
	 */
	public static String getBirthsignPassiveChargesStatus(EntityPlayer player) {
		int current = getBirthsignRemainingPassiveCharges(player);
		int max = getBirthsignMaxPassiveCharges(player);
		return current + "/" + max + " passive charges remaining";
	}

	public static void applyBirthsignActiveEffects(EntityPlayer player, String ignoredBirthsignName) {
		IBirthsignData data = BirthsignDataProvider.get(player);
		int selectedIndex = data != null ? data.getInt("selected_ability_index") : 0;
		
		List<IBirthsignActiveAbility> abilities = getFlattenedAbilities(player);
		if (abilities.isEmpty()) {
			return;
		}
		
		if (selectedIndex < 0 || selectedIndex >= abilities.size()) {
			selectedIndex = 0;
			// Update the index if it was invalid
			if (data != null) data.setInt("selected_ability_index", 0);
		}
		
		IBirthsignActiveAbility ability = abilities.get(selectedIndex);

		String category = getAbilityCategory(ability);
		if (getBirthsignRemainingCharges(player, category) <= 0) {
			// Send message to player that they have no charges remaining
			player.sendMessage(new net.minecraft.util.text.TextComponentString(
				net.minecraft.util.text.TextFormatting.RED + "You have no active ability charges remaining" + 
				(!"birthsign".equals(category) ? " for " + category : "") + "!"
			));
			return;
		}
		
		boolean decrement = ability.activate(player, null);

		if (decrement) {
			decrementBirthsignRemainingCharges(player, ability);
			
			// Sync to client with full capability data
			if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
				net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
				if (data != null) {
					data.writeToNBT(nbt);
				}
				// We send the primary birthsign as the key, or just any valid string, the packet handler needs it
				// to update the GUI maybe? 
				// Actually packet sync just updates the data on client.
				String primary = data != null ? data.getBirthsign() : "";
				com.windanesz.menhir.network.NetworkHandler.INSTANCE.sendTo(
					new com.windanesz.menhir.network.PacketSyncBirthsignData(primary, nbt), 
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