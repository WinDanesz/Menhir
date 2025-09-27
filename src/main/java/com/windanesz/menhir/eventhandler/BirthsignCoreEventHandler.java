package com.windanesz.menhir.eventhandler;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.Settings;
import com.windanesz.menhir.ability.minercaft.UndergroundHasteAbility;
import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.api.BirthsignAttributeModifier;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

@Mod.EventBusSubscriber
public class BirthsignCoreEventHandler {

	private static final int PASSIVE_EFFECT_REAPPLY_INTERVAL = 200; // 10 seconds (200 ticks)

	@SubscribeEvent
	public static void onPlayerLoggedIn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
		EntityPlayer player = event.player;
		String birthsignName = getPlayerBirthsign(player);

		// Check if random birthsign assignment is enabled and player doesn't have a birthsign
		if (birthsignName == null || birthsignName.isEmpty()) {
			if (Settings.generalSettings.random_birthsign_assignment) {
				// Assign a random birthsign to the player
				String randomBirthsign = assignRandomBirthsign(player);
				if (randomBirthsign != null) {
					birthsignName = randomBirthsign;

					// Show message if enabled
					if (Settings.generalSettings.show_random_birthsign_message) {
						// Extract the birthsign name without modid prefix for translation
						String birthsignNameForTranslation = randomBirthsign;
						if (randomBirthsign.contains(":")) {
							birthsignNameForTranslation = randomBirthsign.split(":")[1];
						}

						// Get the localized birthsign name
						String localizedBirthsignName = Menhir.proxy.translate("birthsign." + birthsignNameForTranslation + ".name");

						player.sendMessage(new TextComponentString(
								TextFormatting.GOLD +
										String.format("You have been assigned the birthsign: %s", localizedBirthsignName)
						));
					}

					if (Menhir.logger != null) {
						Menhir.logger.info("Assigned random birthsign '{}' to player: {}", randomBirthsign, player.getName());
					}
				}
			}
		}

