package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.ability.ChannelingAbility;
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

public class VerdantBondAbility extends ChannelingAbility {

	private final int radius;

	public VerdantBondAbility(int chargeup, int radius) {
		super(chargeup);
		this.radius = radius;
	}

	public static VerdantBondAbility create(Map<String, Object> params, String birthsignName) {
		int chargeup = getChargeup(params, 0);
		int radius = 20; // Default 20 blocks

		if (params.containsKey("radius")) {
			Object radiusObj = params.get("radius");
			if (radiusObj instanceof Number) {
				radius = ((Number) radiusObj).intValue();
			}
		}

		return new VerdantBondAbility(chargeup, radius);
	}

	@Override
	protected boolean executeAbility(EntityPlayer player, @Nullable Entity target) {
		// This is a passive ability, so executeAbility is empty
		// The actual effect is handled by the event handler
		return false;
	}

	/**
	 * Called periodically to accelerate plant growth in the player's radius
	 */
	public void acceleratePlantGrowth(EntityPlayer player) {
		World world = player.world;
		if (world.isRemote) return; // Only run on server side

		BlockPos playerPos = player.getPosition();
		int radius = this.radius;

		int cropsAccelerated = 0;
		// Check blocks in radius around player
		outer:
		for (int x = -radius; x <= radius; x++) {
			for (int y = -8; y <= 8; y++) { // Limit vertical range for performance
				for (int z = -radius; z <= radius; z++) {
					BlockPos pos = playerPos.add(x, y, z);
					IBlockState state = world.getBlockState(pos);
					Block block = state.getBlock();

					// Accelerate crop growth (limit to 2 per call)
					if (block instanceof BlockCrops) {
						BlockCrops crop = (BlockCrops) block;
						if (crop.canGrow(world, pos, state, world.isRemote)) {
							// Random chance to accelerate growth
							if (world.rand.nextFloat() < 0.2f) {
								crop.grow(world, pos, state);
								cropsAccelerated++;
								if (cropsAccelerated >= 2) {
									break outer;
								}
							}
						}
					}

					// Accelerate sapling growth
					else if (block instanceof BlockSapling) {
						BlockSapling sapling = (BlockSapling) block;
						if (sapling.canGrow(world, pos, state, world.isRemote)) {
							// Random chance to accelerate growth
							if (world.rand.nextFloat() < 0.1f) { // 10% chance per tick (saplings are slower)
								sapling.grow(world, pos, state, world.rand);
							}
						}
					}

					// Accelerate grass growth
					else if (block == Blocks.GRASS) {
						// Random chance to spread grass to nearby dirt blocks
						if (world.rand.nextFloat() < 0.05f) { // 5% chance per tick
							BlockPos[] nearbyDirt = {
									pos.north(), pos.south(), pos.east(), pos.west(),
									pos.north().east(), pos.north().west(), pos.south().east(), pos.south().west()
							};

							for (BlockPos dirtPos : nearbyDirt) {
								if (world.getBlockState(dirtPos).getBlock() == Blocks.DIRT) {
									world.setBlockState(dirtPos, Blocks.GRASS.getDefaultState());
									break; // Only convert one dirt block per tick
								}
							}
						}
					}
				}
			}
		}
	}

	public int getRadius() {
		return radius;
	}
}
