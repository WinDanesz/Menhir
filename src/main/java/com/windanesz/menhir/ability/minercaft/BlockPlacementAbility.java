package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.ability.ChannelingAbility;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Generic block placement ability that allows players to place blocks where they're looking.
 * This is used by The Mountain birthsign and can be configured to place any block type.
 */
public class BlockPlacementAbility extends ChannelingAbility {

	private static final int MAX_PLACEMENT_DISTANCE = 5; // Maximum distance to place blocks
	private final String blockToPlace;

	public BlockPlacementAbility(int chargeup, String blockToPlace) {
		super(chargeup);
		this.blockToPlace = blockToPlace;
	}

	/**
	 * Creates a new TorchPlacementAbility instance from parameters.
	 */
	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		int chargeup = getChargeup(params, 0);
		String block = ParameterUtils.getStringParameter(params, "block", "minecraft:torch");
		return new BlockPlacementAbility(chargeup, block);
	}

	@Override
	protected boolean executeAbility(EntityPlayer player, @Nullable Entity target) {
		if (player == null || player.world == null) {
			return false;
		}

		World world = player.world;
		if (world.isRemote) {
			return false; // Only run on server side
		}

		// Get the block the player is looking at
		RayTraceResult rayTrace = player.rayTrace(MAX_PLACEMENT_DISTANCE, 1.0f);
		if (rayTrace == null || rayTrace.typeOfHit != RayTraceResult.Type.BLOCK) {
			return false;
		}

		BlockPos targetPos = rayTrace.getBlockPos();
		EnumFacing side = rayTrace.sideHit;

		// Get the block to place from the registry
		Block block = Block.REGISTRY.getObject(new ResourceLocation(blockToPlace));
		if (block == null) {
			return false;
		}

		// For blocks that attach to surfaces (like torches), place on the surface
		// For regular blocks, place in air next to the surface
		BlockPos placementPos = targetPos.offset(side);

		// Check if the block can be placed at the target position
		if (!world.isAirBlock(placementPos)) {
			return false;
		}

		// Check if the target block can support the block to be placed
		if (!canSupportBlock(world, targetPos, side)) {
			return false;
		}

		// Check chunk claim compatibility and other placement restrictions
		if (!canPlaceBlock(player, world, placementPos)) {
			return false;
		}

		// Place the block
		boolean success = placeBlock(world, placementPos, side, block, player);

		if (success) {
			// Play block placement sound
			world.playSound(null, placementPos, net.minecraft.init.SoundEvents.BLOCK_STONE_PLACE,
					net.minecraft.util.SoundCategory.BLOCKS, 1.0f, 1.0f);

			// Spawn block placement particles
			spawnPlacementParticles(world, placementPos);
		}
		return success;
	}

	/**
	 * Checks if a block can support the block to be placed.
	 */
	private boolean canSupportBlock(World world, BlockPos pos, EnumFacing side) {
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();

		// Most blocks can be placed on solid surfaces
		if (side == EnumFacing.UP) {
			// Top placement - needs a solid top surface
			return block.isSideSolid(state, world, pos, EnumFacing.UP);
		} else {
			// Side placement - needs a solid side surface
			return block.isSideSolid(state, world, pos, side);
		}
	}

	/**
	 * Places the configured block at the specified position.
	 */
	private boolean placeBlock(World world, BlockPos pos, EnumFacing side, Block block, EntityPlayer player) {
		// Use the block's own placement logic - it knows how to orient itself
		IBlockState blockState = block.getStateForPlacement(world, pos, side, 0, 0, 0, 0, player);
		return world.setBlockState(pos, blockState);
	}

	/**
	 * Spawns particles at the block placement location.
	 */
	private void spawnPlacementParticles(World world, BlockPos pos) {
		if (world.isRemote) {
			return; // Only spawn particles on client side
		}

		// Send particle packet to nearby players
		for (EntityPlayer nearbyPlayer : world.playerEntities) {
			if (nearbyPlayer.getDistance(pos.getX(), pos.getY(), pos.getZ()) < 32.0) {
				// Spawn block placement particles
				world.spawnParticle(net.minecraft.util.EnumParticleTypes.SMOKE_NORMAL,
						pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
						0.0, 0.1, 0.0);
			}
		}
	}

	/**
	 * Safeguard for chunk claim compatibility and other placement restrictions.
	 */
	private boolean canPlaceBlock(@Nullable Entity placer, World world, BlockPos pos) {
		if (world.isRemote) {
			return true; // Client-side check
		}

		// Check if entity can damage/modify blocks
		if (!canEntityModifyBlocks(placer, world)) {
			return false;
		}

		if (world.isOutsideBuildHeight(pos)) {
			return false;
		}

		// Check if player can modify the block at this position
		if (placer instanceof EntityPlayer && !world.isBlockModifiable((EntityPlayer) placer, pos)) {
			return false;
		}

		// Check Forge events for block placement
		net.minecraftforge.common.util.BlockSnapshot snapshot = net.minecraftforge.common.util.BlockSnapshot.getBlockSnapshot(world, pos);
		if (net.minecraftforge.event.ForgeEventFactory.onBlockPlace(placer, snapshot, EnumFacing.UP).isCanceled()) {
			return false;
		}

		if (placer instanceof EntityPlayer && net.minecraftforge.event.ForgeEventFactory.onPlayerBlockPlace(
				(EntityPlayer) placer, snapshot, EnumFacing.UP, net.minecraft.util.EnumHand.MAIN_HAND).isCanceled()) {
			return false;
		}

		return true;
	}

	/**
	 * Checks if an entity can modify blocks in the world.
	 */
	private boolean canEntityModifyBlocks(@Nullable Entity entity, World world) {
		// This is a simplified version - you might want to implement more specific checks
		// based on your mod's requirements
		return entity != null && entity.isEntityAlive();
	}
}

