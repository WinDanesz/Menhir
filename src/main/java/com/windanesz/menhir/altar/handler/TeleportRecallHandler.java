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
 * Handler for teleport recall altars.
 * These altars mark a location that the player can return to later.
 * The actual recall is typically triggered by a command or item (not implemented yet).
 * 
 * This handler just stores the location in the player's NBT data.
 */
public class TeleportRecallHandler implements IAltarEffectHandler {
	
	@Override
	public String getEffectType() {
		return "teleport_recall";
	}
	
	@Override
	public boolean canHandle(AltarDefinition definition) {
		for (AltarEffect effect : definition.getEffects()) {
			if (effect instanceof AltarEffect.TeleportRecallEffect) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean handleInteraction(World world, BlockPos pos, TileEntityAltar altar,
	                                  AltarDefinition definition, EntityPlayer player) {
		// Find the recall effect
		AltarEffect.TeleportRecallEffect recallEffect = null;
		for (AltarEffect effect : definition.getEffects()) {
			if (effect instanceof AltarEffect.TeleportRecallEffect) {
				recallEffect = (AltarEffect.TeleportRecallEffect) effect;
				break;
			}
		}
		
		if (recallEffect == null) {
			return false;
		}
		
		// Store the altar position in player NBT for later recall
		NBTTagCompound playerData = player.getEntityData();
		if (!playerData.hasKey("MenhirRecallAltars")) {
			playerData.setTag("MenhirRecallAltars", new NBTTagCompound());
		}
		NBTTagCompound recallData = playerData.getCompoundTag("MenhirRecallAltars");
		
		String altarKey = definition.getId();
		NBTTagCompound altarRecall = new NBTTagCompound();
		altarRecall.setInteger("x", pos.getX());
		altarRecall.setInteger("y", pos.getY());
		altarRecall.setInteger("z", pos.getZ());
		altarRecall.setInteger("dimension", world.provider.getDimension());
		altarRecall.setInteger("maxUses", recallEffect.getMaxUses());
		altarRecall.setInteger("usesRemaining", recallEffect.getMaxUses());
		altarRecall.setString("name", definition.getName());
		
		recallData.setTag(altarKey, altarRecall);
		
		// Update usage counts
		altar.incrementPlayerUses(player.getUniqueID());
		altar.incrementTotalUses();
		
		player.sendMessage(new TextComponentString(
				TextFormatting.LIGHT_PURPLE + "Recall point set: " + definition.getName()));
		player.sendMessage(new TextComponentString(
				TextFormatting.GRAY + "You can return here " + recallEffect.getMaxUses() + " times."));
		player.sendMessage(new TextComponentString(
				TextFormatting.GRAY + "Use /menhir recall " + definition.getId() + " to teleport back."));
		
		return true; // Fully handled
	}
}
