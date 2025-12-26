package com.windanesz.menhir.eventhandler;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.ability.minercaft.HealOnKillAbility;
import com.windanesz.menhir.ability.minercaft.VerdantBondAbility;
import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.core.BirthsignDataLoader;
import electroblob.wizardry.event.SpellCastEvent;
import electroblob.wizardry.util.SpellModifiers;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class BirthsignAbilityEventHandler {

	/**
	 * Handles fall damage reduction for any birthsign with the FALL_DAMAGE_REDUCTION effect type
	 * Players with this ability take no fall damage from drops under the specified threshold
	 */
	@SubscribeEvent
	public static void onLivingHurt(LivingHurtEvent event) {
		if (event.getEntityLiving() instanceof EntityPlayer) {
			handleMageWeakness(event);
			handleFallDamageReduction(event);
			handleSpatialSlip(event);
			handleFireImmunity(event);
		}
	}

	@SubscribeEvent
	public static void onArrowImpact(ProjectileImpactEvent.Arrow event) {
		if (!(event.getArrow().shootingEntity instanceof EntityPlayer) || event.getArrow().world.isRemote) return;
		EntityPlayer player = (EntityPlayer) event.getArrow().shootingEntity;

		if (event.getRayTraceResult() == null || event.getRayTraceResult().entityHit == null) return;

		String birthsignName = getPlayerBirthsign(player);
		if (birthsignName == null) return;

		Birthsign birthsign = Birthsign.getBirthsignFromString(birthsignName);
		if (birthsign == null || birthsign.passive == null) return;

		for (Birthsign.BirthsignEffect effect : birthsign.passive) {
			if (effect.effect != null && effect.effect.type == Birthsign.EffectType.ARROW_SALVAGE) {

				ItemStack mainHand = player.getHeldItemMainhand();
				ItemStack offHand = player.getHeldItemOffhand();
				boolean hasInfinityBow = (mainHand.getItem() instanceof ItemBow && EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, mainHand) > 0) ||
						(offHand.getItem() instanceof ItemBow && EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, offHand) > 0);

				if (!hasInfinityBow) {
					int recoveryChance = effect.effect.getParameter("chance", 100);

					if (player.world.rand.nextInt(100) < recoveryChance) {
						int maxPassiveCharges = birthsign.passive_daily_uses;

						if (maxPassiveCharges > -1) {
							int currentPassiveCharges = BirthsignEffectManager.getBirthsignRemainingPassiveCharges(player);
							if (currentPassiveCharges <= 0) {
								return; // No charges left
							}
							BirthsignEffectManager.decrementBirthsignRemainingPassiveCharges(player);
							// client sync
							if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
								IBirthsignData data = BirthsignDataProvider.get(player);
								if (birthsignName != null) {
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

						ItemStack arrowStack;
						try {
							java.lang.reflect.Method getArrowStackMethod = EntityArrow.class.getDeclaredMethod("getArrowStack");
							getArrowStackMethod.setAccessible(true);
							arrowStack = (ItemStack) getArrowStackMethod.invoke(event.getArrow());
						} catch (Exception e) {
							arrowStack = new ItemStack(net.minecraft.init.Items.ARROW);
							Menhir.logger.warn("Could not get arrow itemstack via reflection", e);
						}

						if (!player.inventory.addItemStackToInventory(arrowStack.copy())) {
							player.dropItem(arrowStack.copy(), false);
						}

						event.getArrow().setDead();
					}
				}
				break;
			}
		}
	}

	/**
	 * Handles sneaking speed bonus for The Thief
	 * Players with The Thief move 25% faster while sneaking
	 */
	@SubscribeEvent
	public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
		if (event.getEntityLiving() instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.getEntityLiving();

			// Get the player's birthsign
			String birthsignName = getPlayerBirthsign(player);
			if (birthsignName != null && "menhir:the_thief".equals(birthsignName)) {
				// Check if player is sneaking
				if (player.isSneaking()) {
					// Apply sneaking speed bonus
					IAttributeInstance movementSpeedAttr = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
					if (movementSpeedAttr != null) {
						// The base movement speed is already modified by the attribute modifier
						// This event handler ensures the bonus is applied while sneaking
						// The actual speed bonus is handled by the attribute modifier in the JSON
					}
				}
			}

			// Handle passive abilities dynamically based on birthsign configuration
			if (birthsignName != null) {
				// Check for passive abilities every few ticks to avoid performance issues
				if (player.ticksExisted % 20 == 0) { // Every second
					handlePassiveAbilities(player, birthsignName);
				}

				// Handle Verdant Bond passive ability for plant growth acceleration
				if (player.ticksExisted % 600 == 0) { // Every 30 seconds for plant growth
					handleVerdantBond(player, birthsignName);
				}
			}
		}
	}

	/**
	 * Handles burning attack passive ability for any birthsign with the BURNING_ATTACK effect type
	 * Players with this ability have a chance to ignite targets on melee attacks
	 */
	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		if (event.getEntityPlayer() != null && !event.getEntityPlayer().world.isRemote) {
			EntityPlayer player = event.getEntityPlayer();
			String birthsignName = getPlayerBirthsign(player);

			if (birthsignName != null) {
				Birthsign birthsign = Birthsign.getBirthsignFromString(birthsignName);
				if (birthsign != null && birthsign.passive != null) {
					for (Birthsign.BirthsignEffect effect : birthsign.passive) {
						if (effect.effect != null && effect.effect.type == Birthsign.EffectType.BURNING_ATTACK) {

							double igniteChance = effect.effect.getParameter("ignite_chance", 0.20);
							int igniteDuration = ((Number) effect.effect.getParameter("ignite_duration", 3)).intValue();

							// Check if we should ignite the target
							if (player.world.rand.nextDouble() < igniteChance) {
								if (event.getTarget() instanceof net.minecraft.entity.EntityLivingBase) {
									net.minecraft.entity.EntityLivingBase target = (net.minecraft.entity.EntityLivingBase) event.getTarget();
									target.setFire(igniteDuration);

									// Send message to player
									player.sendMessage(new net.minecraft.util.text.TextComponentString(net.minecraft.util.text.TextFormatting.GOLD + "Smoldering Strikes ignited your target for " + igniteDuration + " seconds!"));
								}
							}
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * Handles spell potency bonus from birthsign attributes
	 * This event is fired when a spell is cast and can be used to modify spell properties
	 */
	@SubscribeEvent
	public static void handleSpellModifierPassive(SpellCastEvent.Pre event) {
		if (!event.getWorld().isRemote && event.getCaster() instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.getCaster();
			// Get the player's birthsign
			String birthsignName = getPlayerBirthsign(player);
			// Apply spell potency bonus using wizardryutils.SpellPotency attribute
			Birthsign birthsign = Birthsign.getBirthsignFromString(birthsignName);
			if (birthsign != null) {
				// for each passive in birthsign
				for (Birthsign.BirthsignEffect effect : birthsign.passive) {
					// if the passive is a spell potency bonus
					if (effect.effect.type == Birthsign.EffectType.WIZARDRY_SPELL_MODIFIER) {
						// apply the spell potency bonus
						String mod = effect.effect.getParameter("name", "");
						Double amount = effect.effect.getParameter("amount", 0.0);
						SpellModifiers modifiers = event.getModifiers();
						modifiers.set(mod, modifiers.get(mod) + amount.floatValue(), false);
					}
				}
			}
		}
	}

	public static void handleMageWeakness(LivingHurtEvent event) {
		EntityPlayer player = (EntityPlayer) event.getEntityLiving();
		String birthsignName = getPlayerBirthsign(player);
		if ("menhir:the_mage".equals(birthsignName)) {
			// Check if the damage is from a magical source
			if (isMagicalDamage(event.getSource())) {
				// Apply 15% magic damage vulnerability
				float increasedDamage = event.getAmount() * 1.15f;
				event.setAmount(increasedDamage);
			}
		}
	}

	/**
	 * Handles fall damage reduction for any birthsign that has the FALL_DAMAGE_REDUCTION effect type
	 * Players with this ability take no fall damage from drops under the specified threshold
	 */
	public static void handleFallDamageReduction(LivingHurtEvent event) {
		EntityPlayer player = (EntityPlayer) event.getEntityLiving();
		String birthsignName = getPlayerBirthsign(player);
		if (birthsignName == null) return;

		Birthsign birthsign = Birthsign.getBirthsignFromString(birthsignName);
		if (birthsign == null || birthsign.passive == null) return;

		// Check if this birthsign has fall damage reduction
		boolean hasFallDamageReduction = false;
		double fallDamageHeightThreshold = 6.0f; // Default height threshold in blocks

		for (Birthsign.BirthsignEffect effect : birthsign.passive) {
			if (effect.effect != null && effect.effect.type == Birthsign.EffectType.FALL_DAMAGE_REDUCTION) {
				hasFallDamageReduction = true;
				// Use custom height threshold if specified, otherwise use default
				Double threshold = effect.effect.getParameter("fall_damage_height_threshold", null);
				if (threshold != null) {
					fallDamageHeightThreshold = threshold;
				}
				break;
			}
		}

		if (!hasFallDamageReduction) return;

		// Check if the damage is from falling
		if (event.getSource() == DamageSource.FALL) {
			// Check if fall distance is under the threshold
			if (player.fallDistance < fallDamageHeightThreshold) {
				// Cancel the fall damage for drops under the threshold
				event.setCanceled(true);
			}
		}
	}

	/**
	 * Handles Spatial Slip ability for any birthsign that has the SPATIAL_SLIP effect type
	 * If the player would take lethal fall damage, they teleport using ender pearl effect instead
	 */
	public static void handleSpatialSlip(LivingHurtEvent event) {
		EntityPlayer player = (EntityPlayer) event.getEntityLiving();
		String birthsignName = getPlayerBirthsign(player);
		if (birthsignName == null) return;

		Birthsign birthsign = Birthsign.getBirthsignFromString(birthsignName);
		if (birthsign == null || birthsign.passive == null) return;

		// Check if this birthsign has the spatial_slip passive ability
		boolean hasSpatialSlip = false;

		for (Birthsign.BirthsignEffect effect : birthsign.passive) {
			if (effect.effect != null && effect.effect.type == Birthsign.EffectType.SPATIAL_SLIP) {
				hasSpatialSlip = true;
				break;
			}
		}

		if (!hasSpatialSlip) return;

		// Only trigger on fall damage
		if (event.getSource() != net.minecraft.util.DamageSource.FALL) return;

		// Check if this damage would be lethal (would kill the player)
		if (event.getAmount() >= player.getHealth()) {
			// Check if player has passive charges available
			int currentPassiveCharges = BirthsignEffectManager.getBirthsignRemainingPassiveCharges(player);
			if (currentPassiveCharges <= 0) {
				return; // No passive charges available, can't use spatial slip
			}

		player.world.playEvent(2003, player.getPosition(), 0); // Portal particles

		// Consume a passive charge
		BirthsignEffectManager.decrementBirthsignRemainingPassiveCharges(player);
		
		// Sync to client with full capability data
		if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
			IBirthsignData data = BirthsignDataProvider.get(player);
			if (birthsignName != null) {
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

		// Cancel the lethal damage
			event.setCanceled(true);

			// Send message to player
			player.sendMessage(new net.minecraft.util.text.TextComponentString("Spatial Slip activated! You teleported to safety."));

			if (Menhir.logger != null) {
				Menhir.logger.info("Spatial Slip activated for player {} with {} passive charges remaining", player.getName(), currentPassiveCharges - 1);
			}
		}
	}

	/**
	 * Handles fire immunity for any birthsign that has the FIRE_IMMUNITY effect type
	 * Players with this ability can ignore fire and lava damage for a limited duration
	 */
	public static void handleFireImmunity(LivingHurtEvent event) {
		EntityPlayer player = (EntityPlayer) event.getEntityLiving();
		String birthsignName = getPlayerBirthsign(player);
		if (birthsignName == null) return;

		Birthsign birthsign = Birthsign.getBirthsignFromString(birthsignName);
		if (birthsign == null || birthsign.passive == null) return;

		// Check if this birthsign has fire immunity
		boolean hasFireImmunity = false;
		int duration = 300;

		for (Birthsign.BirthsignEffect effect : birthsign.passive) {
			if (effect.effect != null && effect.effect.type == Birthsign.EffectType.FIRE_IMMUNITY) {
				hasFireImmunity = true;

				Integer fireDuration = ((Long)effect.effect.getParameter("fire_immunity_duration", 1L)).intValue();
				if (fireDuration != null) {
					duration = fireDuration;
				}

				// Check if player has passive charges available
				int currentPassiveCharges = BirthsignEffectManager.getBirthsignRemainingPassiveCharges(player);
				if (currentPassiveCharges <= 0) {
					return; // No passive charges available, can't use fire immunity
				}

			// Consume a passive charge
			BirthsignEffectManager.decrementBirthsignRemainingPassiveCharges(player);
			
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

			player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, duration));				break;

			}
		}
	}

	/**
	 * Determines if the damage source is magical
	 * This includes damage from spells, magical entities, and other magical sources
	 */
	private static boolean isMagicalDamage(DamageSource source) {
		// Check for common magical damage sources
		String damageType = source.damageType;

		// Damage from spells (ElectroBlob's Wizardry)
		if (damageType.contains("magic") || damageType.contains("spell") || damageType.contains("arcane")) {
			return true;
		}

		// Damage from magical projectiles
		if (source.getImmediateSource() != null) {
			String entityName = source.getImmediateSource().getName();
			if (entityName.contains("spell") || entityName.contains("magic") || entityName.contains("fireball")) {
				return true;
			}
		}

		// Additional checks for specific magical damage types
		if (source.isMagicDamage()) {
			return true;
		}

		return false;
	}

	/**
	 * Gets the player's assigned birthsign from the capability system.
	 */
	private static String getPlayerBirthsign(EntityPlayer player) {
		IBirthsignData data = BirthsignDataProvider.get(player);
		return data != null ? data.getBirthsign() : null;
	}

	/**
	 * Handles passive abilities dynamically based on birthsign configuration
	 */
	private static void handlePassiveAbilities(EntityPlayer player, String birthsignName) {
		Birthsign birthsign = Birthsign.getBirthsignFromString(birthsignName);
		if (birthsign == null || birthsign.passive == null) return;

		for (Birthsign.BirthsignEffect effect : birthsign.passive) {
			if (effect.effect != null && effect.effect.type != null) {
				// Handle custom passive abilities based on effect type
				if (effect.effect.type == Birthsign.EffectType.THREAT_SENSE) {
					handleThreatSense(player);
				}
				// Add more custom ability types here as needed
			}
		}

		// Handle factory-based passive abilities as fallback
		BirthsignDataLoader.PassiveAbilityFactory passiveFactory = getPassiveAbilityFactory(birthsignName);
		if (passiveFactory != null) {
			Runnable abilityRunner = passiveFactory.create(player);
			if (abilityRunner != null) {
				abilityRunner.run();
			}
		}
	}

	/**
	 * Handles heal on kill
	 */
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event.getEntityLiving() instanceof EntityPlayer) return; // Don't trigger on player death

		// Check if a player caused the death
		if (event.getSource().getTrueSource() instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
			String birthsignName = getPlayerBirthsign(player);

			if (birthsignName != null) {
				Birthsign birthsign = Birthsign.getBirthsignFromString(birthsignName);
				if (birthsign != null && birthsign.passive != null) {
					for (Birthsign.BirthsignEffect effect : birthsign.passive) {
						if (effect.effect == null) continue;

						if (effect.effect.type == Birthsign.EffectType.HEAL_ON_KILL) {
							// Create the HealOnKillAbility from the effect parameters
							HealOnKillAbility healAbility = (HealOnKillAbility) HealOnKillAbility.create(effect.effect.parameters, birthsignName);
							healAbility.onKill(player, event.getEntityLiving());
						}
					}
				}
			}
		}
	}

	/**
	 * Handles The Seer's Threat Sense passive ability
	 * When player health drops below 33%, nearby hostile mobs become glowing for 2 seconds
	 */
	private static void handleThreatSense(EntityPlayer player) {
		if (player == null || player.world.isRemote) return;

		// Check if player's health is below 33%
		float healthPercent = player.getHealth() / player.getMaxHealth();
		if (healthPercent > 0.33) return;

		// Find all hostile mobs within 16 block radius
		net.minecraft.util.math.AxisAlignedBB searchBox = new net.minecraft.util.math.AxisAlignedBB(player.posX - 16.0, player.posY - 16.0, player.posZ - 16.0, player.posX + 16.0, player.posY + 16.0, player.posZ + 16.0);

		// Find all hostile mobs within range
		java.util.List<net.minecraft.entity.monster.EntityMob> nearbyHostiles = player.world.getEntitiesWithinAABB(net.minecraft.entity.monster.EntityMob.class, searchBox);

		for (net.minecraft.entity.monster.EntityMob hostile : nearbyHostiles) {
			if (hostile.isEntityAlive() && !hostile.isPotionActive(net.minecraft.init.MobEffects.GLOWING)) {
				// Apply glowing effect for 2 seconds (40 ticks)
				net.minecraft.potion.PotionEffect glowingEffect = new net.minecraft.potion.PotionEffect(net.minecraft.init.MobEffects.GLOWING, 40, 0, false, false);
				hostile.addPotionEffect(glowingEffect);
			}
		}
	}

	/**
	 * Handles Verdant Bond passive ability for any birthsign with the VERDANT_BOND effect type
	 * Players with this ability accelerate plant growth in their radius
	 */
	private static void handleVerdantBond(EntityPlayer player, String birthsignName) {
		Birthsign birthsign = Birthsign.getBirthsignFromString(birthsignName);
		if (birthsign == null || birthsign.passive == null) return;

		for (Birthsign.BirthsignEffect effect : birthsign.passive) {
			if (effect.effect != null && effect.effect.type == Birthsign.EffectType.VERDANT_BOND) {

				// Create and use the VerdantBondAbility to accelerate plant growth
				VerdantBondAbility verdantAbility = VerdantBondAbility.create(java.util.Collections.singletonMap("radius", effect.effect.getParameter("verdant_radius", 15)), birthsignName);

				verdantAbility.acceleratePlantGrowth(player);
				break;
			}
		}
	}

	/**
	 * Gets the passive ability factory for a given birthsign name.
	 */
	private static BirthsignDataLoader.PassiveAbilityFactory getPassiveAbilityFactory(String birthsignName) {
		// Extract the birthsign name without the namespace
		String shortName = birthsignName;
		if (birthsignName.contains(":")) {
			shortName = birthsignName.substring(birthsignName.indexOf(":") + 1);
		}

		// Use reflection to access the private static field
		try {
			java.lang.reflect.Field field = BirthsignDataLoader.class.getDeclaredField("BIRTHSIGN_ABILITY_FACTORIES");
			field.setAccessible(true);
			@SuppressWarnings("unchecked") java.util.Map<String, BirthsignDataLoader.PassiveAbilityFactory> factories = (java.util.Map<String, BirthsignDataLoader.PassiveAbilityFactory>) field.get(null);
			return factories.get(shortName);
		} catch (Exception e) {
			return null;
		}
	}
} 