		if (birthsignName != null && !birthsignName.isEmpty()) {
			BirthsignEffectManager.applyPassiveBirthsignEffects(player, birthsignName);
			// Don't recharge active charges when player logs in
			//birthsignEffectManager.rechargebirthsignCharges(player);
		} else {
			if (Menhir.logger != null) {
				Menhir.logger.info("No birthsign found for player: {}", player.getName());
			}
		}
	}

	/**
	 * Handles player respawn to ensure birthsign data persists
	 * This event is fired when a player respawns after death
	 */
	@SubscribeEvent
	public static void onPlayerRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
		EntityPlayer player = event.player;
		if (player != null && !player.world.isRemote) {
			// The capability system should automatically handle NBT persistence
			// But we need to reapply effects since the player entity is new
			String birthsignName = getPlayerBirthsign(player);
			if (birthsignName != null && !birthsignName.isEmpty()) {
				// Reapply all birthsign effects to the new player entity
				BirthsignEffectManager.applyPassiveBirthsignEffects(player, birthsignName);
				// Recharge charges after respawn
				BirthsignEffectManager.rechargeBirthsignCharges(player);
				BirthsignEffectManager.rechargeBirthsignPassiveCharges(player);
			}
		}
	}

	/**
	 * Handles player dimension changes to ensure birthsign data persists
	 * This event is fired when a player changes dimensions (e.g., going to Nether)
	 */
	@SubscribeEvent
	public static void onPlayerChangedDimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
		EntityPlayer player = event.player;
		if (player != null && !player.world.isRemote) {
			String playerBirthsign = getPlayerBirthsign(player);
			if (playerBirthsign != null && !playerBirthsign.isEmpty()) {
				if (Menhir.logger != null) {
					Menhir.logger.info("Reapplying birthsign effects after dimension change for: {} with birthsign: {}", player.getName(), playerBirthsign);
				}
				// Reapply effects after dimension change
				BirthsignEffectManager.applyPassiveBirthsignEffects(player, playerBirthsign);
			}
		}
	}

	/**
	 * Handles player death to ensure birthsign data is properly saved
	 * This event is fired when a player dies, before the entity is destroyed
	 */
	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntityLiving() instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.getEntityLiving();
			if (player != null && !player.world.isRemote) {
				String playerBirthsign = getPlayerBirthsign(player);
				if (playerBirthsign != null && !playerBirthsign.isEmpty()) {
					if (Menhir.logger != null) {
						Menhir.logger.info("Player {} died with birthsign: {}. Ensuring data persistence.", player.getName(), playerBirthsign);
					}
					// The capability system should automatically save the data
					// Log that the player died with their birthsign intact
					if (Menhir.logger != null) {
						Menhir.logger.info("birthsign data for {} will be persisted through death.", player.getName());
					}
				}
			}
		}
	}

	/**
	 * CRITICAL: Handles player cloning during respawn to ensure birthsign data is transferred
	 * This event is fired when a player respawns and their entity is cloned
	 * This is the key event that prevents data loss on death!
	 */
	@SubscribeEvent
	public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
		EntityPlayer original = event.getOriginal();
		EntityPlayer clone = event.getEntityPlayer();
		boolean wasDeath = event.isWasDeath();

		if (original != null && clone != null && !clone.world.isRemote) {
			// Get the original player's birthsign data
			IBirthsignData originalData = BirthsignDataProvider.get(original);
			if (originalData != null && originalData.getBirthsign() != null && !originalData.getBirthsign().isEmpty()) {
				String birthsignName = originalData.getBirthsign();

				// Get the clone's birthsign data capability
				IBirthsignData cloneData = BirthsignDataProvider.get(clone);
				if (cloneData != null) {
					// Transfer the birthsign data to the clone
					cloneData.setBirthsign(birthsignName);

					// Also transfer any other birthsign-related data
					cloneData.setInt("birthsign_remaining_charges", originalData.getInt("birthsign_remaining_charges"));
					cloneData.setInt("birthsign_remaining_passive_charges", originalData.getInt("birthsign_remaining_passive_charges"));

					if (Menhir.logger != null) {
						Menhir.logger.info("CRITICAL: Transferred birthsign data from {} to clone: {} (wasDeath: {})",
								original.getName(), birthsignName, wasDeath);
					}

					// Apply birthsign effects to the clone immediately
					BirthsignEffectManager.applyPassiveBirthsignEffects(clone, birthsignName);

					// Recharge charges after respawn
					if (wasDeath) {
						BirthsignEffectManager.rechargeBirthsignCharges(clone);
						BirthsignEffectManager.rechargeBirthsignPassiveCharges(clone);
					}
				}
			} else {
				if (Menhir.logger != null) {
					Menhir.logger.info("No birthsign data found for original player: {}", original.getName());
				}
			}
		}
	}

	@SubscribeEvent
	public static void onWorldTick(TickEvent.WorldTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		if (event.world.isRemote) return;

		// Check if it's midnight (when time reaches 0)
		long time = event.world.getWorldTime();
		long timeOfDay = time % 24000;

		// Recharge at midnight (time = 0)
		if (timeOfDay == 0) {
			if (Menhir.logger != null) {
				Menhir.logger.info("Midnight reached! Recharging all online players' birthsign charges");
			}
			// Recharge all online players' birthsign charges at midnight
			for (EntityPlayer player : event.world.playerEntities) {
				String playerBirthsignName = getPlayerBirthsign(player);
				if (playerBirthsignName != null && !playerBirthsignName.isEmpty()) {
					if (Menhir.logger != null) {
						Menhir.logger.info("Recharging charges for player: {} with birthsign: {}", player.getName(), playerBirthsignName);
					}
					BirthsignEffectManager.rechargeBirthsignCharges(player);
					BirthsignEffectManager.rechargeBirthsignPassiveCharges(player);

					// Reset Arcane Echo binding for The Conjuration birthsign
					if ("the_conjuration".equals(playerBirthsignName)) {
						IBirthsignData birthsignData = BirthsignDataProvider.get(player);
						if (birthsignData != null) {
							birthsignData.setInt("arcane_echo_uses", 0);
							player.sendMessage(new net.minecraft.util.text.TextComponentString(
									net.minecraft.util.text.TextFormatting.AQUA + "Your Arcane Echo binding has been reset for the new day!"
							));
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
		EntityPlayer player = event.getEntityPlayer();
		if (player != null && !player.world.isRemote) {
			// Check if the player actually slept through the night
			// Only recharge if it's now daytime (time >= 0 and < 12000)
			long time = player.world.getWorldTime();
			long timeOfDay = time % 24000;

			// Only recharge if it's daytime (0-12000 ticks) after sleeping
			if (timeOfDay >= 0 && timeOfDay < 12000) {
				String playerBirthsign = getPlayerBirthsign(player);
				if (playerBirthsign != null && !playerBirthsign.isEmpty()) {
					// Player slept through the night, recharge their birthsign charges
					BirthsignEffectManager.rechargeBirthsignCharges(player);
					BirthsignEffectManager.rechargeBirthsignPassiveCharges(player);
				}
			}
		}
	}

	/**
	 * Maintains persistent birthsign passive effects by reapplying them every 10 seconds.
	 * Handles potion effects and attribute modifiers to ensure they remain active.
	 *
	 * @param event The PlayerTickEvent containing player and tick information
	 */
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		if (event.player.world.isRemote) return;
		EntityPlayer player = event.player;
		String playerBirthsign = getPlayerBirthsign(player);
		if (playerBirthsign == null || playerBirthsign.isEmpty()) return;

		if (player.ticksExisted % PASSIVE_EFFECT_REAPPLY_INTERVAL == 0) {
			// Reapply passive effects (both potion effects and attribute modifiers)
			Birthsign birthsign = Birthsign.registry.getValue(new net.minecraft.util.ResourceLocation(playerBirthsign));
			if (birthsign != null && birthsign.passive != null) {
				for (Birthsign.BirthsignEffect effect : birthsign.passive) {
					Birthsign.EffectDetail eff = effect.effect;
					if (eff.type == Birthsign.EffectType.POTION_EFFECT) {
						// Reapply the potion effect using the same logic as PotionEffectAbility
						String potionName = eff.getParameter("potioneffect", "");
						int amplifier = eff.getParameter("amplifier", 0);
						int duration = eff.getParameter("duration", 200);
						net.minecraft.potion.Potion potion = net.minecraft.potion.Potion.getPotionFromResourceLocation(potionName);
						if (potion != null) {
							net.minecraft.potion.PotionEffect potionEffect = new net.minecraft.potion.PotionEffect(potion, duration, amplifier, true, true);
							player.addPotionEffect(potionEffect);
						}
					} else if (eff.type == Birthsign.EffectType.ATTRIBUTE_MODIFIER) {
						// Reapply attribute modifiers to ensure they're always present
						String attribute = eff.getParameter("attribute", "");
						double amount = eff.getParameter("amount", 0.0);
						int operation = getOperation(String.valueOf(eff.getParameter("operation", 0)));
						String attributeClass = eff.getParameter("attribute_class", "");
						String attributeField = eff.getParameter("attribute_field", "");

						BirthsignAttributeModifier mod = new BirthsignAttributeModifier(attribute, attributeClass, attributeField, amount, operation, birthsign.name);
						mod.apply(player, birthsign.name);
					} else if (eff.type == Birthsign.EffectType.UNDERGROUND_HASTE) {
						// Apply underground haste effect if conditions are met
						UndergroundHasteAbility.applyHasteIfConditionsMet(player);
					} else if (eff.type == Birthsign.EffectType.BLOCK_PLACEMENT) {
						// Block placement effects are handled by the active ability system
						// No passive effect to apply here
					}
				}
			}
		}
	}

	/**
	 * Gets the player's assigned birthsign from the capability system.
	 */
	private static String getPlayerBirthsign(EntityPlayer player) {
		IBirthsignData data = BirthsignDataProvider.get(player);
		return data != null ? data.getBirthsign() : null;
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

	/**
	 * Assigns a random birthsign to the player.
	 *
	 * @param player The player to assign the birthsign to.
	 * @return The name of the assigned birthsign, or null if no birthsign was assigned.
	 */
	private static String assignRandomBirthsign(EntityPlayer player) {
		if (Birthsign.registry == null) {
			if (Menhir.logger != null) {
				Menhir.logger.warn("birthsign registry is null, cannot assign birthsign to player: {}", player.getName());
			}
			return null;
		}

		java.util.Set<ResourceLocation> birthsignKeys = Birthsign.registry.getKeys();
		if (birthsignKeys.isEmpty()) {
			if (Menhir.logger != null) {
				Menhir.logger.warn("No birthsigns registered to assign to player: {}", player.getName());
			}
			return null;
		}


		// Convert ResourceLocation keys to birthsign names and select a random one
		java.util.List<String> birthsignNames = new java.util.ArrayList<>();
		for (ResourceLocation key : birthsignKeys) {
			birthsignNames.add(key.toString());
		}

		Random random = new Random();
		String randomBirthsign = birthsignNames.get(random.nextInt(birthsignNames.size()));

		IBirthsignData data = BirthsignDataProvider.get(player);
		if (data != null) {
			data.setBirthsign(randomBirthsign);
			data.setInt("birthsign_remaining_charges", Birthsign.getBirthsignFromString(randomBirthsign).active_daily_uses); // Reset charges
			data.setInt("birthsign_remaining_passive_charges", Birthsign.getBirthsignFromString(randomBirthsign).passive_daily_uses); // Reset passive charges
			return randomBirthsign;
		}
		return null;
	}

} 