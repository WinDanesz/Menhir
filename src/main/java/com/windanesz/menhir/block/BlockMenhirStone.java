package com.windanesz.menhir.block;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.Settings;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.eventhandler.BirthsignEffectManager;
import com.windanesz.menhir.tileentity.TileEntityMenhirStone;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class BlockMenhirStone extends Block {

	public static final PropertyDirection FACING = BlockHorizontal.FACING;
	public static final PropertyEnum<BlockPosition> POSITION = PropertyEnum.create("position", BlockPosition.class);
	public static final PropertyBool WORLDGEN = PropertyBool.create("worldgen");
	public BlockMenhirStone() {
		super(Material.ROCK);
		this.setDefaultState(this.blockState.getBaseState()
				.withProperty(FACING, EnumFacing.NORTH)
				.withProperty(POSITION, BlockPosition.BOTTOM));

		// Make the block unbreakable
		this.setBlockUnbreakable();
		this.setResistance(6000000.0F); // Very high blast resistance
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullBlock(IBlockState state) {
		return false;
	}

	@Override
	public net.minecraft.util.math.AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		BlockPosition position = state.getValue(POSITION);

		switch (position) {
			case BOTTOM:
				// Bottom block: wider base, narrower top
				return new net.minecraft.util.math.AxisAlignedBB(0.0625, 0.0, 0.1875, 0.9375, 1.0, 0.8125);
			case MIDDLE:
				// Middle block: narrower than bottom
				return new net.minecraft.util.math.AxisAlignedBB(0.1875, 0.0, 0.3125, 0.8125, 1.0, 0.6875);
			case TOP:
				// Top block: narrowest, with decorative elements
				return new net.minecraft.util.math.AxisAlignedBB(0.1875, 0.0, 0.3125, 0.8125, 1.0, 0.6875);
			default:
				return FULL_BLOCK_AABB;
		}
	}

	@Override
	public net.minecraft.util.math.AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
		// Use the same bounding box for collision as for rendering
		return getBoundingBox(blockState, worldIn, pos);
	}

	@Override
	public net.minecraft.util.math.AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos) {
		// Use the same bounding box for selection as for rendering
		return getBoundingBox(state, worldIn, pos);
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileEntityMenhirStone();
	}

	public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
		return BlockFaceShape.UNDEFINED;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		return this.getDefaultState()
				.withProperty(FACING, placer.getHorizontalFacing().getOpposite())
				.withProperty(POSITION, BlockPosition.BOTTOM);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		// Ensure we only get valid horizontal facing values (0-3)
		int facingIndex = meta & 3;
		EnumFacing facing = EnumFacing.HORIZONTALS[facingIndex];

		// Get position from bits 2-3
		int positionIndex = (meta >> 2) & 3;
		BlockPosition position = BlockPosition.values()[positionIndex];

		return this.getDefaultState()
				.withProperty(FACING, facing)
				.withProperty(POSITION, position);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		int meta = state.getValue(FACING).getIndex() & 3; // Ensure only 2 bits
		meta |= (state.getValue(POSITION).ordinal() << 2); // 2 bits for position
		// Note: worldgen property cannot be stored in metadata due to 4-bit limit
		return meta;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[]{FACING, POSITION});
	}

	@Override
	public IBlockState withRotation(IBlockState state, net.minecraft.util.Rotation rot) {
		return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, net.minecraft.util.Mirror mirrorIn) {
		return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
	}

	/**
	 * Checks if the multiblock structure is complete at the given position
	 */
	private boolean isMultiblockComplete(World world, BlockPos pos) {
		// Check if there are birthsign stone blocks above and below this one
		// For a complete multiblock, we need: BOTTOM, MIDDLE, TOP
		IBlockState currentState = world.getBlockState(pos);
		if (currentState.getBlock() != this) return false;

		BlockPosition currentPos = currentState.getValue(POSITION);

		switch (currentPos) {
			case BOTTOM:
				// Check if there's a middle block above
				IBlockState aboveState = world.getBlockState(pos.up());
				return aboveState.getBlock() == this && aboveState.getValue(POSITION) == BlockPosition.MIDDLE;
			case MIDDLE:
				// Check if there are blocks above and below
				IBlockState belowState = world.getBlockState(pos.down());
				IBlockState aboveState2 = world.getBlockState(pos.up());
				return belowState.getBlock() == this && belowState.getValue(POSITION) == BlockPosition.BOTTOM &&
						aboveState2.getBlock() == this && aboveState2.getValue(POSITION) == BlockPosition.TOP;
			case TOP:
				// Check if there's a middle block below
				IBlockState belowState2 = world.getBlockState(pos.down());
				return belowState2.getBlock() == this && belowState2.getValue(POSITION) == BlockPosition.MIDDLE;
			default:
				return false;
		}
	}

	/**
	 * Gets the main tile entity for the multiblock (always the bottom part)
	 */
	private TileEntityMenhirStone getMainTileEntity(World world, BlockPos pos) {
		IBlockState currentState = world.getBlockState(pos);
		if (currentState.getBlock() != this) return null;

		BlockPosition currentPos = currentState.getValue(POSITION);

		switch (currentPos) {
			case TOP:
				// Get tile entity from the bottom (two blocks down)
				TileEntity te = world.getTileEntity(pos.down(2));
				return te instanceof TileEntityMenhirStone ? (TileEntityMenhirStone) te : null;
			case MIDDLE:
				// Get tile entity from the bottom (one block down)
				TileEntity te2 = world.getTileEntity(pos.down());
				return te2 instanceof TileEntityMenhirStone ? (TileEntityMenhirStone) te2 : null;
			case BOTTOM:
				// This is the bottom part, get tile entity from this position
				TileEntity te3 = world.getTileEntity(pos);
				return te3 instanceof TileEntityMenhirStone ? (TileEntityMenhirStone) te3 : null;
			default:
				return null;
		}
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!worldIn.isRemote) {
			// Check if the multiblock is complete
			if (!isMultiblockComplete(worldIn, pos)) {
				playerIn.sendMessage(new TextComponentString("§cThe menhir stone multiblock is incomplete. Both parts must be placed."));
				return true;
			}

			// Get the main tile entity (always from the bottom part)
			TileEntityMenhirStone birthsign = getMainTileEntity(worldIn, pos);
			if (birthsign != null) {
				String birthsignName = birthsign.getBirthsign();
				if (birthsignName != null && !birthsignName.isEmpty()) {
					IBirthsignData birthsignData = BirthsignDataProvider.get(playerIn);
					if (birthsignData != null) {
						String currentBirthsign = birthsignData.getBirthsign();
						if (currentBirthsign != null && !currentBirthsign.isEmpty()) {
							if (currentBirthsign.equals(birthsignName)) {
								// Extract the birthsign name without modid prefix for translation
								String birthsignNameForTranslation = birthsignName;
								if (birthsignName.contains(":")) {
									birthsignNameForTranslation = birthsignName.split(":")[1];
								}
								String localizedbirthsignName = Menhir.proxy.translate("birthsign." + birthsignNameForTranslation + ".name");
								playerIn.sendMessage(new TextComponentString("§eYou already have the " + localizedbirthsignName + " birthsign."));
							} else {
							// Check if birthsign stones can override existing birthsigns
							if (Settings.generalSettings.menhir_stones_can_override_existing_birthsigns) {
								// Allow override
								birthsignData.setBirthsign(birthsignName);
								
								// Sync to client with full capability data
								if (playerIn instanceof EntityPlayerMP) {
									net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
									birthsignData.writeToNBT(nbt);
									com.windanesz.menhir.network.NetworkHandler.INSTANCE.sendTo(
										new com.windanesz.menhir.network.PacketSyncBirthsignData(birthsignName, nbt), 
										(EntityPlayerMP) playerIn
									);
								}
								
								BirthsignEffectManager.reapplyBirthsignEffects(playerIn, currentBirthsign, birthsignName);

								// Extract birthsign names without modid prefix for translation
									String currentBirthsign1 = currentBirthsign;
									if (currentBirthsign.contains(":")) {
										currentBirthsign1 = currentBirthsign.split(":")[1];
									}
									String birthsignNameForTranslation = birthsignName;
									if (birthsignName.contains(":")) {
										birthsignNameForTranslation = birthsignName.split(":")[1];
									}

									String localizedCurrentBirthsign = Menhir.proxy.translate("birthsign." + currentBirthsign1 + ".name");
									String localizedNewBirthsign = Menhir.proxy.translate("birthsign." + birthsignNameForTranslation + ".name");
									playerIn.sendMessage(new TextComponentString("§aBirthsign changed from " + localizedCurrentBirthsign + " to " + localizedNewBirthsign + "!"));

									// Play a sound effect
									worldIn.playSound(null, pos, net.minecraft.init.SoundEvents.BLOCK_ANVIL_LAND,
											net.minecraft.util.SoundCategory.BLOCKS, 0.5F, 1.0F);
								} else {
									// Prevent override
									// Extract birthsign names without modid prefix for translation
									String currentBirthsign1 = currentBirthsign;
									if (currentBirthsign.contains(":")) {
										currentBirthsign1 = currentBirthsign.split(":")[1];
									}
									String birthsignNameForTranslation = birthsignName;
									if (birthsignName.contains(":")) {
										birthsignNameForTranslation = birthsignName.split(":")[1];
									}

									String localizedCurrentBirthsign = Menhir.proxy.translate("birthsign." + currentBirthsign1 + ".name");
									String localizedNewBirthsign = Menhir.proxy.translate("birthsign." + birthsignNameForTranslation + ".name");
									playerIn.sendMessage(new TextComponentString("§cYou already have the " + localizedCurrentBirthsign + " birthsign. Cannot change to " + localizedNewBirthsign + "."));
								}
							}
					} else {
						// Player has no birthsign, assign this one
						birthsignData.setBirthsign(birthsignName);
						
						// Sync to client with full capability data
						if (playerIn instanceof EntityPlayerMP) {
							net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
							birthsignData.writeToNBT(nbt);
							com.windanesz.menhir.network.NetworkHandler.INSTANCE.sendTo(
								new com.windanesz.menhir.network.PacketSyncBirthsignData(birthsignName, nbt), 
								(EntityPlayerMP) playerIn
							);
						}
						
						BirthsignEffectManager.reapplyBirthsignEffects(playerIn, null, birthsignName);

						// Extract the birthsign name without modid prefix for translation
							String birthsignNameForTranslation = birthsignName;
							if (birthsignName.contains(":")) {
								birthsignNameForTranslation = birthsignName.split(":")[1];
							}
							String localizedbirthsignName = Menhir.proxy.translate("birthsign." + birthsignNameForTranslation + ".name");
							playerIn.sendMessage(new TextComponentString("§aBirthsign " + localizedbirthsignName + " assigned!"));

							// Play a sound effect
							worldIn.playSound(null, pos, net.minecraft.init.SoundEvents.BLOCK_ANVIL_LAND,
									net.minecraft.util.SoundCategory.BLOCKS, 0.5F, 1.0F);
						}
					} else {
						playerIn.sendMessage(new TextComponentString("§cFailed to get birthsign data."));
					}
				} else {
					playerIn.sendMessage(new TextComponentString("§cThis menhir stone has no birthsign assigned."));
				}
			}
		}
		return true; // Always return true to prevent other interactions
	}

	/**
	 * Handles block placement to ensure proper multiblock structure
	 */
	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

		// Determine the position in the multiblock structure
		BlockPosition position = BlockPosition.BOTTOM; // Default to bottom

		// Check if there are birthsign stone blocks below this one
		IBlockState belowState = worldIn.getBlockState(pos.down());
		if (belowState.getBlock() == this) {
			BlockPosition belowPos = belowState.getValue(POSITION);
			if (belowPos == BlockPosition.BOTTOM) {
				position = BlockPosition.MIDDLE;
			} else if (belowPos == BlockPosition.MIDDLE) {
				position = BlockPosition.TOP;
			}
		}

		// Update the block state with the correct position
		worldIn.setBlockState(pos, state.withProperty(POSITION, position));

		// Update neighboring blocks to maintain proper structure
		updateMultiblockStructure(worldIn, pos, position);
	}

	/**
	 * Updates the multiblock structure to ensure proper positioning
	 */
	private void updateMultiblockStructure(World world, BlockPos pos, BlockPosition position) {
		switch (position) {
			case BOTTOM:
				// Check if there's a block above that should be middle
				IBlockState aboveState = world.getBlockState(pos.up());
				if (aboveState.getBlock() == this) {
					world.setBlockState(pos.up(), aboveState.withProperty(POSITION, BlockPosition.MIDDLE));
					// Check if there's another block above that should be top
					IBlockState aboveAboveState = world.getBlockState(pos.up(2));
					if (aboveAboveState.getBlock() == this) {
						world.setBlockState(pos.up(2), aboveAboveState.withProperty(POSITION, BlockPosition.TOP));
					}
				}
				break;
			case MIDDLE:
				// Check if there's a block below that should be bottom
				IBlockState belowState = world.getBlockState(pos.down());
				if (belowState.getBlock() == this && belowState.getValue(POSITION) != BlockPosition.BOTTOM) {
					world.setBlockState(pos.down(), belowState.withProperty(POSITION, BlockPosition.BOTTOM));
				}
				// Check if there's a block above that should be top
				IBlockState aboveState2 = world.getBlockState(pos.up());
				if (aboveState2.getBlock() == this && aboveState2.getValue(POSITION) != BlockPosition.TOP) {
					world.setBlockState(pos.up(), aboveState2.withProperty(POSITION, BlockPosition.TOP));
				}
				break;
			case TOP:
				// Check if there are blocks below that should be properly positioned
				IBlockState belowState2 = world.getBlockState(pos.down());
				if (belowState2.getBlock() == this && belowState2.getValue(POSITION) != BlockPosition.MIDDLE) {
					world.setBlockState(pos.down(), belowState2.withProperty(POSITION, BlockPosition.MIDDLE));
					// Check the bottom block
					IBlockState bottomState = world.getBlockState(pos.down(2));
					if (bottomState.getBlock() == this && bottomState.getValue(POSITION) != BlockPosition.BOTTOM) {
						world.setBlockState(pos.down(2), bottomState.withProperty(POSITION, BlockPosition.BOTTOM));
					}
				}
				break;
		}
	}

	/**
	 * Handles block breaking to ensure proper multiblock structure
	 */
	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		super.breakBlock(worldIn, pos, state);

		BlockPosition position = state.getValue(POSITION);

		switch (position) {
			case TOP:
				// Update the middle block below to remove top reference
				IBlockState belowState = worldIn.getBlockState(pos.down());
				if (belowState.getBlock() == this && belowState.getValue(POSITION) == BlockPosition.MIDDLE) {
					worldIn.setBlockState(pos.down(), belowState.withProperty(POSITION, BlockPosition.MIDDLE));
				}
				break;
			case MIDDLE:
				// Update the bottom block below to remove middle reference
				IBlockState belowState2 = worldIn.getBlockState(pos.down());
				if (belowState2.getBlock() == this && belowState2.getValue(POSITION) == BlockPosition.BOTTOM) {
					worldIn.setBlockState(pos.down(), belowState2.withProperty(POSITION, BlockPosition.BOTTOM));
				}
				// Update the top block above to remove middle reference
				IBlockState aboveState = worldIn.getBlockState(pos.up());
				if (aboveState.getBlock() == this && aboveState.getValue(POSITION) == BlockPosition.TOP) {
					worldIn.setBlockState(pos.up(), aboveState.withProperty(POSITION, BlockPosition.TOP));
				}
				break;
			case BOTTOM:
				// Update the middle block above to remove bottom reference
				IBlockState aboveState2 = worldIn.getBlockState(pos.up());
				if (aboveState2.getBlock() == this && aboveState2.getValue(POSITION) == BlockPosition.MIDDLE) {
					worldIn.setBlockState(pos.up(), aboveState2.withProperty(POSITION, BlockPosition.MIDDLE));
					// Also update the top block if it exists
					IBlockState aboveAboveState = worldIn.getBlockState(pos.up(2));
					if (aboveAboveState.getBlock() == this && aboveAboveState.getValue(POSITION) == BlockPosition.TOP) {
						worldIn.setBlockState(pos.up(2), aboveAboveState.withProperty(POSITION, BlockPosition.TOP));
					}
				}
				break;
		}
	}

	/**
	 * Handles neighbor block changes to ensure proper multiblock structure
	 */
	@Override
	public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
		super.neighborChanged(state, worldIn, pos, blockIn, fromPos);

		// Check if the multiblock structure has changed and update accordingly
		BlockPosition currentPosition = state.getValue(POSITION);
		BlockPosition newPosition = determinePosition(worldIn, pos);

		if (currentPosition != newPosition) {
			worldIn.setBlockState(pos, state.withProperty(POSITION, newPosition));
		}
	}

	/**
	 * Determines the correct position for a block based on its neighbors
	 */
	private BlockPosition determinePosition(World world, BlockPos pos) {
		IBlockState belowState = world.getBlockState(pos.down());
		IBlockState aboveState = world.getBlockState(pos.up());

		if (belowState.getBlock() == this) {
			BlockPosition belowPos = belowState.getValue(POSITION);
			if (belowPos == BlockPosition.BOTTOM) {
				return BlockPosition.MIDDLE;
			} else if (belowPos == BlockPosition.MIDDLE) {
				return BlockPosition.TOP;
			}
		}

		if (aboveState.getBlock() == this) {
			BlockPosition abovePos = aboveState.getValue(POSITION);
			if (abovePos == BlockPosition.TOP) {
				return BlockPosition.MIDDLE;
			} else if (abovePos == BlockPosition.MIDDLE) {
				return BlockPosition.BOTTOM;
			}
		}

		// Default to bottom if no clear structure
		return BlockPosition.BOTTOM;
	}

	/**
	 * Override getPickBlock to include birthsign information in the picked block
	 */
	@Override
	public ItemStack getPickBlock(IBlockState state, net.minecraft.util.math.RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		ItemStack stack = super.getPickBlock(state, target, world, pos, player);

		// Get the birthsign information from the tile entity
		TileEntityMenhirStone tileEntity = getMainTileEntity(world, pos);
		if (tileEntity != null) {
			String birthsignName = tileEntity.getBirthsign();
			if (birthsignName != null && !birthsignName.isEmpty()) {
				// Create NBT compound if it doesn't exist
				if (!stack.hasTagCompound()) {
					stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
				}
				// Store the birthsign name in NBT
				stack.getTagCompound().setString("birthsign", birthsignName);
			}
		}

		return stack;
	}

	/**
	 * Enum for the different positions in the multiblock structure
	 */
	public enum BlockPosition implements net.minecraft.util.IStringSerializable {
		BOTTOM("bottom"),
		MIDDLE("middle"),
		TOP("top");

		private final String name;

		BlockPosition(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	/**
	 * Custom ItemBlock class that displays the bound birthsign type in the tooltip
	 */
	public static class ItemBlockMenhirStone extends ItemBlock {

		public ItemBlockMenhirStone(Block block) {
			super(block);
		}

		@Override
		public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag flagIn) {
			super.addInformation(stack, worldIn, tooltip, flagIn);

			// Add basic information about the birthsign stone
			tooltip.add(TextFormatting.GRAY + "A mystical stone that can bind");
			tooltip.add(TextFormatting.GRAY + "birthsign powers to players.");
			tooltip.add("");
			tooltip.add(TextFormatting.YELLOW + "Right-click to assign birthsign");
			tooltip.add(TextFormatting.YELLOW + "when placed in world.");

			// Check if this stack has NBT data with birthsign information
			if (stack.hasTagCompound() && stack.getTagCompound().hasKey("birthsign")) {
				String birthsignName = stack.getTagCompound().getString("birthsign");
				if (!birthsignName.isEmpty()) {
					tooltip.add("");

					// Extract the birthsign name without modid prefix for translation
					String birthsignNameForTranslation = birthsignName;
					if (birthsignName.contains(":")) {
						birthsignNameForTranslation = birthsignName.split(":")[1];
					}
					String localizedName = Menhir.proxy.translate("birthsign." + birthsignNameForTranslation + ".name");
					tooltip.add(TextFormatting.GREEN + "Bound to: " + TextFormatting.WHITE + localizedName);
				}
			}
		}
	}
} 