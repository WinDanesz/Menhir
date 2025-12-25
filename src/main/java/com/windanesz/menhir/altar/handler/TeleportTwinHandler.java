package com.windanesz.menhir.altar.handler;

import com.windanesz.menhir.api.altar.AltarDefinition;
import com.windanesz.menhir.api.altar.AltarEffect;
import com.windanesz.menhir.api.altar.AltarRarity;
import com.windanesz.menhir.api.altar.IAltarEffectHandler;
import com.windanesz.menhir.core.BirthsignRegistrationHandler;
import com.windanesz.menhir.tileentity.TileEntityAltar;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Handler for teleport twin altars.
 * On first use, generates a twin altar far away and teleports the player to it.
 * Subsequent uses teleport between the twin altars.
 */
public class TeleportTwinHandler implements IAltarEffectHandler {
	
	@Override
	public String getEffectType() {
		return "teleport_twin";
	}
	
	@Override
	public boolean canHandle(AltarDefinition definition) {
		for (AltarEffect effect : definition.getEffects()) {
			if (effect instanceof AltarEffect.TeleportTwinEffect) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean handleInteraction(World world, BlockPos pos, TileEntityAltar altar,
	                                  AltarDefinition definition, EntityPlayer player) {
		// Find the teleport twin effect
		String twinId = null;
		for (AltarEffect effect : definition.getEffects()) {
			if (effect instanceof AltarEffect.TeleportTwinEffect) {
				twinId = ((AltarEffect.TeleportTwinEffect) effect).getTwinId();
				break;
			}
		}
		
		if (twinId == null) {
			return false;
		}
		
		// Check if twin already exists in altar data
		NBTTagCompound twinData = altar.getDataValue("teleport_twin");
		BlockPos twinPos = null;
		int dimension = 0;
		
		if (twinData != null && twinData.hasKey("x")) {
			twinPos = new BlockPos(twinData.getInteger("x"), twinData.getInteger("y"), twinData.getInteger("z"));
			dimension = twinData.getInteger("dimension");
		}
		
		if (twinPos != null) {
			// Twin exists, teleport player
			teleportPlayerToTwin(player, twinPos, dimension);
			
			// Update usage counts
			altar.incrementPlayerUses(player.getUniqueID());
			altar.incrementTotalUses();
			return true;
		}
		
		// First use - need to generate twin altar
		player.sendMessage(new TextComponentString(TextFormatting.GOLD + "Searching for a suitable location..."));
		
		BlockPos newTwinPos = findAndGenerateTwinAltar(world, pos, definition, twinId, world.provider.getDimension());
		if (newTwinPos != null) {
			// Store twin position in this altar's data
			NBTTagCompound newTwinData = new NBTTagCompound();
			newTwinData.setInteger("x", newTwinPos.getX());
			newTwinData.setInteger("y", newTwinPos.getY());
			newTwinData.setInteger("z", newTwinPos.getZ());
			newTwinData.setInteger("dimension", world.provider.getDimension());
			altar.setDataValue("teleport_twin", newTwinData);
			
			// Store this altar's position in the new twin altar's data
			TileEntity twinTE = world.getTileEntity(newTwinPos);
			if (twinTE instanceof TileEntityAltar) {
				TileEntityAltar twinAltar = (TileEntityAltar) twinTE;
				NBTTagCompound backLinkData = new NBTTagCompound();
				backLinkData.setInteger("x", pos.getX());
				backLinkData.setInteger("y", pos.getY());
				backLinkData.setInteger("z", pos.getZ());
				backLinkData.setInteger("dimension", world.provider.getDimension());
				twinAltar.setDataValue("teleport_twin", backLinkData);
			}
			
			teleportPlayerToTwin(player, newTwinPos, world.provider.getDimension());
			
			// Update usage counts
			altar.incrementPlayerUses(player.getUniqueID());
			altar.incrementTotalUses();
			
			player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Twin altar created!"));
			return true;
		} else {
			player.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to find suitable location for twin altar."));
			return false;
		}
	}
	
	private BlockPos findAndGenerateTwinAltar(World world, BlockPos originalPos, AltarDefinition definition, String twinId, int originalDimension) {
		Random random = world.rand;
		int minDistance = 1000; // Minimum 1000 blocks away
		int maxDistance = 5000; // Maximum 5000 blocks away
		int attempts = 20;
		
		for (int i = 0; i < attempts; i++) {
			// Pick random direction and distance
			double angle = random.nextDouble() * 2 * Math.PI;
			int distance = minDistance + random.nextInt(maxDistance - minDistance);
			
			int x = originalPos.getX() + (int)(Math.cos(angle) * distance);
			int z = originalPos.getZ() + (int)(Math.sin(angle) * distance);
			
			// Find suitable ground level
			BlockPos surfacePos = world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z));
			
			// Check if location is suitable
			if (canGenerateAltarAt(world, surfacePos)) {
				// Generate the twin altar
				generateTwinAltar(world, surfacePos, definition);
				return surfacePos;
			}
		}
		
		return null; // Failed to find suitable location
	}
	
