package com.windanesz.menhir.altar.handler;

import com.windanesz.menhir.api.altar.AltarDefinition;
import com.windanesz.menhir.api.altar.AltarEffect;
import com.windanesz.menhir.api.altar.IAltarEffectHandler;
import com.windanesz.menhir.tileentity.TileEntityAltar;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * Handler for prayer altars.
 * Displays the prayer text to the player and applies success effects.
 * Tracks prayer cooldowns per player to prevent spam.
 * 
 * Future enhancement: Could require player to type the prayer text in chat.
 */
public class PrayerHandler implements IAltarEffectHandler {
	
	private static final long COOLDOWN_TICKS = 24000; // 20 minutes (1 Minecraft day)
	
	@Override
	public String getEffectType() {
		return "prayer";
	}
	
	@Override
	public boolean canHandle(AltarDefinition definition) {
		for (AltarEffect effect : definition.getEffects()) {
			if (effect instanceof AltarEffect.PrayerEffect) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean handleInteraction(World world, BlockPos pos, TileEntityAltar altar,
	                                  AltarDefinition definition, EntityPlayer player) {
		// Find the prayer effect
		AltarEffect.PrayerEffect prayerEffect = null;
		for (AltarEffect effect : definition.getEffects()) {
			if (effect instanceof AltarEffect.PrayerEffect) {
				prayerEffect = (AltarEffect.PrayerEffect) effect;
				break;
			}
		}
		
		if (prayerEffect == null) {
			return false;
		}
		
		// Get or create prayer data
		NBTTagCompound prayerData = altar.getDataValue("prayer_data");
		if (prayerData == null) {
			prayerData = new NBTTagCompound();
		}
		
		String playerKey = "player_" + player.getUniqueID().toString();
		long lastPrayerTime = prayerData.getLong(playerKey);
		long currentTime = world.getTotalWorldTime();
		
		// Check cooldown
		if (lastPrayerTime > 0 && (currentTime - lastPrayerTime) < COOLDOWN_TICKS) {
			long remainingTicks = COOLDOWN_TICKS - (currentTime - lastPrayerTime);
			long remainingMinutes = remainingTicks / 1200; // 1200 ticks = 1 minute
			
			player.sendMessage(new TextComponentString(
					TextFormatting.YELLOW + "Your prayers have not yet been answered. " +
					"Wait " + remainingMinutes + " more minutes."));
			return true; // Block interaction
		}
		
		// Display prayer text (if not hidden)
		if (!prayerEffect.isHidden()) {
			player.sendMessage(new TextComponentString(
					TextFormatting.GOLD + "Prayer: " + TextFormatting.ITALIC + prayerEffect.getPrayerText()));
		}
		
		// Apply success effects
		NBTTagCompound altarData = new NBTTagCompound();
		altar.writeToNBT(altarData);
		
		if (prayerEffect.getSuccessEffects() != null) {
			for (AltarEffect successEffect : prayerEffect.getSuccessEffects()) {
				successEffect.apply(world, pos, player, altarData);
			}
		}
		
		// Store prayer time
		prayerData.setLong(playerKey, currentTime);
		altar.setDataValue("prayer_data", prayerData);
		
		// Update usage counts
		altar.incrementPlayerUses(player.getUniqueID());
		altar.incrementTotalUses();
		
		player.sendMessage(new TextComponentString(
				TextFormatting.GREEN + "Your prayer has been heard!"));
		
		return true; // Fully handled
	}
}
