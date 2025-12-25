package com.windanesz.menhir.altar.handler;

import com.windanesz.menhir.api.altar.AltarDefinition;
import com.windanesz.menhir.api.altar.AltarEffect;
import com.windanesz.menhir.api.altar.IAltarEffectHandler;
import com.windanesz.menhir.tileentity.TileEntityAltar;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

/**
 * Handler for temporary item effects.
 * Gives players items that are marked as temporary and will disappear after:
 * - A set duration (in ticks)
 * - When the player logs out (if configured)
 * 
 * Items are tracked in player NBT and checked/removed by an event handler.
 */
public class TemporaryItemHandler implements IAltarEffectHandler {
	
	@Override
	public String getEffectType() {
		return "temporary_item";
	}
	
	@Override
	public boolean canHandle(AltarDefinition definition) {
		for (AltarEffect effect : definition.getEffects()) {
			if (effect instanceof AltarEffect.TemporaryItemEffect) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean handleInteraction(World world, BlockPos pos, TileEntityAltar altar,
	                                  AltarDefinition definition, EntityPlayer player) {
		// Find temporary item effects
		boolean anyGiven = false;
		
		for (AltarEffect effect : definition.getEffects()) {
			if (effect instanceof AltarEffect.TemporaryItemEffect) {
				AltarEffect.TemporaryItemEffect itemEffect = (AltarEffect.TemporaryItemEffect) effect;
				
				// Give the item to the player
				ItemStack stack = itemEffect.getItemStack().copy();
				
				// Add special NBT tag to mark it as temporary
				if (!stack.hasTagCompound()) {
					stack.setTagCompound(new NBTTagCompound());
				}
				NBTTagCompound tag = stack.getTagCompound();
				tag.setBoolean("MenhirTemporary", true);
				tag.setLong("MenhirExpiryTime", world.getTotalWorldTime() + itemEffect.getDurationTicks());
				tag.setBoolean("MenhirRemoveOnLogout", itemEffect.isRemoveOnLogout());
				tag.setString("MenhirSourceAltar", definition.getName());
				
				// Try to add to player inventory
				boolean added = player.inventory.addItemStackToInventory(stack);
				
				if (added) {
					// Track in player NBT for cleanup
					trackTemporaryItem(player, stack, world.getTotalWorldTime() + itemEffect.getDurationTicks(), 
					                   itemEffect.isRemoveOnLogout());
					
					int durationSeconds = itemEffect.getDurationTicks() / 20;
					int durationMinutes = durationSeconds / 60;
					
					String durationText = durationMinutes > 0 
						? durationMinutes + " minute" + (durationMinutes > 1 ? "s" : "")
						: durationSeconds + " second" + (durationSeconds > 1 ? "s" : "");
					
					player.sendMessage(new TextComponentString(
							TextFormatting.GOLD + "Received temporary item: " + 
							TextFormatting.WHITE + stack.getDisplayName()));
					player.sendMessage(new TextComponentString(
							TextFormatting.GRAY + "Duration: " + durationText + 
							(itemEffect.isRemoveOnLogout() ? " (removed on logout)" : "")));
					
					anyGiven = true;
				} else {
					// Inventory full, drop on ground
					EntityItem entityItem = new EntityItem(world, player.posX, player.posY, player.posZ, stack);
					world.spawnEntity(entityItem);
					
					player.sendMessage(new TextComponentString(
							TextFormatting.YELLOW + "Item dropped at your feet (inventory full)"));
					anyGiven = true;
				}
			}
		}
		
		if (anyGiven) {
			// Update usage counts
			altar.incrementPlayerUses(player.getUniqueID());
			altar.incrementTotalUses();
			return true; // Fully handled
		}
		
		return false;
	}
	
	private void trackTemporaryItem(EntityPlayer player, ItemStack stack, long expiryTime, boolean removeOnLogout) {
		NBTTagCompound playerData = player.getEntityData();
		if (!playerData.hasKey("MenhirTemporaryItems")) {
			playerData.setTag("MenhirTemporaryItems", new NBTTagList());
		}
		
		NBTTagList tempItems = playerData.getTagList("MenhirTemporaryItems", Constants.NBT.TAG_COMPOUND);
		
		NBTTagCompound itemData = new NBTTagCompound();
		stack.writeToNBT(itemData);
		itemData.setLong("expiryTime", expiryTime);
		itemData.setBoolean("removeOnLogout", removeOnLogout);
		
		tempItems.appendTag(itemData);
		playerData.setTag("MenhirTemporaryItems", tempItems);
	}
}