	private boolean canGenerateAltarAt(World world, BlockPos pos) {
		// Check if there's solid ground
		if (!world.getBlockState(pos.down()).getMaterial().isSolid()) {
			return false;
		}
		
		// Check if there's enough space (3 blocks tall) - must be air or replaceable
		for (int y = 0; y < 3; y++) {
			BlockPos checkPos = pos.up(y);
			IBlockState state = world.getBlockState(checkPos);
			Block block = state.getBlock();
			if (!block.isAir(state, world, checkPos) && !block.isReplaceable(world, checkPos)) {
				return false;
			}
		}
		
		// Check for nearby structures (avoid generating too close to other things)
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				for (int dy = -2; dy <= 5; dy++) {
					BlockPos checkPos = pos.add(dx, dy, dz);
					IBlockState state = world.getBlockState(checkPos);
					// Avoid generating near other altars or important structures
					if (state.getBlock() == BirthsignRegistrationHandler.ALTAR) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	private void generateTwinAltar(World world, BlockPos pos, AltarDefinition definition) {
		// Import the BlockPosition enum from BlockAltar
		com.windanesz.menhir.block.BlockAltar.BlockPosition BOTTOM = com.windanesz.menhir.block.BlockAltar.BlockPosition.BOTTOM;
		com.windanesz.menhir.block.BlockAltar.BlockPosition MIDDLE = com.windanesz.menhir.block.BlockAltar.BlockPosition.MIDDLE;
		com.windanesz.menhir.block.BlockAltar.BlockPosition TOP = com.windanesz.menhir.block.BlockAltar.BlockPosition.TOP;
		
		EnumFacing facing = EnumFacing.HORIZONTALS[world.rand.nextInt(EnumFacing.HORIZONTALS.length)];
		
		// Clear the space first (replace any grass, flowers, etc.)
		for (int y = 0; y < 3; y++) {
			world.setBlockState(pos.up(y), Blocks.AIR.getDefaultState(), 2);
		}
		
		// Place the 3-block altar structure
		world.setBlockState(pos, BirthsignRegistrationHandler.ALTAR.getDefaultState()
				.withProperty(com.windanesz.menhir.block.BlockAltar.FACING, facing)
				.withProperty(com.windanesz.menhir.block.BlockAltar.POSITION, BOTTOM), 2);
		world.setBlockState(pos.up(), BirthsignRegistrationHandler.ALTAR.getDefaultState()
				.withProperty(com.windanesz.menhir.block.BlockAltar.FACING, facing)
				.withProperty(com.windanesz.menhir.block.BlockAltar.POSITION, MIDDLE), 2);
		world.setBlockState(pos.up(2), BirthsignRegistrationHandler.ALTAR.getDefaultState()
				.withProperty(com.windanesz.menhir.block.BlockAltar.FACING, facing)
				.withProperty(com.windanesz.menhir.block.BlockAltar.POSITION, TOP), 2);
		
		// Set tile entity data
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof TileEntityAltar) {
			TileEntityAltar altar = (TileEntityAltar) te;
			altar.setAltarId(definition.getId());
		}
		
		// Add platform based on rarity
		addPlatform(world, pos, definition.getRarity());
	}
	
	private void addPlatform(World world, BlockPos pos, AltarRarity rarity) {
		Block platformBlock;
		switch (rarity) {
			case COMMON:
				platformBlock = Blocks.COBBLESTONE;
				break;
			case UNCOMMON:
				platformBlock = Blocks.STONE_BRICK_STAIRS;
				break;
			case RARE:
				platformBlock = Blocks.QUARTZ_BLOCK;
				break;
			case EPIC:
				platformBlock = Blocks.PURPUR_BLOCK;
				break;
			case LEGENDARY:
				platformBlock = Blocks.END_STONE;
				break;
			default:
				platformBlock = Blocks.STONE;
		}
		
		// Create a small platform
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos platformPos = pos.add(dx, -1, dz);
				if (world.getBlockState(platformPos).getBlock().isReplaceable(world, platformPos)) {
					world.setBlockState(platformPos, platformBlock.getDefaultState(), 2);
				}
			}
		}
	}
	
	private void teleportPlayerToTwin(EntityPlayer player, BlockPos twinPos, int dimension) {
		if (player instanceof EntityPlayerMP) {
			EntityPlayerMP playerMP = (EntityPlayerMP) player;
			
			if (playerMP.dimension != dimension) {
				// Cross-dimension teleport
				playerMP.getServer().getPlayerList().transferPlayerToDimension(
						playerMP, dimension, new net.minecraftforge.common.util.ITeleporter() {
							@Override
							public void placeEntity(World world, Entity entity, float yaw) {
								// Teleport above the 3-block tall altar structure
								entity.setPositionAndUpdate(twinPos.getX() + 0.5, twinPos.getY() + 3, twinPos.getZ() + 0.5);
							}
						});
			} else {
				// Same dimension teleport - place above the 3-block tall altar
				playerMP.setPositionAndUpdate(twinPos.getX() + 0.5, twinPos.getY() + 3, twinPos.getZ() + 0.5);
			}
			
			// Play teleport sound
			playerMP.world.playSound(null, playerMP.posX, playerMP.posY, playerMP.posZ,
					SoundEvents.ENTITY_ENDERMEN_TELEPORT,
					SoundCategory.PLAYERS, 1.0F, 1.0F);
			
			player.sendMessage(new TextComponentString(TextFormatting.LIGHT_PURPLE + "You have been teleported to the twin altar!"));
		}
	}
}
