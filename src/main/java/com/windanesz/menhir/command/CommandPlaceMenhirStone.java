package com.windanesz.menhir.command;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.block.BlockMenhirStone;
import com.windanesz.menhir.core.BirthsignRegistrationHandler;
import com.windanesz.menhir.integration.antiqueatlas.MenhirAntiqueAtlasIntegration;
import com.windanesz.menhir.tileentity.TileEntityMenhirStone;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandPlaceMenhirStone extends CommandBase {

	@Override
	public String getName() {
		return "placemenhirstone";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/placemenhirstone <birthsign_name>";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2; // Requires OP level 2
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return false;
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		if (args.length == 1) {
			List<String> completions = new ArrayList<>();

			// Get all registered menhirs from the registry
			if (Birthsign.registry != null) {
				for (ResourceLocation key : Birthsign.registry.getKeys()) {
					completions.add(key.toString()); // This will include modid:name format
				}
			}

			// Filter based on what the user has typed so far
			String partial = args[0].toLowerCase();
			return completions.stream()
					.filter(name -> name.toLowerCase().startsWith(partial))
					.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (!(sender instanceof EntityPlayer)) {
			throw new CommandException("This command can only be used by players");
		}

		if (args.length != 1) {
			throw new CommandException("Usage: " + getUsage(sender));
		}

		EntityPlayer player = (EntityPlayer) sender;
		String birthsignName = args[0];
		World world = player.world;
		BlockPos playerPos = player.getPosition();

		// Check if the birthsign name is valid
		if (birthsignName == null || birthsignName.trim().isEmpty()) {
			throw new CommandException("Birthsign name cannot be empty");
		}

		// Check if the world is valid
		if (world == null) {
			throw new CommandException("Invalid world");
		}

		// Check if the birthsign exists
		if (!isValidBirthsign(birthsignName)) {
			throw new CommandException("Invalid birthsign name: " + birthsignName);
		}

		// Check if the area is clear for all three blocks
		BlockPos bottomPos = playerPos;
		BlockPos middlePos = playerPos.up();
		BlockPos topPos = playerPos.up(2);

		// Check if the positions are valid (world.isValid doesn't exist in 1.12.2)
		if (bottomPos.getY() < 0 || bottomPos.getY() > 255 || middlePos.getY() < 0 || middlePos.getY() > 255 || topPos.getY() < 0 || topPos.getY() > 255) {
			throw new CommandException("Invalid position for placing blocks");
		}

		if (!canPlaceBlock(world, bottomPos) || !canPlaceBlock(world, middlePos) || !canPlaceBlock(world, topPos)) {
			throw new CommandException("Cannot place birthsign stone here - area is not clear");
		}

		try {
			// Place the bottom block
			boolean bottomPlaced = world.setBlockState(bottomPos, BirthsignRegistrationHandler.MENHIR_STONE.getDefaultState()
					.withProperty(BlockMenhirStone.FACING, player.getHorizontalFacing().getOpposite())
					.withProperty(BlockMenhirStone.POSITION, BlockMenhirStone.BlockPosition.BOTTOM));

			// Place the middle block
			boolean middlePlaced = world.setBlockState(middlePos, BirthsignRegistrationHandler.MENHIR_STONE.getDefaultState()
					.withProperty(BlockMenhirStone.FACING, player.getHorizontalFacing().getOpposite())
					.withProperty(BlockMenhirStone.POSITION, BlockMenhirStone.BlockPosition.MIDDLE));

			// Place the top block
			boolean topPlaced = world.setBlockState(topPos, BirthsignRegistrationHandler.MENHIR_STONE.getDefaultState()
					.withProperty(BlockMenhirStone.FACING, player.getHorizontalFacing().getOpposite())
					.withProperty(BlockMenhirStone.POSITION, BlockMenhirStone.BlockPosition.TOP));

			// Check if all three blocks were placed successfully
			if (!bottomPlaced || !middlePlaced || !topPlaced) {
				// Clean up any partially placed blocks
				world.setBlockState(bottomPos, Blocks.AIR.getDefaultState());
				world.setBlockState(middlePos, Blocks.AIR.getDefaultState());
				world.setBlockState(topPos, Blocks.AIR.getDefaultState());
				throw new CommandException("Failed to place one or more blocks");
			}

			// Verify the blocks were actually placed
			if (world.getBlockState(bottomPos).getBlock() != BirthsignRegistrationHandler.MENHIR_STONE ||
					world.getBlockState(middlePos).getBlock() != BirthsignRegistrationHandler.MENHIR_STONE ||
					world.getBlockState(topPos).getBlock() != BirthsignRegistrationHandler.MENHIR_STONE) {
				// Clean up any partially placed blocks
				world.setBlockState(bottomPos, Blocks.AIR.getDefaultState());
				world.setBlockState(middlePos, Blocks.AIR.getDefaultState());
				world.setBlockState(topPos, Blocks.AIR.getDefaultState());
				throw new CommandException("Block verification failed - blocks were not placed correctly");
			}

			// Set the birthsign name in all three tile entities
			TileEntityMenhirStone bottomTileEntity = (TileEntityMenhirStone) world.getTileEntity(bottomPos);
			TileEntityMenhirStone middleTileEntity = (TileEntityMenhirStone) world.getTileEntity(middlePos);
			TileEntityMenhirStone topTileEntity = (TileEntityMenhirStone) world.getTileEntity(topPos);

			// System.out.println("CommandPlaceMenhirStone: Bottom tile entity = " + bottomTileEntity);
			// System.out.println("CommandPlaceMenhirStone: Middle tile entity = " + middleTileEntity);
			// System.out.println("CommandPlaceMenhirStone: Top tile entity = " + topTileEntity);

			if (bottomTileEntity != null && middleTileEntity != null && topTileEntity != null) {
				// System.out.println("CommandPlaceMenhirStone: Setting birthsign '" + birthsignName + "' in all tile entities");
				bottomTileEntity.setBirthsign(birthsignName);
				middleTileEntity.setBirthsign(birthsignName);
				topTileEntity.setBirthsign(birthsignName);

				bottomTileEntity.markDirty();
				middleTileEntity.markDirty();
				topTileEntity.markDirty();

				// System.out.println("CommandPlaceMenhirStone: Bottom birthsign = '" + bottomTileEntity.getBirthsign() + "'");
				// System.out.println("CommandPlaceMenhirStone: Middle birthsign = '" + middleTileEntity.getBirthsign() + "'");
				// System.out.println("CommandPlaceMenhirStone: Top birthsign = '" + topTileEntity.getBirthsign() + "'");

				// Force a block update to trigger Minecraft's built-in tile entity syncing
				world.notifyBlockUpdate(bottomPos, world.getBlockState(bottomPos), world.getBlockState(bottomPos), 3);
				world.notifyBlockUpdate(middlePos, world.getBlockState(middlePos), world.getBlockState(middlePos), 3);
				world.notifyBlockUpdate(topPos, world.getBlockState(topPos), world.getBlockState(topPos), 3);
			} else {
				// If tile entities don't exist, remove the blocks
				world.setBlockState(bottomPos, Blocks.AIR.getDefaultState());
				world.setBlockState(middlePos, Blocks.AIR.getDefaultState());
				world.setBlockState(topPos, Blocks.AIR.getDefaultState());
				throw new CommandException("Failed to get tile entities for birthsign stone - they should be created automatically");
			}

			// Send success message
			// Get the localized birthsign name for display
			String birthsignNameForTranslation = birthsignName;
			if (birthsignName.contains(":")) {
				birthsignNameForTranslation = birthsignName.split(":")[1];
			}
			String localizedbirthsignName = Menhir.proxy.translate("birthsign." + birthsignNameForTranslation + ".name");

			// Play a sound effect
			world.playSound(null, bottomPos, net.minecraft.init.SoundEvents.BLOCK_ANVIL_LAND,
					net.minecraft.util.SoundCategory.BLOCKS, 0.5F, 1.0F);

			// Add Antique Atlas marker if integration is enabled
			try {
				MenhirAntiqueAtlasIntegration.markMenhirStone(world, bottomPos.getX(), bottomPos.getZ(), birthsignName);
			} catch (Exception e) {
				// Log warning but don't fail the command
				// System.out.println("Warning: Failed to add Antique Atlas marker: " + e.getMessage());
			}

		} catch (Exception e) {
			// Clean up any partially placed blocks
			world.setBlockState(bottomPos, Blocks.AIR.getDefaultState());
			world.setBlockState(middlePos, Blocks.AIR.getDefaultState());
			world.setBlockState(topPos, Blocks.AIR.getDefaultState());
			throw new CommandException("Failed to place menhir stone: " + e.getMessage());
		}
	}

	private boolean isValidBirthsign(String birthsignName) {
		if (birthsignName == null || birthsignName.trim().isEmpty()) {
			return false;
		}

		// Must be in modid:name format
		if (!birthsignName.contains(":")) {
			return false;
		}

		// Parse as a ResourceLocation and check if it exists in the registry
		try {
			ResourceLocation resourceLocation = new ResourceLocation(birthsignName);
			if (Birthsign.registry != null && Birthsign.registry.containsKey(resourceLocation)) {
				return true;
			}
		} catch (Exception e) {
			// Invalid ResourceLocation format
			return false;
		}

		return false;
	}


	private boolean canPlaceBlock(World world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() == Blocks.AIR || world.getBlockState(pos).getBlock().isReplaceable(world, pos);
	}
}
