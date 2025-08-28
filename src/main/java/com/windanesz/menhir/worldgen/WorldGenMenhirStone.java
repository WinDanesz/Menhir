package com.windanesz.menhir.worldgen;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.Settings;
import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.block.BlockMenhirStone;
import com.windanesz.menhir.core.BirthsignRegistrationHandler;
import com.windanesz.menhir.integration.antiqueatlas.MenhirAntiqueAtlasIntegration;
import com.windanesz.menhir.tileentity.TileEntityMenhirStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * World generator for menhir stones.
 */
public class WorldGenMenhirStone implements IWorldGenerator {

	public static final String MENHIR_SPAWN_LOCATIONS = "MenhirSpawnLocations";
	public static final String PLACED_MENHIRS = "PlacedMenhirs";
	// Configuration constants
	// To change the spawn distance, simply modify MENHIR_SPAWN_DISTANCE_CHUNKS below
	// Each chunk is 16x16 blocks, so 20 chunks = 320 blocks from spawn
	private static final int MAX_BIRTHSIGNS_PER_WORLD = Settings.generalSettings.max_birthsigns_per_world;
	private static final int MENHIR_STONE_SPAWN_DISTANCE_CHUNKS = Settings.generalSettings.menhir_stone_spawn_distance_chunks; // Configurable distance in chunks
	private static final int MIN_DISTANCE_BETWEEN_STONES = 16; // Minimum blocks between stones
	private static final int MAX_ATTEMPTS_PER_CHUNK = 3;

	public static List<String> getMenhirsForWorld(long worldSeed) {
		List<String> allMenhirs = getAllAvailableBirthsigns();
		List<String> worldMenhirs = new ArrayList<>();

		//	Menhir.logger.info("getbirthsignsForWorld called with seed: {}, found {} available birthsigns", worldSeed, allbirthsigns.size());

		// Log all available birthsigns
		//Menhir.logger.info("Available birthsign: {}", allbirthsigns);

		if (allMenhirs.isEmpty()) {
			///    Menhir.logger.warn("No birthsigns available for world generation");
			return worldMenhirs;
		}

		Random seedRand = new Random(worldSeed);
		int birthsignCount = Math.min(MAX_BIRTHSIGNS_PER_WORLD, allMenhirs.size()); // TODO: move to configurable number

		List<String> availableBirthsigns = new ArrayList<>(allMenhirs);
		//Menhir.logger.info("Starting with {} available birthsigns: {}", availablebirthsigns.size(), availablebirthsigns);

		for (int i = 0; i < birthsignCount; i++) {

			if (availableBirthsigns.isEmpty()) {
				break;
			}

			int index = seedRand.nextInt(availableBirthsigns.size());
			String selectedBirthsign = availableBirthsigns.get(index);
			worldMenhirs.add(selectedBirthsign);
			availableBirthsigns.remove(index);

			//Menhir.logger.info("Selected birthsign {}: {} (iteration {}), remaining: {}", i, selectedbirthsign, i, availablebirthsigns.size());
		}

		//Menhir.logger.info("Final birthsign selection for world: {} birthsigns", worldbirthsigns.size());
		//Menhir.logger.info("Final selected birthsigns: {}", worldbirthsigns);
		return worldMenhirs;
	}

	private static List<String> getAllAvailableBirthsigns() {
		List<String> birthsignNames = new ArrayList<>();

		//Menhir.logger.info("getAllAvailablebirthsigns: Checking birthsign.registry...");

		if (Birthsign.registry != null) {
			//Menhir.logger.info("birthsign registry found, checking for available birthsigns");
			//Menhir.logger.info("Registry size: {}", Birthsign.registry.getKeys().size());

			// Log all registry keys
			//Menhir.logger.info("Registry keys: {}", Birthsign.registry.getKeys());

			for (Birthsign birthsign : Birthsign.registry.getValues()) {
				String birthsignName = birthsign.getRegistryName().toString();
				birthsignNames.add(birthsignName);
				//	Menhir.logger.info("Found birthsign in registry: {}", birthsignName);
			}
			//Menhir.logger.info("Total birthsigns in registry: {}", birthsignNames.size());
		} else {
			//Menhir.logger.warn("birthsign registry is null! This means birthsigns haven't been loaded yet.");
			//Menhir.logger.warn("This could happen if getAllAvailablebirthsigns is called before the registry is populated.");
		}

		return birthsignNames;
	}

