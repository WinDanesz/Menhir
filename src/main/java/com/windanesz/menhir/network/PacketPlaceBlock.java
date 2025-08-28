package com.windanesz.menhir.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPlaceBlock implements IMessage {
	private String blockToPlace;
	private BlockPos targetPos;
	private EnumFacing side;

	public PacketPlaceBlock() {
	}

	public PacketPlaceBlock(String blockToPlace, BlockPos targetPos, EnumFacing side) {
		this.blockToPlace = blockToPlace;
		this.targetPos = targetPos;
		this.side = side;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		int length = buf.readInt();
		byte[] bytes = new byte[length];
		buf.readBytes(bytes);
		this.blockToPlace = new String(bytes);

		this.targetPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
		this.side = EnumFacing.values()[buf.readInt()];
	}

	@Override
	public void toBytes(ByteBuf buf) {
		byte[] bytes = blockToPlace.getBytes();
		buf.writeInt(bytes.length);
		buf.writeBytes(bytes);

		buf.writeInt(targetPos.getX());
		buf.writeInt(targetPos.getY());
		buf.writeInt(targetPos.getZ());
		buf.writeInt(side.ordinal());
	}

	public static class Handler implements IMessageHandler<PacketPlaceBlock, IMessage> {
		@Override
		public IMessage onMessage(PacketPlaceBlock message, MessageContext ctx) {
			MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
			EntityPlayerMP player = ctx.getServerHandler().player;

			server.addScheduledTask(() -> {
				handleBlockPlacement(player, message.blockToPlace, message.targetPos, message.side);
			});

			return null;
		}

		private void handleBlockPlacement(EntityPlayerMP player, String blockToPlace, BlockPos targetPos, EnumFacing side) {
			if (player == null || player.world == null) {
				return;
			}

			World world = player.world;

			// Calculate the position where the block should be placed
			BlockPos placementPos = targetPos.offset(side);

			// Check if the block can be placed at the target position
			if (!world.isAirBlock(placementPos)) {
				return;
			}

			// Check if the target block can support the block to be placed
			if (!canSupportBlock(world, targetPos, side)) {
				return;
			}

			// Place the block
			boolean success = placeBlock(world, placementPos, side, blockToPlace);

			if (success) {
				// Play block placement sound
				world.playSound(null, placementPos, net.minecraft.init.SoundEvents.BLOCK_STONE_PLACE,
						SoundCategory.BLOCKS, 1.0f, 1.0f);

				// Spawn block placement particles
				spawnPlacementParticles(world, placementPos);
			}
		}

		/**
		 * Checks if a block can support the block to be placed.
		 */
		private boolean canSupportBlock(World world, BlockPos pos, EnumFacing side) {
			net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
			net.minecraft.block.Block block = state.getBlock();

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
		 * Places the configured block at the specified position with proper facing.
		 */
		private boolean placeBlock(World world, BlockPos pos, EnumFacing side, String blockToPlace) {
			// Get the block to place from the registry
			Block block = Block.REGISTRY.getObject(new ResourceLocation(blockToPlace));
			if (block == null) {
				return false;
			}

			// Get the default state for the block
			IBlockState blockState = block.getDefaultState();

			// Handle different block types with proper facing
			if (block == Blocks.TORCH) {
				if (side == EnumFacing.UP) {
					// Standing torch on top surface
					blockState = Blocks.TORCH.getDefaultState();
				} else {
					// Wall torch on side surface - use regular torch for now
					// In Minecraft 1.12.2, wall torches might not be available
					blockState = Blocks.TORCH.getDefaultState();
				}
			} else if (block == Blocks.LADDER) {
				// Ladder with proper facing
				try {
					blockState = Blocks.LADDER.getDefaultState()
							.withProperty(net.minecraft.block.BlockLadder.FACING, side);
				} catch (Exception e) {
					// Fallback to default state if property doesn't exist
					blockState = Blocks.LADDER.getDefaultState();
				}
			} else if (block == Blocks.VINE) {
				// Vines with proper facing
				try {
					blockState = Blocks.VINE.getDefaultState()
							.withProperty(net.minecraft.block.BlockVine.NORTH, side == EnumFacing.NORTH)
							.withProperty(net.minecraft.block.BlockVine.SOUTH, side == EnumFacing.SOUTH)
							.withProperty(net.minecraft.block.BlockVine.EAST, side == EnumFacing.EAST)
							.withProperty(net.minecraft.block.BlockVine.WEST, side == EnumFacing.WEST);
				} catch (Exception e) {
					// Fallback to default state if properties don't exist
					blockState = Blocks.VINE.getDefaultState();
				}
			} else {
				// For other blocks, use default state
				blockState = block.getDefaultState();
			}

			return world.setBlockState(pos, blockState);
		}

		/**
		 * Spawns particles at the block placement location.
		 */
		private void spawnPlacementParticles(World world, BlockPos pos) {
			// Spawn block placement particles
			world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
					pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					0.0, 0.1, 0.0);
		}
	}
}
