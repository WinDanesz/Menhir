package com.windanesz.menhir.worldgen;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.api.altar.AltarDefinition;
import com.windanesz.menhir.core.AltarRegistry;
import com.windanesz.menhir.core.BirthsignRegistrationHandler;
import com.windanesz.menhir.tileentity.TileEntityAltar;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

public class AltarWorldGenerator implements IWorldGenerator {

	// Configuration - should be moved to a config file
	private static int ALTAR_SPAWN_CHANCE = 3; // 1 in X chunks
	private static final int MIN_ALTAR_HEIGHT = 0;
	private static final int MAX_ALTAR_HEIGHT = 90;

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
		// Only generate in overworld for now
		if (world.provider.getDimension() != 0) {
			return;
		}

		// Random chance to spawn an altar in this chunk
		if (random.nextInt(ALTAR_SPAWN_CHANCE) != 0) {
			return;
		}

		// Get a random altar definition
		AltarDefinition definition = AltarRegistry.getRandomAltar(random);
		if (definition == null) {
			return;
		}

		// Check unique per world restriction
		if (definition.isUniquePerWorld()) {
			// TODO: Implement world-wide tracking of spawned unique altars
			// For now, we'll skip this check
		}

		// Find a suitable location in the chunk
		int x = chunkX * 16 + random.nextInt(16);
		int z = chunkZ * 16 + random.nextInt(16);
		int y = findSuitableHeight(world, x, z, random);

		if (y == -1) {
			return; // No suitable location found
		}

		BlockPos pos = new BlockPos(x, y, z);

		// Generate the altar structure
		generateAltarStructure(world, pos, definition, random);

		Menhir.logger.info("Generated altar '{}' ({}) at [{}, {}, {}]", 
				definition.getName(), 
				definition.getRarity().getName(), 
				pos.getX(), 
				pos.getY(), 
				pos.getZ());
	}

	private int findSuitableHeight(World world, int x, int z, Random random) {
		// Try to find a solid surface between min and max height
		for (int y = MAX_ALTAR_HEIGHT; y >= MIN_ALTAR_HEIGHT; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			IBlockState state = world.getBlockState(pos);
			IBlockState below = world.getBlockState(pos.down());

			// Check if this is a good spot (air above, solid ground below)
			if (state.getBlock() == Blocks.AIR && below.isSideSolid(world, pos.down(), net.minecraft.util.EnumFacing.UP)) {
				// Check if there's enough space (3x3x4 area)
				if (hasEnoughSpace(world, pos)) {
					return y;
				}
			}
		}

		return -1;
	}

	private boolean hasEnoughSpace(World world, BlockPos pos) {
		// Check 3x3 area, 4 blocks tall
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				for (int y = 0; y < 4; y++) {
					BlockPos checkPos = pos.add(x, y, z);
					if (!world.getBlockState(checkPos).getBlock().isReplaceable(world, checkPos)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private void generateAltarStructure(World world, BlockPos pos, AltarDefinition definition, Random random) {
		// Clear the area (need 3 blocks tall for the altar)
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				for (int y = 0; y < 4; y++) {
					BlockPos clearPos = pos.add(x, y, z);
					world.setBlockToAir(clearPos);
				}
			}
		}

		// Create a platform
		IBlockState platformBlock = getPlatformBlock(definition);
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				BlockPos platformPos = pos.add(x, -1, z);
				world.setBlockState(platformPos, platformBlock, 2);
			}
		}

		// Place the altar multiblock structure (3 blocks tall)
		net.minecraft.util.EnumFacing facing = net.minecraft.util.EnumFacing.NORTH;
		
		// Bottom block
		world.setBlockState(pos, BirthsignRegistrationHandler.ALTAR.getDefaultState()
				.withProperty(com.windanesz.menhir.block.BlockAltar.FACING, facing)
				.withProperty(com.windanesz.menhir.block.BlockAltar.POSITION, com.windanesz.menhir.block.BlockAltar.BlockPosition.BOTTOM), 2);
		
		// Middle block
		world.setBlockState(pos.up(), BirthsignRegistrationHandler.ALTAR.getDefaultState()
				.withProperty(com.windanesz.menhir.block.BlockAltar.FACING, facing)
				.withProperty(com.windanesz.menhir.block.BlockAltar.POSITION, com.windanesz.menhir.block.BlockAltar.BlockPosition.MIDDLE), 2);
		
		// Top block
		world.setBlockState(pos.up(2), BirthsignRegistrationHandler.ALTAR.getDefaultState()
				.withProperty(com.windanesz.menhir.block.BlockAltar.FACING, facing)
				.withProperty(com.windanesz.menhir.block.BlockAltar.POSITION, com.windanesz.menhir.block.BlockAltar.BlockPosition.TOP), 2);

		// Set the altar definition on the bottom block's tile entity
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof TileEntityAltar) {
			TileEntityAltar altar = (TileEntityAltar) te;
			altar.setAltarId(definition.getId());
			altar.markDirty();
		}

		// Add decorative elements based on rarity
		addDecorativeElements(world, pos, definition, random);
	}

	private IBlockState getPlatformBlock(AltarDefinition definition) {
		// Different platform blocks based on rarity
		switch (definition.getRarity()) {
			case COMMON:
				return Blocks.COBBLESTONE.getDefaultState();
			case UNCOMMON:
				return Blocks.STONE_BRICK_STAIRS.getDefaultState();
			case RARE:
				return Blocks.MOSSY_COBBLESTONE.getDefaultState();
			case EPIC:
				return Blocks.OBSIDIAN.getDefaultState();
			case LEGENDARY:
				return Blocks.END_STONE.getDefaultState();
			default:
				return Blocks.STONE.getDefaultState();
		}
	}

	private void addDecorativeElements(World world, BlockPos pos, AltarDefinition definition, Random random) {
		// Add torches or other decorative elements around the altar
		switch (definition.getRarity()) {
			case UNCOMMON:
			case RARE:
				// Add torches at corners
				placeIfAir(world, pos.add(-1, 0, -1), Blocks.TORCH.getDefaultState());
				placeIfAir(world, pos.add(1, 0, -1), Blocks.TORCH.getDefaultState());
				placeIfAir(world, pos.add(-1, 0, 1), Blocks.TORCH.getDefaultState());
				placeIfAir(world, pos.add(1, 0, 1), Blocks.TORCH.getDefaultState());
				break;

			case EPIC:
				// Add glowstone
				placeIfAir(world, pos.add(-2, 1, 0), Blocks.GLOWSTONE.getDefaultState());
				placeIfAir(world, pos.add(2, 1, 0), Blocks.GLOWSTONE.getDefaultState());
				placeIfAir(world, pos.add(0, 1, -2), Blocks.GLOWSTONE.getDefaultState());
				placeIfAir(world, pos.add(0, 1, 2), Blocks.GLOWSTONE.getDefaultState());
				break;

			case LEGENDARY:
				// Add sea lanterns and special blocks
				for (int x = -2; x <= 2; x++) {
					for (int z = -2; z <= 2; z++) {
						if (Math.abs(x) == 2 || Math.abs(z) == 2) {
							if (random.nextInt(3) == 0) {
								placeIfAir(world, pos.add(x, 0, z), Blocks.SEA_LANTERN.getDefaultState());
							}
						}
					}
				}
				break;
		}
	}

	private void placeIfAir(World world, BlockPos pos, IBlockState state) {
		if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
			world.setBlockState(pos, state, 2);
		}
	}
}