	/**
	 * Public method to get all stored spawn locations (useful for debugging)
	 */
	public static java.util.List<MenhirSpawnLocation> getAllStoredSpawnLocations(World world) {
		try {
			net.minecraft.nbt.NBTTagCompound worldData = world.getWorldInfo().getDimensionData(world.provider.getDimension());
			if (worldData == null || !worldData.hasKey(WorldGenMenhirStone.MENHIR_SPAWN_LOCATIONS)) {
				return new java.util.ArrayList<>();
			}

			net.minecraft.nbt.NBTTagList locationList = worldData.getTagList(MENHIR_SPAWN_LOCATIONS, net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND);
			java.util.List<MenhirSpawnLocation> locations = new java.util.ArrayList<>();

			for (int i = 0; i < locationList.tagCount(); i++) {
				net.minecraft.nbt.NBTTagCompound locData = locationList.getCompoundTagAt(i);
				int x = locData.getInteger("x");
				int y = locData.getInteger("y");
				int z = locData.getInteger("z");
				String birthsignName = locData.getString("menhir");

				locations.add(new MenhirSpawnLocation(new BlockPos(x, y, z), birthsignName));
			}

			return locations;
		} catch (Exception e) {
			Menhir.logger.error("Failed to get stored spawn locations: {}", e.getMessage());
			return new java.util.ArrayList<>();
		}
	}

	/**
	 * Public method to get all placed menhirs (useful for debugging)
	 */
	public static java.util.Set<String> getAllPlacedMenhirs(World world) {
		try {
			net.minecraft.nbt.NBTTagCompound worldData = world.getWorldInfo().getDimensionData(world.provider.getDimension());
			if (worldData == null || !worldData.hasKey(PLACED_MENHIRS)) {
				return new java.util.HashSet<>();
			}

			net.minecraft.nbt.NBTTagList placedList = worldData.getTagList(PLACED_MENHIRS, net.minecraftforge.common.util.Constants.NBT.TAG_STRING);
			java.util.Set<String> placedMenhirs = new java.util.HashSet<>();

			for (int i = 0; i < placedList.tagCount(); i++) {
				placedMenhirs.add(placedList.getStringTagAt(i));
			}

			return placedMenhirs;
		} catch (Exception e) {
			Menhir.logger.error("Failed to get placed menhir: {}", e.getMessage());
			return new java.util.HashSet<>();
		}
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
		// Only generate in the overworld (dimension 0)
		if (world.provider.getDimension() != 0) {
			return;
		}

		if (Birthsign.registry == null) {
			return;
		}

		// Check if this is the first time we're generating for this world
		if (isFirstWorldGen(world)) {
			//	Menhir.info("First world generation detected, pre-calculating all menhir locations");
			preCalculateAllMenhirLocations(world);
		} else {
			//	Menhir.debug("Not first world generation, checking for locations in chunk [{}, {}]", chunkX, chunkZ);
		}

		// Check if any pre-calculated locations fall within this chunk
		java.util.List<MenhirSpawnLocation> locationsInChunk = getLocationsInChunk(world, chunkX, chunkZ);

		//Menhir.info("Found {} locations in chunk [{}, {}]", locationsInChunk.size(), chunkX, chunkZ);

		for (MenhirSpawnLocation location : locationsInChunk) {
			// Check if this menhir hasn't been placed yet
			if (!isBirthsignPlaced(world, location.birthsignName)) {
				//Menhir.info("Attempting to place menhir '{}' at location {}", location.birthsignName, location.position);

				// Try to place the stone at the pre-calculated location
				if (placeMenhirStoneAtLocation(world, location)) {
					// Mark this menhir as placed
					markBirthsignAsPlaced(world, location.birthsignName);

					//Menhir.info("Successfully placed pre-calculated menhir stone for '{}' at {} (chunk: [{}, {}])",
					//		location.birthsignName, location.position, chunkX, chunkZ);
				} else {
					//	Menhir.warn("Failed to place menhir stone for '{}' at location {}", location.birthsignName, location.position);
				}
			} else {
				//Menhir.debug("menhir '{}' has already been placed", location.birthsignName);
			}
		}
	}

