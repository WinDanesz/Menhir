package com.windanesz.menhir.eventhandler;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.Settings;
import com.windanesz.menhir.ability.minercaft.UndergroundHasteAbility;
import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.api.BirthsignAttributeModifier;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.network.NetworkHandler;
import com.windanesz.menhir.network.PacketSyncBirthsignData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class BirthsignCoreEventHandler {

	private static final int PASSIVE_EFFECT_REAPPLY_INTERVAL = 200; // 10 seconds (200 ticks)
	private static final Map<net.minecraft.world.World, Boolean> worldMidnightRechargeStatus = new WeakHashMap<>();

	/**
	 * Helper method to get all active traits (birthsigns, races, etc.) for a player
	 */
	private static List<Birthsign> getAllActiveTraits(EntityPlayer player) {
		List<Birthsign> traits = new ArrayList<>();
		IBirthsignData data = BirthsignDataProvider.get(player);
		if (data != null) {
			Map<String, String> selectedTraits = data.getAllTraits();
			for (String traitName : selectedTraits.values()) {
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

	@SubscribeEvent
	public static void onPlayerLoggedIn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
		EntityPlayer player = event.player;
		
		// Sync all birthsign data to client
		if (player instanceof EntityPlayerMP) {
			IBirthsignData data = BirthsignDataProvider.get(player);
			net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
			if (data != null) {
				data.writeToNBT(nbt);
			}
			// Use primary birthsign as the key for packet (compatibility)
			String primary = data != null ? data.getBirthsign() : "";
			NetworkHandler.INSTANCE.sendTo(new PacketSyncBirthsignData(primary, nbt), (EntityPlayerMP) player);
		}

		String birthsignName = getPlayerBirthsign(player);

		// Check if player doesn't have a birthsign (primary trait)
		if (birthsignName == null || birthsignName.isEmpty()) {
			// Check if random assignment is enabled (selection mode doesn't auto-assign, player chooses manually)
			if (!Settings.generalSettings.allow_birthsign_selection_on_first_spawn && Settings.generalSettings.random_birthsign_assignment) {
				// Assign a random birthsign to the player
				String randomBirthsign = assignRandomBirthsign(player);
				if (randomBirthsign != null) {
					birthsignName = randomBirthsign;

					// Sync the newly assigned birthsign to client with full capability data
					if (player instanceof EntityPlayerMP) {
						IBirthsignData data = BirthsignDataProvider.get(player);
						net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
						if (data != null) {
							data.writeToNBT(nbt);
						}
						NetworkHandler.INSTANCE.sendTo(new PacketSyncBirthsignData(birthsignName, nbt), (EntityPlayerMP) player);
					}

					// Show message if enabled
					if (Settings.generalSettings.show_random_birthsign_message) {
						// Extract the birthsign name without modid prefix for translation
						String birthsignNameForTranslation = randomBirthsign;
						if (randomBirthsign.contains(":")) {
							birthsignNameForTranslation = randomBirthsign.split(":")[1];
						}

						// Create the message component with client-side translation
						TextComponentString message = new TextComponentString("You have been assigned the birthsign: ");
						message.getStyle().setColor(TextFormatting.GOLD);
						message.appendSibling(new TextComponentTranslation("birthsign." + birthsignNameForTranslation + ".name"));

						player.sendMessage(message);
					}

					if (Menhir.logger != null) {
						Menhir.logger.info("Assigned random birthsign '{}' to player: {}", randomBirthsign, player.getName());
					}
				}
			}
		}

		// Apply passive effects for ALL traits
		List<Birthsign> traits = getAllActiveTraits(player);
		if (!traits.isEmpty()) {
			for (Birthsign trait : traits) {
				BirthsignEffectManager.applyPassiveBirthsignEffects(player, trait.getRegistryName().toString());
			}
			// Don't recharge active charges when player logs in
			//birthsignEffectManager.rechargebirthsignCharges(player);
		} else {
			if (Menhir.logger != null) {
				Menhir.logger.info("No traits found for player: {}", player.getName());
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
			
			// Sync data first
			if (player instanceof EntityPlayerMP) {
				IBirthsignData data = BirthsignDataProvider.get(player);
				if (data != null) {
					net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
					data.writeToNBT(nbt);
					String primary = data.getBirthsign();
					NetworkHandler.INSTANCE.sendTo(new PacketSyncBirthsignData(primary, nbt), (EntityPlayerMP) player);
				}
			}
			
			// Reapply all birthsign effects to the new player entity
			List<Birthsign> traits = getAllActiveTraits(player);
			for (Birthsign trait : traits) {
				BirthsignEffectManager.applyPassiveBirthsignEffects(player, trait.getRegistryName().toString());
			}
			
			// Recharge charges after respawn if enabled in config
			if (Settings.generalSettings.recharge_charges_on_respawn) {
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
			List<Birthsign> traits = getAllActiveTraits(player);
			if (!traits.isEmpty()) {
				if (Menhir.logger != null) {
					Menhir.logger.info("Reapplying trait effects after dimension change for: {}", player.getName());
				}
				
				// Sync birthsign data to client with full capability data
				if (player instanceof EntityPlayerMP) {
					IBirthsignData data = BirthsignDataProvider.get(player);
					if (data != null) {
						net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
						data.writeToNBT(nbt);
						String primary = data.getBirthsign();
						NetworkHandler.INSTANCE.sendTo(new PacketSyncBirthsignData(primary, nbt), (EntityPlayerMP) player);
					}
				}
				
				// Reapply effects after dimension change
				for (Birthsign trait : traits) {
					BirthsignEffectManager.applyPassiveBirthsignEffects(player, trait.getRegistryName().toString());
				}
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
			IBirthsignData cloneData = BirthsignDataProvider.get(clone);
			
			if (originalData != null && cloneData != null) {
				Map<String, String> originalTraits = originalData.getAllTraits();
				
				if (!originalTraits.isEmpty()) {
					// Transfer all traits
					for (Map.Entry<String, String> entry : originalTraits.entrySet()) {
						cloneData.setTrait(entry.getKey(), entry.getValue());
					}

					// Also transfer any other birthsign-related data
					cloneData.setInt("birthsign_remaining_charges", originalData.getInt("birthsign_remaining_charges"));
					cloneData.setInt("birthsign_remaining_passive_charges", originalData.getInt("birthsign_remaining_passive_charges"));

					// Sync to client with full capability data
					if (clone instanceof EntityPlayerMP) {
						net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
						cloneData.writeToNBT(nbt);
						String primary = cloneData.getBirthsign();
						NetworkHandler.INSTANCE.sendTo(new PacketSyncBirthsignData(primary, nbt), (EntityPlayerMP) clone);
					}

					if (Menhir.logger != null) {
						Menhir.logger.info("CRITICAL: Transferred trait data from {} to clone (wasDeath: {})",
								original.getName(), wasDeath);
					}

					// Apply birthsign effects to the clone immediately
					List<Birthsign> traits = getAllActiveTraits(clone);
					for (Birthsign trait : traits) {
						BirthsignEffectManager.applyPassiveBirthsignEffects(clone, trait.getRegistryName().toString());
					}

					// Recharge charges after respawn if enabled in config
					if (wasDeath && Settings.generalSettings.recharge_charges_on_respawn) {
						BirthsignEffectManager.rechargeBirthsignCharges(clone);
						BirthsignEffectManager.rechargeBirthsignPassiveCharges(clone);
					}
				} else {
					if (Menhir.logger != null) {
						Menhir.logger.info("No birthsign data found for original player: {}", original.getName());
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onWorldTick(TickEvent.WorldTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		if (event.world.isRemote) return;

		net.minecraft.world.World world = event.world;
		long time = world.getWorldTime();
		long timeOfDay = time % 24000;

		// Get the recharge status for this specific world
		boolean hasRechargedForMidnight = worldMidnightRechargeStatus.getOrDefault(world, false);

		// Recharge at midnight (time = 0)
		if (timeOfDay == 0 && !hasRechargedForMidnight) {
			if (Menhir.logger != null) {
				Menhir.logger.info("Midnight reached in dimension {}! Recharging all online players' birthsign charges", world.provider.getDimension());
			}
			// Recharge all online players' birthsign charges at midnight
			for (EntityPlayer player : world.playerEntities) {
				List<Birthsign> traits = getAllActiveTraits(player);
				if (!traits.isEmpty()) {
					if (Menhir.logger != null) {
						Menhir.logger.info("Recharging charges for player: {}", player.getName());
					}
					BirthsignEffectManager.rechargeBirthsignCharges(player);
					BirthsignEffectManager.rechargeBirthsignPassiveCharges(player);

					// Reset Arcane Echo binding for The Conjuration birthsign
					// TODO: Make this generic if other traits need reset
					if (traits.stream().anyMatch(t -> "the_conjuration".equals(t.getRegistryName().getPath()))) {
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
			worldMidnightRechargeStatus.put(world, true); // Update status for this world
		} else if (timeOfDay > 0 && hasRechargedForMidnight) {
			// Reset the flag once midnight has passed
			worldMidnightRechargeStatus.put(world, false); // Update status for this world
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
				List<Birthsign> traits = getAllActiveTraits(player);
				if (!traits.isEmpty()) {
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
		
		List<Birthsign> traits = getAllActiveTraits(player);
		if (traits.isEmpty()) return;

		if (player.ticksExisted % PASSIVE_EFFECT_REAPPLY_INTERVAL == 0) {
			// Reapply passive effects (both potion effects and attribute modifiers) for ALL traits
			for (Birthsign trait : traits) {
				if (trait.passive != null) {
					for (Birthsign.BirthsignEffect effect : trait.passive) {
						Birthsign.EffectDetail eff = effect.effect;
						if (eff.type == Birthsign.EffectType.POTION_EFFECT) {
							// Reapply the potion effect using the same logic as PotionEffectAbility
							String potionName = eff.getParameter("potioneffect", "");
							// Handle both Integer and Long types from JSON
							Number amplifierNum = eff.getParameter("amplifier", 0);
							Number durationNum = eff.getParameter("duration", 200);
							int amplifier = amplifierNum.intValue();
							int duration = durationNum.intValue();
							net.minecraft.potion.Potion potion = net.minecraft.potion.Potion.getPotionFromResourceLocation(potionName);
							if (potion != null) {
								net.minecraft.potion.PotionEffect potionEffect = new net.minecraft.potion.PotionEffect(potion, duration, amplifier, true, true);
								player.addPotionEffect(potionEffect);
							}
						} else if (eff.type == Birthsign.EffectType.ATTRIBUTE_MODIFIER) {
							// Reapply attribute modifiers to ensure they're always present
							String attribute = eff.getParameter("attribute", "");
							double amount = eff.getParameter("amount", 0.0);
							Object operationObj = eff.getParameter("operation", 0);
							int operation = getOperation(String.valueOf(operationObj));

							BirthsignAttributeModifier mod = new BirthsignAttributeModifier(attribute, amount, operation, trait.name);
							mod.apply(player, trait.name);
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

		// Filter for only primary birthsigns (category == null or "birthsign")
		java.util.List<String> birthsignNames = new java.util.ArrayList<>();
		for (Birthsign b : Birthsign.registry) {
			if (b.category == null || "birthsign".equals(b.category)) {
				birthsignNames.add(b.getRegistryName().toString());
			}
		}

		if (birthsignNames.isEmpty()) {
			if (Menhir.logger != null) {
				Menhir.logger.warn("No birthsigns registered to assign to player: {}", player.getName());
			}
			return null;
		}

		Random random = new Random();
		String randomBirthsign = birthsignNames.get(random.nextInt(birthsignNames.size()));

		IBirthsignData data = BirthsignDataProvider.get(player);
		if (data != null) {
			data.setBirthsign(randomBirthsign);
			// Note: This reset logic is still a bit weird with global pools, 
			// but for initial random assignment it's okay to reset to this birthsign's max.
			// Ideally we'd calculate total max from all traits, but on first login usually only one trait exists.
			Birthsign bs = Birthsign.getBirthsignFromString(randomBirthsign);
			if (bs != null) {
				data.setInt("birthsign_remaining_charges", bs.active_daily_uses);
				data.setInt("birthsign_remaining_passive_charges", bs.passive_daily_uses);
			}
			return randomBirthsign;
		}
		return null;
	}

} 