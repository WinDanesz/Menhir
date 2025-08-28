package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

public class NaturesEmbraceAbility implements IBirthsignActiveAbility {

	private final int radius;

	public NaturesEmbraceAbility(int radius) {
		this.radius = radius;
	}

	public static NaturesEmbraceAbility create(Map<String, Object> params, String birthsignName) {
		int radius = 3; // Default 3 blocks

		if (params.containsKey("radius")) {
			Object radiusObj = params.get("radius");
			if (radiusObj instanceof Number) {
				radius = ((Number) radiusObj).intValue();
			}
		}

		return new NaturesEmbraceAbility(radius);
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		World world = player.world;
		if (world.isRemote) return false; // Only run on server side

		BlockPos playerPos = player.getPosition();
		int radius = this.radius;
		int plantsAffected = 0;

		// Check blocks in radius around player
		for (int x = -radius; x <= radius; x++) {
			for (int y = -3; y <= 3; y++) { // Limit vertical range for the active ability
				for (int z = -radius; z <= radius; z++) {
					BlockPos pos = playerPos.add(x, y, z);
					IBlockState state = world.getBlockState(pos);
					Block block = state.getBlock();

					// Instantly mature crops
					if (block instanceof BlockCrops) {
						BlockCrops crop = (BlockCrops) block;
						if (crop.canGrow(world, pos, state, world.isRemote)) {
							// Force the crop to mature
							while (crop.canGrow(world, pos, state, world.isRemote)) {
								crop.grow(world, pos, state);
								state = world.getBlockState(pos); // Update state after growth
								plantsAffected++;
							}
						}
					}

					// Instantly grow saplings
					else if (block instanceof BlockSapling) {
						BlockSapling sapling = (BlockSapling) block;
						if (sapling.canGrow(world, pos, state, world.isRemote)) {
							// Force the sapling to grow into a tree
							sapling.grow(world, pos, state, world.rand);
							plantsAffected++;
						}
					}

					// Convert dirt to grass
					else if (block == Blocks.DIRT) {
						// Check if there's grass nearby to spread from
						boolean hasNearbyGrass = false;
						for (int dx = -2; dx <= 2; dx++) {
							for (int dz = -2; dz <= 2; dz++) {
								BlockPos grassPos = pos.add(dx, 0, dz);
								if (world.getBlockState(grassPos).getBlock() == Blocks.GRASS) {
									hasNearbyGrass = true;
									break;
								}
							}
						}

						if (hasNearbyGrass) {
							world.setBlockState(pos, Blocks.GRASS.getDefaultState());
							plantsAffected++;
						}
					}

					// Add flowers and tall grass to grass blocks
					else if (block == Blocks.GRASS) {
						BlockPos above = pos.up();
						if (world.isAirBlock(above)) {
							// Apply actual bonemeal effect to nearby blocks
							// This will naturally spawn appropriate plants for the biome
							if (world.rand.nextFloat() < 0.4f) { // 40% chance
								// Use bonemeal on the grass block to spawn plants naturally
								net.minecraft.item.ItemDye.applyBonemeal(
										new net.minecraft.item.ItemStack(net.minecraft.init.Items.DYE, 1, 15),
										world,
										pos
								);

								plantsAffected++;
							}
						}
					}
				}
			}
		}

		return true;
	}

	public int getRadius() {
		return radius;
	}
}