	/**
	 * Checks if this is the first time world generation is running for this world
	 */
	private boolean isFirstWorldGen(World world) {
		try {
			net.minecraft.nbt.NBTTagCompound worldData = world.getWorldInfo().getDimensionData(world.provider.getDimension());
			boolean isFirst = worldData == null || !worldData.hasKey(MENHIR_SPAWN_LOCATIONS);

			//Menhir.info("isFirstWorldGen check: worldData={}, hasKey={}, isFirst={}",
			//		worldData != null ? "exists" : "null",
			//		worldData != null && worldData.hasKey("menhirSpawnLocations"),
			//		isFirst);

			return isFirst;
		} catch (Exception e) {
			Menhir.logger.error("Failed to check first world gen: {}", e.getMessage());
			e.printStackTrace();
			return true; // Assume first gen on error
		}
	}

	/**
	 * Pre-calculates spawn locations for all menhir stones in the world
	 */
	private void preCalculateAllMenhirLocations(World world) {
		try {
			long worldSeed = world.getSeed();
			java.util.List<String> worldMenhirs = getMenhirsForWorld(worldSeed);

			Menhir.logger.info("Pre-calculating spawn locations for {} birthsigns with world seed: {}", worldMenhirs.size(), worldSeed);

			if (worldMenhirs.isEmpty()) {
				Menhir.logger.warn("No birthsigns available for world generation");
				return;
			}

			java.util.List<MenhirSpawnLocation> allLocations = new java.util.ArrayList<>();

			// Calculate positions for all birthsigns in a circle around spawn
			//Menhir.logger.info("Calculating circular positions for {} birthsigns", worldMenhirs.size());
			for (int i = 0; i < worldMenhirs.size(); i++) {
				BlockPos pos = calculateCircularPosition(world, i, worldSeed);
				if (pos != null) {
					//	birthsigns.logger.info("Circular location {}: {} at chunk ({}, {})", i, worldbirthsigns.get(i), pos.getX() >> 4, pos.getZ() >> 4);
					MenhirSpawnLocation location = new MenhirSpawnLocation(pos, worldMenhirs.get(i));
					allLocations.add(location);
				}
			}

			//Menhir.logger.info("Calculated {} total locations, saving to world data...", allLocations.size());

			// Save all pre-calculated locations
			saveMenhirSpawnLocations(world, allLocations);

			Menhir.logger.info("Successfully pre-calculated and saved {} menhir spawn locations for world seed: {}", allLocations.size(), worldSeed);

		} catch (Exception e) {
			Menhir.logger.error("Failed to pre-calculate birthsign locations: {}", e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Calculates a position in a circle around spawn at the configured chunk distance
	 */
	private BlockPos calculateCircularPosition(World world, int index, long worldSeed) {
		Random rand = new Random(worldSeed + index * 31);
		BlockPos spawnPos = world.getSpawnPoint();

		// Convert spawn to chunk coordinates
		int spawnChunkX = spawnPos.getX() >> 4;
		int spawnChunkZ = spawnPos.getZ() >> 4;

		// Calculate angle for this birthsign (evenly distributed around the circle)
		double angle = (2 * Math.PI * index) / MAX_BIRTHSIGNS_PER_WORLD;

		// Add some randomness to the distance (±2 chunks)
		int distanceVariation = rand.nextInt(5) - 2; // -2 to +2 chunks
		int actualDistance = MENHIR_STONE_SPAWN_DISTANCE_CHUNKS + distanceVariation;

		// Calculate target chunk coordinates using trigonometry
		int targetChunkX = spawnChunkX + (int) (Math.cos(angle) * actualDistance);
		int targetChunkZ = spawnChunkZ + (int) (Math.sin(angle) * actualDistance);

		// Convert back to block coordinates (center of chunk)
		int blockX = targetChunkX * 16 + 8;
		int blockZ = targetChunkZ * 16 + 8;

		//Menhir.logger.debug("Circular position {}: angle {:.2f}, distance {}, chunk ({}, {}) -> block ({}, {})",
		//	index, Math.toDegrees(angle), actualDistance, targetChunkX, targetChunkZ, blockX, blockZ);
		return new BlockPos(blockX, 0, blockZ); // Y will be determined during actual placement
	}

	/**
	 * Gets all pre-calculated spawn locations that fall within the specified chunk
	 */
	private java.util.List<MenhirSpawnLocation> getLocationsInChunk(World world, int chunkX, int chunkZ) {
		try {
			java.util.List<MenhirSpawnLocation> allLocations = loadMenhirSpawnLocations(world);
			java.util.List<MenhirSpawnLocation> locationsInChunk = new java.util.ArrayList<>();

			int chunkStartX = chunkX * 16;
			int chunkEndX = chunkStartX + 15;
			int chunkStartZ = chunkZ * 16;
			int chunkEndZ = chunkStartZ + 15;

			//Menhir.info("Checking chunk [{}, {}] (X: {} to {}, Z: {} to {}) for {} total locations",
			//		chunkX, chunkZ, chunkStartX, chunkEndX, chunkStartZ, chunkEndZ, allLocations.size());

			for (MenhirSpawnLocation location : allLocations) {
				if (location.position.getX() >= chunkStartX && location.position.getX() <= chunkEndX &&
						location.position.getZ() >= chunkStartZ && location.position.getZ() <= chunkEndZ) {
					locationsInChunk.add(location);
				}
			}

			//Menhir.info("Found {} locations in chunk [{}, {}]", locationsInChunk.size(), chunkX, chunkZ);
			return locationsInChunk;
		} catch (Exception e) {
			//Menhir.error("Failed to get locations in chunk: {}", e.getMessage());
			return new java.util.ArrayList<>();
		}
	}

	/**
	 * Checks if a menhir has already been placed
	 */
	private boolean isBirthsignPlaced(World world, String birthsignName) {
		try {
			net.minecraft.nbt.NBTTagCompound worldData = world.getWorldInfo().getDimensionData(world.provider.getDimension());
			if (worldData == null || !worldData.hasKey(WorldGenMenhirStone.PLACED_MENHIRS)) {
				return false;
			}

			net.minecraft.nbt.NBTTagList placedList = worldData.getTagList(PLACED_MENHIRS, net.minecraftforge.common.util.Constants.NBT.TAG_STRING);
			for (int i = 0; i < placedList.tagCount(); i++) {
				if (birthsignName.equals(placedList.getStringTagAt(i))) {
					return true;
				}
			}

			return false;
		} catch (Exception e) {
			//Menhir.error("Failed to check if menhir is placed: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Marks a menhir as placed
	 */
	private void markBirthsignAsPlaced(World world, String birthsignName) {
		try {
			net.minecraft.nbt.NBTTagCompound worldData = world.getWorldInfo().getDimensionData(world.provider.getDimension());
			if (worldData == null) {
				worldData = new net.minecraft.nbt.NBTTagCompound();
			}

			net.minecraft.nbt.NBTTagList placedList;
			if (worldData.hasKey(PLACED_MENHIRS)) {
				placedList = worldData.getTagList(PLACED_MENHIRS, net.minecraftforge.common.util.Constants.NBT.TAG_STRING);
			} else {
				placedList = new net.minecraft.nbt.NBTTagList();
			}

			placedList.appendTag(new net.minecraft.nbt.NBTTagString(birthsignName));
			worldData.setTag(PLACED_MENHIRS, placedList);
			world.getWorldInfo().setDimensionData(world.provider.getDimension(), worldData);

		} catch (Exception e) {
			//Menhir.error("Failed to mark menhir as placed: {}", e.getMessage());
		}
	}

	/**
	 * Places a menhir stone at the pre-calculated location
	 */
	private boolean placeMenhirStoneAtLocation(World world, MenhirSpawnLocation location) {
		// First try the exact pre-calculated location
		BlockPos actualPos = findSuitablePosition(world, location.position.getX(), location.position.getZ());
		if (actualPos != null) {
			// Place the stone
			return placeMenhirStone(world, actualPos, location.birthsignName);
		}

		// If that fails, try neighboring chunks
		//Menhir.warn("Could not find suitable position for menhir '{}' at {}, {}, trying neighboring chunks",
		//		location.birthsignName, location.position.getX(), location.position.getZ());

		return tryPlaceInNeighboringChunks(world, location);
	}

	/**
	 * Tries to place the menhir stone in neighboring chunks if the target location fails
	 */
	private boolean tryPlaceInNeighboringChunks(World world, MenhirSpawnLocation location) {
		// Get the target chunk coordinates
		int targetChunkX = location.position.getX() >> 4;
		int targetChunkZ = location.position.getZ() >> 4;

		// Try neighboring chunks in a spiral pattern
		int[] offsets = {1, -1, 2, -2, 3, -3}; // Try chunks at increasing distances

		for (int offset : offsets) {
			// Try all 8 neighboring directions
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0) continue; // Skip the center chunk

					int neighborChunkX = targetChunkX + (dx * offset);
					int neighborChunkZ = targetChunkZ + (dz * offset);

					// Convert to block coordinates (center of chunk)
					int blockX = neighborChunkX * 16 + 8;
					int blockZ = neighborChunkZ * 16 + 8;

					//Menhir.debug("Trying neighboring chunk ({}, {}) at block ({}, {}) for menhir '{}'",
					//		neighborChunkX, neighborChunkZ, blockX, blockZ, location.birthsignName);

					BlockPos actualPos = findSuitablePosition(world, blockX, blockZ);
					if (actualPos != null) {
						//Menhir.info("Found suitable position in neighboring chunk ({}, {}) for menhir '{}'",
						//		neighborChunkX, neighborChunkZ, location.birthsignName);

						// Update the location to the new position
						location.position = actualPos;

						// Place the stone
						return placeMenhirStone(world, actualPos, location.birthsignName);
					}
				}
			}
		}

		//Menhir.error("Could not find suitable position for menhir '{}' in target chunk or any neighboring chunks", location.birthsignName);
		return false;
	}

	/**
	 * Saves all pre-calculated menhir spawn locations to world data
	 */
	private void saveMenhirSpawnLocations(World world, java.util.List<MenhirSpawnLocation> locations) {
		try {
			//Menhir.info("Saving {} spawn locations to world data...", locations.size());

			net.minecraft.nbt.NBTTagCompound worldData = world.getWorldInfo().getDimensionData(world.provider.getDimension());
			if (worldData == null) {
				//Menhir.info("Creating new world data for dimension {}", world.provider.getDimension());
				worldData = new net.minecraft.nbt.NBTTagCompound();
			}

			net.minecraft.nbt.NBTTagList locationList = new net.minecraft.nbt.NBTTagList();
			for (MenhirSpawnLocation location : locations) {
				net.minecraft.nbt.NBTTagCompound locData = new net.minecraft.nbt.NBTTagCompound();
				locData.setInteger("x", location.position.getX());
				locData.setInteger("y", location.position.getY());
				locData.setInteger("z", location.position.getZ());
				locData.setString("menhir", location.birthsignName);
				locationList.appendTag(locData);

				//Menhir.debug("Saving location: {} at ({}, {}, {})", location.birthsignName, location.position.getX(), location.position.getY(), location.position.getZ());
			}

			worldData.setTag(MENHIR_SPAWN_LOCATIONS, locationList);
			world.getWorldInfo().setDimensionData(world.provider.getDimension(), worldData);

			//Menhir.info("Successfully saved {} spawn locations to world data", locations.size());

		} catch (Exception e) {
			//Menhir.error("Failed to save menhir spawn locations: {}", e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Loads all pre-calculated menhir spawn locations from world data
	 */
	private java.util.List<MenhirSpawnLocation> loadMenhirSpawnLocations(World world) {
		try {
			net.minecraft.nbt.NBTTagCompound worldData = world.getWorldInfo().getDimensionData(world.provider.getDimension());
			if (worldData == null || !worldData.hasKey(MENHIR_SPAWN_LOCATIONS)) {
				//Menhir.warn("No world data or menhirSpawnLocations key found for dimension {}", world.provider.getDimension());
				return new java.util.ArrayList<>();
			}

			net.minecraft.nbt.NBTTagList locationList = worldData.getTagList(MENHIR_SPAWN_LOCATIONS, net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND);
			java.util.List<MenhirSpawnLocation> locations = new java.util.ArrayList<>();

			//Menhir.info("Loading {} spawn locations from world data", locationList.tagCount());

			for (int i = 0; i < locationList.tagCount(); i++) {
				net.minecraft.nbt.NBTTagCompound locData = locationList.getCompoundTagAt(i);
				int x = locData.getInteger("x");
				int y = locData.getInteger("y");
				int z = locData.getInteger("z");
				String birthsignName = locData.getString("menhir");

				//Menhir.info("Loaded location {}: {} at ({}, {}, {})", i, birthsignName, x, y, z);
				locations.add(new MenhirSpawnLocation(new BlockPos(x, y, z), birthsignName));
			}

			//Menhir.info("Successfully loaded {} spawn locations", locations.size());
			return locations;
		} catch (Exception e) {
			//Menhir.error("Failed to load menhir spawn locations: {}", e.getMessage());
			e.printStackTrace();
			return new java.util.ArrayList<>();
		}
	}

	private BlockPos findSuitablePosition(World world, int x, int z) {
		for (int y = world.getHeight() - 1; y > 0; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			IBlockState state = world.getBlockState(pos);

			if (state.isFullBlock() && state.isFullCube() && state.getBlock() != Blocks.BEDROCK) {
				BlockPos above = pos.up();
				if (world.isAirBlock(above) && world.isAirBlock(above.up()) && world.isAirBlock(above.up(2))) {
					return above;
				}
			}
		}

		return null;
	}

	private boolean placeMenhirStone(World world, BlockPos pos, String birthsignName) {
		try {
			if (!world.setBlockState(pos, BirthsignRegistrationHandler.MENHIR_STONE.getDefaultState()
					.withProperty(BlockMenhirStone.POSITION, BlockMenhirStone.BlockPosition.BOTTOM))) {
				return false;
			}

			if (!world.setBlockState(pos.up(), BirthsignRegistrationHandler.MENHIR_STONE.getDefaultState()
					.withProperty(BlockMenhirStone.POSITION, BlockMenhirStone.BlockPosition.MIDDLE))) {
				return false;
			}

			if (!world.setBlockState(pos.up(2), BirthsignRegistrationHandler.MENHIR_STONE.getDefaultState()
					.withProperty(BlockMenhirStone.POSITION, BlockMenhirStone.BlockPosition.TOP))) {
				return false;
			}

			// Set the birthsign on ALL tile entities so tooltips work from any part
			boolean success = false;

			// Set on BOTTOM tile entity (main tile entity)
			if (world.getTileEntity(pos) instanceof TileEntityMenhirStone) {
				TileEntityMenhirStone bottomTileEntity = (TileEntityMenhirStone) world.getTileEntity(pos);
				bottomTileEntity.setBirthsign(birthsignName);
				bottomTileEntity.markDirty();
				success = true;
			}

			// Set on MIDDLE tile entity
			if (world.getTileEntity(pos.up()) instanceof TileEntityMenhirStone) {
				TileEntityMenhirStone middleTileEntity = (TileEntityMenhirStone) world.getTileEntity(pos.up());
				middleTileEntity.setBirthsign(birthsignName);
				middleTileEntity.markDirty();
			}

			// Set on TOP tile entity
			if (world.getTileEntity(pos.up(2)) instanceof TileEntityMenhirStone) {
				TileEntityMenhirStone topTileEntity = (TileEntityMenhirStone) world.getTileEntity(pos.up(2));
				topTileEntity.setBirthsign(birthsignName);
				topTileEntity.markDirty();
			}

			// Add Antique Atlas marker if integration is enabled
			if (success) {
				try {
					MenhirAntiqueAtlasIntegration.markMenhirStone(world, pos.getX(), pos.getZ(), Menhir.proxy.translate(birthsignName));
				} catch (Exception e) {
					Menhir.logger.warn("Failed to add Antique Atlas marker for menhir stone: {}", e.getMessage());
				}
			}

			return success;

		} catch (Exception e) {
			Menhir.logger.error("Error placing menhir stone at {}: {}", pos, e.getMessage());
		}

		return false;
	}

	/**
	 * Inner class to hold menhir spawn location data
	 */
	public static class MenhirSpawnLocation {
		public final String birthsignName;
		public BlockPos position;

		public MenhirSpawnLocation(BlockPos position, String birthsignName) {
			this.position = position;
			this.birthsignName = birthsignName;
		}

		@Override
		public String toString() {
			return "menhirSpawnLocation{position=" + position + ", menhir='" + birthsignName + "'}";
		}
	}
}
