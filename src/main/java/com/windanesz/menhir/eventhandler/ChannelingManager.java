package com.windanesz.menhir.eventhandler;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChannelingManager {
	private static final Map<UUID, ChannelingData> activeChanneling = new HashMap<>();
	private static final Map<UUID, Integer> channelingCooldowns = new HashMap<>();
	private static final Map<UUID, Boolean> lastExecutionResults = new HashMap<>();
	private static final int COOLDOWN_TICKS = 40; // 2 seconds cooldown after channeling completes

	public static void setLastExecutionResult(EntityPlayer player, boolean success) {
		lastExecutionResults.put(player.getUniqueID(), success);
	}
	
	private static boolean getAndClearLastExecutionResult(EntityPlayer player) {
		Boolean result = lastExecutionResults.remove(player.getUniqueID());
		return result != null && result;
	}

	public static void startChanneling(EntityPlayer player, IBirthsignActiveAbility ability, int channelingTicks) {
		UUID playerId = player.getUniqueID();		// Check if player is on cooldown
		if (channelingCooldowns.containsKey(playerId)) {
			int cooldownLeft = channelingCooldowns.get(playerId);
			if (cooldownLeft > 0) {
				player.sendMessage(new TextComponentString(
						TextFormatting.YELLOW + "Channeling on cooldown... " +
								(cooldownLeft / 20) + " seconds remaining"
				));
				return;
			} else {
				// Cooldown expired, remove it
				channelingCooldowns.remove(playerId);
			}
		}

		if (channelingTicks <= 0) {
			// No channeling needed, activate immediately
			ability.activate(player, null);
			return;
		}

		activeChanneling.put(playerId, new ChannelingData(ability, channelingTicks, player));

		// Send start message
		player.sendMessage(new TextComponentString(
				TextFormatting.GOLD + "Channeling ... Hold " +
						TextFormatting.YELLOW + "K" +
						TextFormatting.GOLD + " to continue."
		));
	}

	public static void stopChanneling(EntityPlayer player) {
		UUID playerId = player.getUniqueID();
		ChannelingData data = activeChanneling.remove(playerId);
		if (data != null) {
			player.sendMessage(new TextComponentString(
					TextFormatting.RED + "Channeling interrupted!"
			));
		}
	}

	public static boolean isPlayerChanneling(EntityPlayer player) {
		return activeChanneling.containsKey(player.getUniqueID());
	}

	public static ChannelingData getPlayerChannelingData(EntityPlayer player) {
		return activeChanneling.get(player.getUniqueID());
	}

	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		EntityPlayer player = event.player;
		
		// Only process channeling on the server side to avoid double-ticking
		if (player.world.isRemote) return;
		
		UUID playerId = player.getUniqueID();

		// Handle cooldowns
		if (channelingCooldowns.containsKey(playerId)) {
			int cooldownLeft = channelingCooldowns.get(playerId);
			if (cooldownLeft > 0) {
				channelingCooldowns.put(playerId, cooldownLeft - 1);
			}
		}

		ChannelingData data = activeChanneling.get(playerId);

		if (data != null) {
			data.currentTicks++;

			// Show progress every second (20 ticks)
			if (data.currentTicks % 20 == 0) {
				int secondsLeft = (data.totalTicks - data.currentTicks) / 20;
				if (secondsLeft > 0) {
					player.sendMessage(new TextComponentString(
							TextFormatting.AQUA + "Channeling... " + secondsLeft + " seconds remaining"
					));
				}
			}

			// Check if channeling is complete
			if (data.isComplete()) {
				activeChanneling.remove(playerId);

				// Add cooldown to prevent immediate re-channeling
				channelingCooldowns.put(playerId, COOLDOWN_TICKS);

				// Execute the ability's channeling completion method
				data.ability.onChannelingComplete(player);
				
				// Check if the ability execution was successful
				boolean success = getAndClearLastExecutionResult(player);
				
				if (success) {
					// Consume a charge only if the ability succeeded
					BirthsignEffectManager.decrementBirthsignRemainingCharges(player);
					
					// Sync charges to client
					if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
						com.windanesz.menhir.api.IBirthsignData birthsignData = com.windanesz.menhir.capability.BirthsignDataProvider.get(player);
						net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
						if (birthsignData != null) {
							birthsignData.writeToNBT(nbt);
						}
						String birthsignName = birthsignData != null ? birthsignData.getBirthsign() : "";
						com.windanesz.menhir.network.NetworkHandler.INSTANCE.sendTo(
							new com.windanesz.menhir.network.PacketSyncBirthsignData(birthsignName, nbt), 
							(net.minecraft.entity.player.EntityPlayerMP) player
						);
					}
				}
				
				// player.sendMessage(new TextComponentString(
				// 		TextFormatting.GREEN + "Channeling complete!"
				// ));
			}
		}
	}

	@SubscribeEvent
	public void onKeyInput(InputEvent.KeyInputEvent event) {
		// Check if the player released the K key (or whatever key is bound)
		// This would need to be integrated with the existing key binding system
	}

	public static class ChannelingData {
		public final IBirthsignActiveAbility ability;
		public final int totalTicks;
		public final EntityPlayer player;
		public int currentTicks;

		public ChannelingData(IBirthsignActiveAbility ability, int totalTicks, EntityPlayer player) {
			this.ability = ability;
			this.totalTicks = totalTicks;
			this.currentTicks = 0;
			this.player = player;
		}

		public boolean isComplete() {
			return currentTicks >= totalTicks;
		}

		public float getProgress() {
			return (float) currentTicks / totalTicks;
		}
	}
} 